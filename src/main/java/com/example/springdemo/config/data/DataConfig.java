package com.example.springdemo.config.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@Configuration
@EnableJdbcRepositories(basePackages = "com.example.springdemo.data")
public class DataConfig {
    
}