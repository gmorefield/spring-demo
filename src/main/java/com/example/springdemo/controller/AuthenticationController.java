package com.example.springdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {
    
    @GetMapping("/authorize")
    public String authorize() {
        return "secure";
    }
}
