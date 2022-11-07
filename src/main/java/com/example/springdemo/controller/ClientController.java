package com.example.springdemo.controller;

import java.util.Map;

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

    public ClientController(RestTemplate restClient, WebClient webClient) {
        this.restClient = restClient;
        this.webClient = webClient;
    }

    @GetMapping(path = "xml", produces = { MediaType.APPLICATION_XML_VALUE })
    public String xml() {
        return "<person><id>123</id><firstName>John</firstName><lastName>Doe</lastName></person>";
    }

    @GetMapping(path = "xmlInJson", produces = { MediaType.APPLICATION_JSON_VALUE })
    public String xmlInJson() {
        return "{\"return\": \"" + xml() + "\" }";
    }

    @GetMapping(path = "getXmlInJson", produces = { MediaType.APPLICATION_XML_VALUE })
    public String getXmlInJson() {
        Map<?, ?> response = restClient.getForObject("/message/xmlInJson", Map.class);
        return response == null ? "" : (String) response.get("return");
    }

    @GetMapping(path = "getXml", produces = { MediaType.APPLICATION_XML_VALUE })
    public Person getXml() {
        // HttpHeaders headers = new HttpHeaders();
        // headers.setAccept(List.of(MediaType.APPLICATION_XML));
        // ResponseEntity<Person> response = restClient.exchange("/message/xml",
        // HttpMethod.GET, new HttpEntity(headers), Person.class);

        ResponseEntity<Person> response = restClient.getForEntity("/message/xml", Person.class);
        return response.getBody();
    }

    @GetMapping(path = "getError")
    public ResponseEntity getError() throws Exception {
        try {
            ResponseEntity<Person> response = restClient.getForEntity("/message/DoesNotExist", Person.class);
            return response;
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getRawStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @GetMapping(path = "getXmlInJsonFlux", produces = { MediaType.APPLICATION_XML_VALUE })
    public String getXmlInJsonUsingFlux() {
        Map<?, ?> response = webClient.get()
                .uri("/message/xmlInJson")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response == null ? "" : (String) response.get("return");
    }

    @GetMapping(path = "getXmlFlux", produces = { MediaType.APPLICATION_XML_VALUE })
    public Person getXmlUsingFlux() {
        ResponseEntity<Person> response = webClient.get()
                .uri("/message/xml")
                .retrieve()
                .toEntity(Person.class)
                .block();

        return response.getBody();
    }
}
