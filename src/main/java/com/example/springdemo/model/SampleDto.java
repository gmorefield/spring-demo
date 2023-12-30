package com.example.springdemo.model;

import lombok.Data;

import java.util.Map;

@Data
public class SampleDto {
    private String name;
    private Map<String, Object> props;
}
