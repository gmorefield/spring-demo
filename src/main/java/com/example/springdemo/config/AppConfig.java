package com.example.springdemo.config;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.springframework.scheduling.annotation.Async;

import com.example.springdemo.filter.RequestLoggingFilter;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AppConfig implements ApplicationContextAware {
    /** 
     * The easiest approach is to add @Component to Filter definition, but a FilterRegistrationBean
     * can also be used to register Filters with customization (eg. url patterns)
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DataSource ds = applicationContext.getBean(DataSource.class);
        if (ds instanceof HikariDataSource) {
            @SuppressWarnings("resource")   // DataSource lifecyle managed by Spring
            HikariDataSource hds = (HikariDataSource) ds;
            log.info("maximum-pool-size: {}", hds.getMaximumPoolSize());
        }
        logger.info("cache-manager-exists: {}", applicationContext.containsBean("cacheManager"));
    }

    // @Bean
    // public HttpMessageConverter<CloudEvent> cloudEventHttpMessageConverter() {
    //     return new CloudEventHttpMessageConverter();
    // }
}
