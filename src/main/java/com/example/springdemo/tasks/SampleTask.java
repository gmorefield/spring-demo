package com.example.springdemo.tasks;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.SECONDS;

@Component
public class SampleTask {

    @Scheduled(fixedRate = 5, timeUnit = SECONDS)
    public void heapEcho() {
        System.out.format("*** FreeMemory = %d%n", Runtime.getRuntime().freeMemory());
    }
}
