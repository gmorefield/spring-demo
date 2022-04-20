package com.example.springdemo.config;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * This class is used for delaying the startup of the application to experiment
 * with the K8s Liveness check
 */
@Configuration
public class StartupDelay implements CommandLineRunner {
    private static Logger logger = LoggerFactory.getLogger(StartupDelay.class);
    private int delay;

    public StartupDelay(@Value("${sample.command-line.delay}") int delay) {
        this.delay = delay;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("CommandLine: starting");
        TimeUnit.SECONDS.sleep(delay);
        logger.info("CommandLine: finished");
    }
}
