package com.example.springdemo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.springdemo.model.Person;

@RestController
@RequestMapping("message")
public class MessageController {
    public RestTemplate restClient;

    public MessageController(RestTemplate msgRestTemplate) {
        this.restClient = msgRestTemplate;
    }

    @GetMapping("xml")
    public String xml() {
        return "<person><id>123</id><firstName>John</firstName><lastName>Doe</lastName></person>";
    }

    @GetMapping("xmlInJson")
    public String xmlInJson() {
        return "{\"return\": \"" + xml() + "\" }";
    }

    @GetMapping("getXmlInJson")
    public String getXmlInJson() {
        ResponseEntity<Map> response = restClient.getForEntity("/message/xmlInJson", Map.class);
        return (String) response.getBody().get("return");
    }

    @GetMapping("getXml")
    public Person getXml() {
        // HttpHeaders headers = new HttpHeaders();
        // headers.setAccept(List.of(MediaType.APPLICATION_XML));
        // ResponseEntity<Person> response = restClient.exchange("/message/xml",
        // HttpMethod.GET, new HttpEntity(headers), Person.class);

        ResponseEntity<Person> response = restClient.getForEntity("/message/xml", Person.class);
        return response.getBody();
    }
}
