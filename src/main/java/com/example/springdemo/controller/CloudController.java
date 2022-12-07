package com.example.springdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springdemo.config.InfoProps;

import io.cloudevents.CloudEvent;

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
}
