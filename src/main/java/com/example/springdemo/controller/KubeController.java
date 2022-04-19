package com.example.springdemo.controller;

import com.example.springdemo.service.AvailabilityService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("kube")
public class KubeController {
    private AvailabilityService availabilityService;

    public KubeController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("ignore")
    public String ignoreTraffic(@RequestBody int seconds) {
        availabilityService.ignoreTraffic(seconds);
        return String.format("Ignoring traffic for %x seconds", seconds);
    }
}
