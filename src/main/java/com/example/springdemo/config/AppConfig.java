package com.example.springdemo.config;

import com.example.springdemo.filter.RequestLoggingFilter;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Configuration
@EnableRetry
@Slf4j
public class AppConfig implements ApplicationContextAware {
    /**
     * The easiest approach is to add @Component to Filter definition, but a FilterRegistrationBean
     * can also be used to register Filters with customization (e.g. url patterns)
     */
    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> filterRegistrationBean() {
        FilterRegistrationBean<RequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        RequestLoggingFilter customFilter = new RequestLoggingFilter();

        log.info("Registering person filter");
        registrationBean.setFilter(customFilter);
        registrationBean.addUrlPatterns("/person");
        return registrationBean;
    }

    @Async
    @EventListener
    public void onEvent(AvailabilityChangeEvent<LivenessState> event) {
        // comment
        switch (event.getState()) {
            case BROKEN:
                // notify others
                log.warn("Availability broken");
                break;
            case CORRECT:
                // we're back
                log.info("Availability restored");
        }
    }

    @Async
    @EventListener
    public void onStateChange(AvailabilityChangeEvent<ReadinessState> event) {
        switch (event.getState()) {
            case ACCEPTING_TRAFFIC:
                log.warn("Accepting traffic");
                break;
            case REFUSING_TRAFFIC:
                log.warn("Refusing traffic");
                break;
        }
    }

    @Async
    @EventListener
    public void onEnvironmentChange(EnvironmentChangeEvent event) {
        log.info("Environment Changed for keys={}", event.getKeys().toString());
    }

    @Async
    @EventListener
    public void onLeaderGranted(OnGrantedEvent event) {
        log.info("Leader Granted with role={}", event.getRole());
    }

    @Async
    @EventListener
    public void onLeaderRevoked(OnRevokedEvent event) {
        log.info("Leader Revoked with keys={}", event.getRole());
    }

    @Component("sampleRetryListener")
    public static class SampleRetryListener implements RetryListener {
        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
            // method is called one time before all retries
            return RetryListener.super.open(context, callback);
        }

        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            // method called once at the end
            RetryListener.super.close(context, callback, throwable);
        }

        @Override
        public <T, E extends Throwable> void onSuccess(RetryContext context, RetryCallback<T, E> callback, T result) {
            RetryListener.super.onSuccess(context, callback, result);
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            log.info("Retry {} error {}", context.getRetryCount(), throwable.getClass().getSimpleName());
            RetryListener.super.onError(context, callback, throwable);
        }
    }

    //    @SuppressWarnings("RedundantSuppression")
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DataSource ds = applicationContext.getBean(DataSource.class);
        if (ds instanceof HikariDataSource) {
            @SuppressWarnings("resource")   // DataSource lifecycle managed by Spring
            HikariDataSource hds = (HikariDataSource) ds;
            log.info("maximum-pool-size: {}", hds.getMaximumPoolSize());
        }
        log.info("cache-manager-exists: {}", applicationContext.containsBean("cacheManager"));
    }
}
