package com.example.springdemo.controller;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springdemo.config.InfoProps;
import com.example.springdemo.model.CloudEventDto;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;

@RestController()
@RequestMapping("/cloud")
public class CloudController {

    private InfoProps props;

    public CloudController(InfoProps props) {
        this.props = props;
    }

    @GetMapping("info")
    public InfoProps getInfo() {
        return props;
    }

    @PostMapping("event/echo")
    public CloudEvent acceptEvent(@RequestBody CloudEvent event) {
        return event;
    }

    @PostMapping("event/echo/data")
    public CloudEventData echoData(@RequestBody CloudEvent event) {
        return event.getData();
    }

    @PostMapping("entity/echo")
    public ResponseEntity<String> echoEntity(HttpEntity<String> requestEntity) {
        return new ResponseEntity<String>(requestEntity.getBody(), requestEntity.getHeaders(), HttpStatus.OK);
    }

    @PostMapping("entity/echo/shell")
    public CloudEventDto echoEntityShell( @RequestHeader HttpHeaders headers, InputStream body) throws StreamReadException, DatabindException, IOException {
        CloudEventDto event = new ObjectMapper().readValue(body, CloudEventDto.class);
        return event;
    }
}
