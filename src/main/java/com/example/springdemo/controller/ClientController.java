package com.example.springdemo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.springdemo.model.Person;

@RestController
@RequestMapping("client")
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

    @GetMapping(path = "getError")
    public ResponseEntity getError() throws Exception {
        try {
            ResponseEntity<Person> response = restClient.getForEntity("/data/DoesNotExist", Person.class);
            return response;
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getRawStatusCode()).body(e.getResponseBodyAsString());
        }
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

    @GetMapping(path = "getXmlFlux", produces = { MediaType.APPLICATION_XML_VALUE })
    public Person getXmlUsingFlux() {
        ResponseEntity<Person> response = webClient.get()
                .uri("/data/xml")
                .retrieve()
                .toEntity(Person.class)
                .block();

        return response.getBody();
    }
}
