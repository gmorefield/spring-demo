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
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.springframework.http.HttpStatus.CONFLICT;

@RestController
@RequestMapping("/tasks")
public class ScheduleController {
    private final SchedulerConfig config;
    private final SampleTask sampleTask;
    private final TaskScheduler taskScheduler;

    public ScheduleController(SchedulerConfig config, TaskScheduler taskScheduler, Optional<SampleTask> sampleTask) {
        this.config = config;
        this.sampleTask = sampleTask.orElse(null);
        this.taskScheduler = taskScheduler;
    }

    @GetMapping("/schedules")
    public Collection<ScheduledFuture<?>> schedules() {
        return config.getScheduledTasks().values();
    }

    @GetMapping("/schedules/sample/stop")
    public ResponseEntity<?> stopEcho() {
        if (sampleTask != null) {
            config.getScheduledTasks().get(sampleTask).cancel(false);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(CONFLICT).build();
        }
    }

    @GetMapping("/schedules/sample/start")
    public ResponseEntity<?> startEcho() throws NoSuchMethodException {
        if (sampleTask != null) {
            ScheduledMethodRunnable runnable = new ScheduledMethodRunnable(sampleTask, "heapEcho");
            taskScheduler.scheduleAtFixedRate(runnable, Duration.ofSeconds(3));
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(CONFLICT).build();
        }
    }
}
