package com.example.springdemo.model;

import java.util.Map;

import lombok.Data;

@Data
public class SampleDto {
    private String name;
    private Map<String, Object> props;
}
