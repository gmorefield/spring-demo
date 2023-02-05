package com.example.springdemo.controller;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.output.CountingOutputStream;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.example.springdemo.client.RecordStorageClient;
import com.example.springdemo.data.DocumentPagingAndSortingRepository;
import com.example.springdemo.model.Document;
import com.example.springdemo.soap.model.SaveStorageRecordResponse;
import com.example.springdemo.util.JsonConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

@Profile({ "mssql", "H2" })
@RestController
@RequestMapping("/document")
public class DocumentController {

    private AsyncTaskExecutor taskExecutor;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private DocumentPagingAndSortingRepository documentRepository;
    private ObjectMapper objectMapper;
    private RecordStorageClient recordStorageClient;

    public DocumentController(AsyncTaskExecutor taskExecutor, NamedParameterJdbcTemplate jdbcTemplate,
            DocumentPagingAndSortingRepository documentRepository, ObjectMapper objectMapper,
            RecordStorageClient recordStorageClient) {
        this.taskExecutor = taskExecutor;
        this.jdbcTemplate = jdbcTemplate;
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
        this.recordStorageClient = recordStorageClient;
    }

    @PostMapping(path = "/store", consumes = MediaType.MULTIPART_FORM_DATA_VALUE )
    public ResponseEntity<String> storeDocument(@RequestPart(name = "meta", required = false) Optional<Map<?, ?>> meta,
            @RequestPart("file") MultipartFile multipartFile,
            @RequestParam(required = false) Optional<Boolean> transform,
            @RequestParam(required = false) Optional<Boolean> compactFormat) throws IOException {

        boolean convertToXml = transform.orElse(false) &&
                Optional.ofNullable(multipartFile.getContentType()).orElse("").contains("json");

        Map<String, Object> params = new HashMap<>();
        UUID docUid = UUID.randomUUID();
        params.put("id", docUid);
        params.put("createDt", LocalDateTime.now());
        if (convertToXml) {
            params.put("contentType", multipartFile.getContentType().replace("json", "xml"));
            params.put("fileName", multipartFile.getOriginalFilename().replace("json", "xml"));
            params.put("size", -1); // don't know size of converted file
        } else {
            params.put("contentType", multipartFile.getContentType());
            params.put("fileName", multipartFile.getOriginalFilename());
            params.put("size", multipartFile.getSize());
        }

        final PipedOutputStream pout = new PipedOutputStream();
        PipedInputStream pin = new PipedInputStream(pout, 1024);
        params.put("docBin", pin);

        taskExecutor.submit(() -> {
            CountingOutputStream cout = null;
            GZIPOutputStream gzout = null;
            InputStream mpout = null;
            try {
                gzout = new GZIPOutputStream(pout);
                cout = new CountingOutputStream(gzout);
                mpout = multipartFile.getInputStream();

                if (convertToXml) {
                    new JsonConverter().root("root").useCompactFormat(compactFormat.orElse(true)).streamToXml(mpout,
                            cout);
                } else {
                    StreamUtils.copy(mpout, cout);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeQuietly(cout);
                closeQuietly(gzout);
                closeQuietly(pout);
                closeQuietly(mpout);
            }
        });

        int rowsUpdated = jdbcTemplate.update(
                "insert into DOCUMENT (ID, Create_Dt, Doc_Bin, Content_Type, Content_Len, File_Nm) values (:id, :createDt, :docBin, :contentType, :size, :fileName)",
                new MapSqlParameterSource(params));

        return ResponseEntity.status(HttpStatus.CREATED).body(docUid.toString());
    }

    @GetMapping()
    public Iterable<Document> findAll() {
        return documentRepository.findAll();
    }

    @GetMapping("/{docId}")
    public Optional<Document> findById(@PathVariable String docId) {
        return documentRepository.findById(docId);
    }

    // Read - by sorted and paginated
    @GetMapping(params = { "page", "size", "sortBy", "sortOrder" })
    public List<Document> findAllBySortAndPage(
            @RequestParam("page") final int page,
            @RequestParam("size") final int size,
            @RequestParam("sortBy") final String sortBy,
            @RequestParam(name = "sortOrder", required = false) final Optional<String> sortOrder) {

        PageRequest pageable = PageRequest.of(page, size, Direction.fromString(sortOrder.orElse("asc")), sortBy);
        Page<Document> result = documentRepository.findAll(pageable);

        return result.isEmpty() ? Collections.emptyList() : result.getContent();
    }
    // Read - by only paginated

    @GetMapping(params = { "page", "size" })
    public List<Document> findAllByPage(
            @RequestParam("page") final int page,
            @RequestParam("size") final int size) {

        PageRequest pageable = PageRequest.of(page, size);
        Page<Document> result = documentRepository.findAll(pageable);

        return result.isEmpty() ? Collections.emptyList() : result.getContent();
    }
    // Read - by only sorted

    @GetMapping(params = { "sortBy" })
    public List<Document> findAllBySort(
            @RequestParam("sortBy") final String sortBy,
            @RequestParam(name = "sortOrder", required = false) final Optional<String> sortOrder) {

        Sort sort = Sort.by(Direction.fromString(sortOrder.orElse("asc")), sortBy);
        return (List<Document>) documentRepository.findAll(sort);
    }

    @GetMapping("/{docId}/binary")
    public ResponseEntity<StreamingResponseBody> getDocument(@PathVariable("docId") String docId,
            @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) Optional<String> acceptEncodingHeader,
            @RequestParam(required = false) Optional<Boolean> transform,
            @RequestParam(required = false) Optional<Boolean> compactFormat) {

        final boolean useGzip = acceptEncodingHeader.orElse("identity").contains("gzip");

        Document doc = jdbcTemplate.queryForObject(
                "select CONTENT_TYPE, CONTENT_LEN, FILE_NM, -1 as BIN_LEN from DOCUMENT where ID = :id",
                new MapSqlParameterSource("id", docId),
                (ResultSet rs, int rowNum) -> {
                    Document d = new Document();
                    d.setId(docId);
                    d.setContentLength(rs.getLong(useGzip ? "BIN_LEN" : "CONTENT_LEN"));
                    d.setContentType(rs.getString("CONTENT_TYPE"));
                    d.setFileName(rs.getString("FILE_NM"));
                    return d;
                });

        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        final boolean convertToXml = transform.orElse(false) &&
                Optional.ofNullable(doc.getContentType()).orElse("").contains("json");

        StreamingResponseBody body = outputStream -> {
            jdbcTemplate.query("select Doc_Bin from DOCUMENT where ID = :id",
                    new MapSqlParameterSource("id", docId),
                    new RowCallbackHandler() {
                        public void processRow(ResultSet resultSet) throws SQLException {
                            InputStream inputStream = resultSet.getBinaryStream("Doc_Bin");

                            try {
                                if (!useGzip || convertToXml) {
                                    inputStream = new GZIPInputStream(inputStream);
                                }
                                if (convertToXml) {
                                    new JsonConverter().root("root").useCompactFormat(compactFormat.orElse(true))
                                            .streamToXml(inputStream, outputStream);
                                } else {
                                    StreamUtils.copy(inputStream, outputStream);
                                }
                            } catch (IOException e) {
                                throw new SQLException(e);
                            } finally {
                                closeQuietly(inputStream);
                            }
                        }
                    });
        };

        long contentLength = doc.getContentLength();
        String fileName = doc.getFileName();
        String contentType = doc.getContentType();
        if (convertToXml) {
            contentType = contentType.replace("json", "xml");
            fileName = fileName.replace("json", "xml");
            contentLength = -1;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(contentType));
        headers.add("Content-Disposition", "attachment; filename=" + fileName);
        if (contentLength > 0) {
            // headers.setContentLength(doc.getContentLength());
        }
        // headers.add("Pragma", "no-cache");
        // headers.add("Cache-Control", "no-cache");
        if (useGzip && !convertToXml) {
            headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");
        }
        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }

    @PostMapping("/record/save")
    public ResponseEntity<SaveStorageRecordResponse> saveRecord(@RequestBody Map<String, Object> data) {
        SaveStorageRecordResponse response = recordStorageClient.saveRecord("/Users/morefigs/Downloads/" + data.get("fileName"),
                (String) data.get("contentType"));
        return ResponseEntity.ok(response);
    }

    // need @RestControllerAdvice?
    // @ExceptionHandler(MaxUploadSizeExceededException.class)
    // // @ExceptionHandler(SizeLimitExceededException.class)
    // @ResponseStatus(value = HttpStatus.PAYLOAD_TOO_LARGE)
    // public ResponseEntity handleMultipartException(MaxUploadSizeExceededException
    // e) {

    // Map<String, String> result = new HashMap<>();
    // result.put("message", "Error ==> Large File ");
    // return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
    // .body(result);

    // }

}
