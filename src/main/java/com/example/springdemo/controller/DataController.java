package com.example.springdemo.controller;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.example.springdemo.util.JsonConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

@RestController
@RequestMapping("data")
public class DataController {
    NamedParameterJdbcTemplate jdbcTemplate;

    public DataController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping(path = "xml", produces = { MediaType.APPLICATION_XML_VALUE })
    public String xml() {
        return "<person><id>123</id><firstName>John</firstName><lastName>Doe</lastName></person>";
    }

    @GetMapping(path = "xmlInJson", produces = { MediaType.APPLICATION_JSON_VALUE })
    public String xmlInJson() {
        return "{\"return\": \"" + xml() + "\" }";
    }

    @GetMapping(path = "sampleJson", produces = { MediaType.APPLICATION_JSON_VALUE })
    public String sampleJson() throws IOException {
        // final String jsonStr =
        // "{\"name\":\"JSON\",\"integer\":1,\"double\":2.0,\"boolean\":true,\"nested\":{\"id\":42},\"array\":[1,2,3]}";
        Path path = Paths.get("src/test/resources/sample-claim-app-event.json");
        return Files.readString(path);
    }

    @GetMapping(path = "parseJson", produces = { MediaType.APPLICATION_JSON_VALUE })
    public void parseJson(
            @PathVariable(name = "useNodeName", required = false) @DefaultValue("true") boolean useNodeName,
            HttpServletResponse response) throws IOException, XMLStreamException {
        Path path = Paths.get("src/test/resources/sample-claim-app-event.json");
        new JsonConverter().root("ccxml").streamToXml(new FileInputStream(path.toFile()), response.getOutputStream());
    }

    @GetMapping(path = "saveJson", produces = { MediaType.APPLICATION_JSON_VALUE })
    public void saveJson(
            @PathVariable(name = "useNodeName", required = false) @DefaultValue("true") boolean useNodeName,
            HttpServletResponse response) throws IOException, XMLStreamException {
        Path path = Paths.get("src/test/resources/sample-claim-app-event-xhuge.json");

        Map<String, Object> params = new HashMap<>();
        params.put("id", UUID.randomUUID());
        params.put("createDt", LocalDateTime.now());

        PipedOutputStream pout = new PipedOutputStream();
        PipedInputStream pin = new PipedInputStream(pout, 1024);
        params.put("eventBody", pin);

        Executors.newCachedThreadPool().submit(() -> {
            try {
                new JsonConverter().root("ccxml").streamToXml(new FileInputStream(path.toFile()), pout);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    pout.close();
                } catch (Exception ignored) {
                }
            }
        });

