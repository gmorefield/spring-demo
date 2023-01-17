package com.example.springdemo.controller;

import static org.springframework.http.HttpStatus.OK;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("data")
public class DataController {
    @GetMapping(path = "xml", produces = { MediaType.APPLICATION_XML_VALUE })
    public String xml() {
        return "<person><id>123</id><firstName>John</firstName><lastName>Doe</lastName></person>";
    }

    @GetMapping(path = "xmlInJson", produces = { MediaType.APPLICATION_JSON_VALUE })
    public String xmlInJson() {
        return "{\"return\": \"" + xml() + "\" }";
    }

    @GetMapping(path = "getStatus")
    public ResponseEntity<?> getStatus(@RequestParam(name = "code", required = false) Optional<Integer> statusCode)
            throws Exception {

        return ResponseEntity.status(statusCode.orElse(OK.value())).build();
    }
}