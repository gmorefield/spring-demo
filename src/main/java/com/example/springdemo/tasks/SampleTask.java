package com.example.springdemo.tasks;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class SampleTask {

    @Scheduled(cron = "${sample.task.free-memory.schedule:-}")
    public void heapEcho() {
        System.out.format("*** [%s] FreeMemory = %d%n",
                Thread.currentThread().getName(),
                Runtime.getRuntime().freeMemory());
    }

    public void timeEcho() {
        System.out.format("*** [%s] Time = %s%n",
                Thread.currentThread().getName(),
                OffsetDateTime.now());
    }

    @Scheduled(cron = "${sample.task.free-memory.schedule:-}")
    public void uuidEcho() {
        System.out.format("*** [%s] uid = %s%n",
                Thread.currentThread().getName(),
                UUID.randomUUID());
    }

}
