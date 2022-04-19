package com.example.springdemo.service;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityService {
    private ApplicationEventPublisher eventPublisher;

    public AvailabilityService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Async
    public void ignoreTraffic(final int seconds) {
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (Exception ignored) {
        }
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
    }
}
