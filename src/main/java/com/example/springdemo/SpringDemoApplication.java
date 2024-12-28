package com.example.springdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ObjectUtils;

@SpringBootApplication
public class SpringDemoApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SpringDemoApplication.class);
        ApplicationContext context = app.run(args);

        if (context.getEnvironment().matchesProfiles("job") || ObjectUtils.containsElement(args, "--mode=job")) {
            System.exit(SpringApplication.exit(context));
        }
    }
}
