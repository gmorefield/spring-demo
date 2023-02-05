package com.example.springdemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.ModelAndView;

import com.example.springdemo.model.Person;

@RequestMapping("/deception")
@RestController
public class DeceptionController {

    private WebClient webClient;

    public DeceptionController(WebClient webClient) {
        this.webClient = webClient;
    }

    @RequestMapping(value = "/runAround", method = RequestMethod.GET)
    public ModelAndView runAround() {
        ResponseEntity<Person> response = webClient.get()
                .uri("/data/xml")
                .retrieve()
                .toEntity(Person.class)
                .block();

        return new ModelAndView("redirect:" + "/deception/landing");
    }

    @RequestMapping(value = "/landing", method = RequestMethod.GET)
    public ResponseEntity<String> entryPoint() {

        return ResponseEntity.ok("Made it");
    }
}
