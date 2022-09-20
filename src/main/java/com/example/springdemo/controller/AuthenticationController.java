package com.example.springdemo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {
    
    @RequestMapping("/authorize")
    public String authorize() {
        return "secure";
    }
}
