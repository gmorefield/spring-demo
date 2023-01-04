package com.example.springdemo.controller;

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
import org.springframework.boot.context.properties.bind.DefaultValue;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.example.springdemo.data.DocumentPagingAndSortingRepository;
import com.example.springdemo.model.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

@Profile("mssql")
@RestController
@RequestMapping("/document")
public class DocumentController {

    private AsyncTaskExecutor taskExecutor;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private DocumentPagingAndSortingRepository documentRepository;
    private ObjectMapper objectMapper;

    public DocumentController(AsyncTaskExecutor taskExecutor, NamedParameterJdbcTemplate jdbcTemplate,
            DocumentPagingAndSortingRepository documentRepository, ObjectMapper objectMapper) {
        this.taskExecutor = taskExecutor;
        this.jdbcTemplate = jdbcTemplate;
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/store")
    public ResponseEntity<String> storeDocument(@RequestPart(name = "meta", required = false) Optional<Map<?, ?>> meta,
            @RequestPart("file") MultipartFile multipartFile) throws IOException {

        Map<String, Object> params = new HashMap<>();
        UUID docUid = UUID.randomUUID();
        params.put("id", docUid);
        params.put("createDt", LocalDateTime.now());
        params.put("contentType", multipartFile.getContentType());
        params.put("size", multipartFile.getSize());
        params.put("fileName", multipartFile.getOriginalFilename());

        final PipedOutputStream pout = new PipedOutputStream();
        PipedInputStream pin = new PipedInputStream(pout, 1024);
        params.put("docBin", pin);

        taskExecutor.submit(() -> {
            CountingOutputStream cout = null;
            GZIPOutputStream gzout = null;
            try {
                gzout = new GZIPOutputStream(pout);
                cout = new CountingOutputStream(gzout);

                Map<?,?> event = objectMapper.readValue(multipartFile.getInputStream(), Map.class);
                InputStream test = multipartFile.getInputStream();
                System.out.println("markSupported: " + test.markSupported());
                StreamUtils.copy(multipartFile.getInputStream(), cout);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (gzout != null)
                        gzout.close();
                    pout.close();
                } catch (Exception ignored) {
                }
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
            @RequestParam(name = "sortOrder", required = false) @DefaultValue("asc") final String sortOrder) {

        PageRequest pageable = PageRequest.of(page, size, Direction.fromString(sortOrder), sortBy);
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
            @RequestParam(name = "sortOrder", required = false) @DefaultValue("asc") final Optional<String> sortOrder) {

        Sort sort = Sort.by(Direction.fromString(sortOrder.orElse("asc")), sortBy);
        return (List<Document>) documentRepository.findAll(sort);
    }

    @GetMapping("/{docId}/binary")
    public ResponseEntity<StreamingResponseBody> getDocument(@PathVariable("docId") String docId,
            @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) @DefaultValue("identity") String acceptEncodingHeader) {

        boolean useGzip = acceptEncodingHeader != null && acceptEncodingHeader.contains("gzip");

        Document doc = jdbcTemplate.queryForObject(
                "select CONTENT_TYPE, CONTENT_LEN, FILE_NM, DATALENGTH(DOC_BIN) as BIN_LEN from DOCUMENT where ID = :id",
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

        StreamingResponseBody body = outputStream -> {
            jdbcTemplate.query("select Doc_Bin from DOCUMENT where ID = :id",
                    new MapSqlParameterSource("id", docId),
                    new RowCallbackHandler() {
                        public void processRow(ResultSet resultSet) throws SQLException {
                            InputStream inputStream = resultSet.getBinaryStream("Doc_Bin");

                            try {
                                if (!useGzip) {
                                    inputStream = new GZIPInputStream(inputStream);
                                }
                                StreamUtils.copy(inputStream, outputStream);
                            } catch (IOException e) {
                                throw new SQLException(e);
                            }
                        }
                    });
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(doc.getContentType()));
        headers.add("Content-Disposition", "attachment; filename=" + doc.getFileName());
        if (doc.getContentLength() > 0) {
            // headers.setContentLength(doc.getContentLength());
        }
        // headers.add("Pragma", "no-cache");
        // headers.add("Cache-Control", "no-cache");
        if (useGzip) {
            headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");
        }
        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
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