        int rowsUpdated = jdbcTemplate.update(
                "insert into App_Event (ID, Create_Dt, Event_Body) values (:id, :createDt, :eventBody)",
                new MapSqlParameterSource(params));
    }

    @GetMapping(path = "saveXmlBin")
    public void saveXmlBin(
            @PathVariable(name = "useNodeName", required = false) @DefaultValue("true") boolean useNodeName,
            HttpServletResponse response) throws IOException, XMLStreamException {
        Path path = Paths.get("src/test/resources/sample-claim-app-event-xhuge.json");

        Map<String, Object> params = new HashMap<>();
        params.put("id", UUID.randomUUID());
        params.put("createDt", LocalDateTime.now());

        final PipedOutputStream pout = new PipedOutputStream();
        PipedInputStream pin = new PipedInputStream(pout, 1024);
        params.put("eventBin", pin);

        Executors.newCachedThreadPool().submit(() -> {
            GZIPOutputStream gzout = null;
            try {
                gzout = new GZIPOutputStream(pout);
                
                new JsonConverter().root("ccxml").streamToXml(new FileInputStream(path.toFile()), gzout);
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
                "insert into App_Event (ID, Create_Dt, Event_Bin) values (:id, :createDt, :eventBin)",
                new MapSqlParameterSource(params));
    }

    @GetMapping(path = "loadXml/{id}", produces = { MediaType.APPLICATION_XML_VALUE })
    public void loadXml(@PathVariable("id") @DefaultValue("D73D2DDA-C1A5-4D48-8523-4BDBA5F7B588") String id,
            HttpServletResponse response)
            throws Exception {

        jdbcTemplate.query("select DATALENGTH(Event_Body) as Event_Size, Event_Body from App_Event where ID = :id",
                new MapSqlParameterSource("id", id),
                new RowCallbackHandler() {
                    public void processRow(ResultSet resultSet) throws SQLException {
                        // if (resultSet.next()) {
                        String eventSize = resultSet.getString("Event_Size");
                        System.out.println("Content-Length=" + eventSize);
                        response.addHeader("Content-Length", eventSize);
                        response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
                        InputStream inputStream = resultSet.getAsciiStream("Event_Body");
                        try {
                            StreamUtils.copy(inputStream, response.getOutputStream());
                        } catch (IOException e) {
                            throw new SQLException(e);
                        }
                        // }
                    }
                });

    }

    @GetMapping(path = "streamXml/{id}", produces = { MediaType.APPLICATION_XML_VALUE })
    public ResponseEntity<StreamingResponseBody> streamXml(
            @PathVariable("id") @DefaultValue("D73D2DDA-C1A5-4D48-8523-4BDBA5F7B588") String id,
            @RequestParam(name = "col", required = false) @DefaultValue("body") String column,
            @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) @DefaultValue("identity") String acceptEncodingHeader,
            HttpServletRequest httpServletRequest)
            throws Exception {

        String columnName = "bin".equalsIgnoreCase(column) ? "Bin" : "Body";
        String acceptEncoding = acceptEncodingHeader==null?"identity":acceptEncodingHeader;
        boolean useBin = "bin".equalsIgnoreCase(column);
        Long eventSize = jdbcTemplate.queryForObject(
                "select DATALENGTH(Event_" + columnName + ") as Event_Size from App_Event where ID = :id",
                new MapSqlParameterSource("id", id),
                Long.class);

        if (eventSize == null) {
            return ResponseEntity.notFound().build();
        }

        StreamingResponseBody body = outputStream -> {
            jdbcTemplate.query("select Event_" + columnName + " from App_Event where ID = :id",
                    new MapSqlParameterSource("id", id),
                    new RowCallbackHandler() {
                        public void processRow(ResultSet resultSet) throws SQLException {
                            InputStream inputStream = useBin
                                    ? resultSet.getBinaryStream("Event_Bin")
                                    : resultSet.getAsciiStream("Event_Body");

                            try {
                                if (useBin && !acceptEncoding.contains("gzip")) {
                                    System.out.println("unzipping input");
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
        headers.setContentType(MediaType.APPLICATION_XML);
        if (!useBin) {
            headers.setContentLength(eventSize == null ? 0 : eventSize.longValue());
        }
        else if (useBin && acceptEncoding.contains("gzip")) {
            System.out.println("adding content-type: gzip");
            headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");
            headers.setContentLength(-1);
        }
        return ResponseEntity.ok()
                // .contentType(MediaType.APPLICATION_XML)
                // .contentLength(eventSize == null ? 0 : eventSize.longValue())
                .headers(headers)
                .body(body);
    }

    @GetMapping(path = "json2xml")
    public String jsonToXml() throws IOException {
        // final String jsonStr =
        // "{\"name\":\"JSON\",\"integer\":1,\"double\":2.0,\"boolean\":true,\"nested\":{\"id\":42},\"array\":[1,2,3]}";
        Path path = Paths.get("src/test/resources/sample-claim-app-event.json");
        // BufferedReader reader = Files.newBufferedReader(path);
        // final String jsonStr = reader.readLine();
        final String jsonStr = Files.readString(path);

        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode node = jsonMapper.readValue(jsonStr, JsonNode.class);
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_1_1, true);
        StringWriter sw = new StringWriter();
        xmlMapper.writeValue(sw, node);

        return sw.toString();
    }

    @GetMapping(path = "parseXml")
    public String parseXml() throws IOException {
        // final String jsonStr =
        // "{\"name\":\"JSON\",\"integer\":1,\"double\":2.0,\"boolean\":true,\"nested\":{\"id\":42},\"array\":[1,2,3]}";
        Path path = Paths.get("src/test/resources/sample-claim-app-event.json");
        // BufferedReader reader = Files.newBufferedReader(path);
        // final String jsonStr = reader.readLine();
        final String jsonStr = Files.readString(path);

        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode node = jsonMapper.readValue(jsonStr, JsonNode.class);
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_1_1, true);
        StringWriter sw = new StringWriter();
        xmlMapper.writeValue(sw, node);

        return sw.toString();
    }

}
