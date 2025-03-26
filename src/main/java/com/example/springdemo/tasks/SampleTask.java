package com.example.springdemo.tasks;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Component
public class SampleTask {

    @Scheduled(cron = "${sample.task.free-memory.schedule:-}")
    public void heapEcho() {
        System.out.format("*** [%s] FreeMemory = %d%n",
                Thread.currentThread().getName(),
                Runtime.getRuntime().freeMemory());
    }

    public void timeEcho() {
        System.out.format("*** [%s] [%s] Time = %s%n",
                Thread.currentThread().getName(),
                MDC.get("ctx"),
                OffsetDateTime.now());
    }

    @Scheduled(cron = "${sample.task.free-memory.schedule:-}")
    public void uuidEcho() {
        System.out.format("*** [%s] uid = %s%n",
                Thread.currentThread().getName(),
                UUID.randomUUID());
    }

}
