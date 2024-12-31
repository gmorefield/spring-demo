package com.example.springdemo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Configures the scheduler to allow multiple concurrent pools.
 * Prevents blocking.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class SchedulerConfig implements SchedulingConfigurer {

    private ScheduledTaskRegistrar taskRegistrar;

    /**
     * Configures the scheduler to allow multiple pools.
     *
     * @param taskRegistrar The task registrar.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        this.taskRegistrar = taskRegistrar;
//        ThreadPoolTaskScheduler threadPoolTaskScheduler = taskScheduler();
//
//        threadPoolTaskScheduler.setPoolSize(POOL_SIZE);
//        threadPoolTaskScheduler.setThreadNamePrefix("scheduled-task-pool-");
//        threadPoolTaskScheduler.initialize();
//
//        taskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
    }
}
