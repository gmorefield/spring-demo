package com.example.springdemo.controller;

import com.example.springdemo.model.Person;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("client")
@Slf4j
public class ClientController {
    private final RestTemplate restClient;
    private final WebClient webClient;

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
        ResponseEntity<Person> response = restClient.getForEntity("/data/xml", Person.class);
        return response.getBody();
    }

    @GetMapping(path = "getStatus")
    public ResponseEntity<?> getStatus(@RequestParam(name = "code", required = false) Optional<Integer> statusCode,
                                       @RequestParam(name="retry", required = false) Optional<Boolean> retryEnabled) {
        ResponseEntity<?> response = webClient.get()
                .uri("/data/getStatus?code={code}", statusCode.orElse(HttpStatus.OK.value()))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, r -> {
                    // return Mono.empty();
                    return r.bodyToMono(String.class)
                            .map(RuntimeException::new);
                })
                .toEntity(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).jitter(.5)
                        .filter(throwable -> retryEnabled.orElse(false) && throwable != null))
                .block();
        return response;
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
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.debug("Body is {}", body);
                            return Mono.error(new RuntimeException(
                                    "Client call failed with response: " + response.statusCode()));
                        }))
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
