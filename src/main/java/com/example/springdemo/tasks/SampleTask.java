package com.example.springdemo.tasks;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SampleTask {

    @Scheduled(cron = "${sample.task.free-memory.schedule:-}")
    public void heapEcho() {
        System.out.format("*** FreeMemory = %d%n", Runtime.getRuntime().freeMemory());
    }
}
