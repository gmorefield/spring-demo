package com.example.springdemo.controller;

import com.example.springdemo.service.AvailabilityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@RestController()
@RequestMapping("kube")
@Validated
public class KubeController {
    private final AvailabilityService availabilityService;

    public KubeController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("ignore")
    public String ignoreTraffic(@RequestBody @Validated @Min(0) @Max(30) int seconds) {
        availabilityService.ignoreTraffic(seconds);
        return String.format("Ignoring traffic for %d seconds", seconds);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> onValidationError(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
