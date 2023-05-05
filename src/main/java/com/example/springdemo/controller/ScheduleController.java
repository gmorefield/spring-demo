package com.example.springdemo.controller;

import com.example.springdemo.config.SchedulerConfig;
import com.example.springdemo.tasks.SampleTask;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;

@RestController
@RequestMapping("/tasks")
public class ScheduleController {
    private SchedulerConfig config;
    private SampleTask sampleTask;
    private TaskScheduler taskScheduler;

    public ScheduleController(SchedulerConfig config, SampleTask echoTask, TaskScheduler taskScheduler) {
        this.config = config;
        this.sampleTask = echoTask;
        this.taskScheduler = taskScheduler;
    }

    @GetMapping("/schedules")
    public Collection<ScheduledFuture<?>> schedules() {
        return config.getScheduledTasks().values();
    }

    @GetMapping("/schedules/sample/stop")
    public ResponseEntity stopEcho() {
        config.getScheduledTasks().get(sampleTask).cancel(false);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/schedules/sample/start")
    public ResponseEntity startEcho() throws NoSuchMethodException {
        ScheduledMethodRunnable runnable = new ScheduledMethodRunnable(sampleTask,"heapEcho");
        taskScheduler.scheduleAtFixedRate(runnable, Duration.ofSeconds(3));
        return ResponseEntity.noContent().build();
    }
}
