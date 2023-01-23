package com.example.springdemo.controller;

import static org.springframework.http.HttpStatus.OK;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.example.springdemo.model.Person;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("client")
@Slf4j
public class ClientController {
    private RestTemplate restClient;
    private WebClient webClient;

    public ClientController(@Qualifier("dataRestClient") RestTemplate restClient, WebClient webClient) {
        this.restClient = restClient;
        this.webClient = webClient;
    }

    @GetMapping(path = "getXmlInJson", produces = { MediaType.APPLICATION_XML_VALUE })
    public String getXmlInJson() {
        Map<?, ?> response = restClient.getForObject("/data/xmlInJson", Map.class);
        return response == null ? "" : (String) response.get("return");
    }

    @GetMapping(path = "getXml", produces = { MediaType.APPLICATION_XML_VALUE })
    public String getXml() {
        ResponseEntity<String> response = restClient.getForEntity("/data/xml", String.class);
        return response.getBody();
    }

    @GetMapping(path = "getPerson", produces = { MediaType.APPLICATION_XML_VALUE })
    public Person getPerson() {
        // HttpHeaders headers = new HttpHeaders();
        // headers.setAccept(List.of(MediaType.APPLICATION_XML));
        // ResponseEntity<Person> response = restClient.exchange("/data/xml",
        // HttpMethod.GET, new HttpEntity(headers), Person.class);

        ResponseEntity<Person> response = restClient.getForEntity("/data/xml", Person.class);
        return response.getBody();
    }

    @GetMapping(path = "getStatus")
    public ResponseEntity<?> getStatus(@RequestParam(name = "code", required = false) Optional<Integer> statusCode)
            throws Exception {

        return ResponseEntity.status(statusCode.orElse(OK.value())).build();
    }

    @GetMapping(path = "getXmlFlux", produces = { MediaType.APPLICATION_XML_VALUE })
    public String getXmlUsingFlux() {
        ResponseEntity<String> response = webClient.get()
                .uri("/data/xml")
                .retrieve()
                .toEntity(String.class)
                .block();
        return response.getBody();
    }

    @GetMapping(path = "getXmlInJsonFlux", produces = { MediaType.APPLICATION_XML_VALUE })
    public String getXmlInJsonUsingFlux() {
        Map<?, ?> response = webClient.get()
                .uri("/data/xmlInJson")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response == null ? "" : (String) response.get("return");
    }

    @GetMapping(path = "getPersonFlux", produces = { MediaType.APPLICATION_XML_VALUE })
    public Person getPersonUsingFlux() {
        ResponseEntity<Person> response = webClient.get()
                .uri("/data/xml")
                .retrieve()
                .toEntity(Person.class)
                .block();

        return response.getBody();
    }

    @GetMapping(path = "flux/document/binary/{docId}")
    public ResponseEntity<StreamingResponseBody> getDocumentUsingFlux(@PathVariable String docId,
            @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) Optional<String> acceptEncodingHeader) {

        final ResponseEntity<Flux<DataBuffer>> fluxEntity = webClient.get()
                .uri("/document/{docId}/binary", docId)
                .header(HttpHeaders.ACCEPT_ENCODING, acceptEncodingHeader.orElse("identity"))
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.debug("Body is {}", body);
                                return Mono.error(new RuntimeException(
                                        "Client call failed with response: " + response.rawStatusCode()));
                            });
                })
                // .bodyToFlux(DataBuffer.class);
                .toEntityFlux(DataBuffer.class)
                // .timeout(Duration.ofSeconds(30))
                .block(Duration.ofSeconds(30));

        StreamingResponseBody body = outputStream -> {
            Flux<DataBuffer> flux = fluxEntity.getBody();

            DataBufferUtils
                    .write(flux, outputStream)
                    .map(DataBufferUtils::release)
                    .blockLast();

            // DataBufferUtils
            // .write(flux, Paths.get("destination"),
            // StandardOpenOption.CREATE)
            // .block();
        };

        return ResponseEntity.ok()
                .headers(fluxEntity.getHeaders())
                .body(body);
    }
}
