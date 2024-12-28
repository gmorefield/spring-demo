package com.example.springdemo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * This class is used for delaying the startup of the application to experiment
 * with the K8s Liveness check
 */
@Configuration
@ConditionalOnProperty(name="spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class StartupDelay implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(StartupDelay.class);
    private final int delay;

    public StartupDelay(SampleProperties sampleProperties) {
        this.delay = sampleProperties.getCommandLine().getDelay();
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("CommandLine: starting");
        TimeUnit.SECONDS.sleep(delay);
        logger.info("CommandLine: finished");
    }
}
