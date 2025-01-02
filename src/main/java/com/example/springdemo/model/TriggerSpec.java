package com.example.springdemo.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.Duration;

@ToString(doNotUseGetters = true)
@Data
@Builder
public class TriggerSpec {
    private int id;
    private TriggerType triggerType;
    private String cronExpression;
    private Duration interval;
    private Duration initialDelay;
    private String targetBean;
    private String targetMethod;
    private boolean enabled;

    public enum TriggerType {
        CRON, FIXED_RATE, FIXED_DELAY, ONE_TIME
    }
}
