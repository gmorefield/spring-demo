package com.example.springdemo.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import java.time.Duration;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * Configures the scheduler to allow multiple concurrent pools.
 * Prevents blocking.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name="spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class SchedulerConfig implements SchedulingConfigurer {

    private final Map<Object, ScheduledFuture<?>> scheduledTasks = new IdentityHashMap<>();

    private static final int POOL_SIZE = 10;

    public Map<Object, ScheduledFuture<?>> getScheduledTasks() {
        return scheduledTasks;
    }

    /**
     * Configures the scheduler to allow multiple pools.
     *
     * @param taskRegistrar The task registrar.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = taskScheduler();

        threadPoolTaskScheduler.setPoolSize(POOL_SIZE);
        threadPoolTaskScheduler.setThreadNamePrefix("scheduled-task-pool-");
        threadPoolTaskScheduler.initialize();

        taskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
    }

    @Bean
    @Primary
    public ThreadPoolTaskScheduler taskScheduler() {
        return new CustomTaskScheduler();
    }

    class CustomTaskScheduler extends ThreadPoolTaskScheduler {

        @NotNull
        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable task, Duration duration) {
            ScheduledFuture<?> future = super.scheduleAtFixedRate(task, duration);

            ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task;
            scheduledTasks.put(runnable.getTarget(), future);

            return future;
        }

        @NotNull
        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable task, @NotNull Instant instant,
                Duration duration) {
            ScheduledFuture<?> future = super.scheduleAtFixedRate(task, instant, duration);

            ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task;
            scheduledTasks.put(runnable.getTarget(), future);

            return future;
        }
    }
}
