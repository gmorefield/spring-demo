package com.example.springdemo.config;

import javax.sql.DataSource;

import com.example.springdemo.filter.RequestLoggingFilter;
import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AppConfig implements ApplicationContextAware {
    private static Logger logger = LoggerFactory.getLogger(AppConfig.class);

    /** 
     * The easiest approach is to add @Component to Filter definition, but a FilterRegistrationBean
     * can also be used to register Filters with customization (eg. url patterns)
     */
    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> filterRegistrationBean() {
        FilterRegistrationBean<RequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        RequestLoggingFilter customFilter = new RequestLoggingFilter();

        registrationBean.setFilter(customFilter);
        registrationBean.addUrlPatterns("/person");
        return registrationBean;
    }

    @Async
    @EventListener
    public void onEvent(AvailabilityChangeEvent<LivenessState> event) {
        switch (event.getState()) {
            case BROKEN:
                // notify others
                logger.warn("Availability broken");
                break;
            case CORRECT:
                // we're back
                logger.info("Availability restored");
        }
    }

    @Async
    @EventListener
    public void onStateChange(AvailabilityChangeEvent<ReadinessState> event) {
        switch (event.getState()) {
            case ACCEPTING_TRAFFIC:
                logger.warn("Accepting traffic");
                break;
            case REFUSING_TRAFFIC:
                logger.warn("Refusing traffic");
                break;
        }
    }

    @Async
    @EventListener
    public void onEnvironmentChange(EnvironmentChangeEvent event) {
        logger.info("Environment Changed for keys={}", event.getKeys().toString());
    }
    @Async
    @EventListener
    public void onLeaderGranted(OnGrantedEvent event) {
        logger.info("Leader Granted with role={}", event.getRole());
    }
    @Async
    @EventListener
    public void onLeaderRevoked(OnRevokedEvent event) {
        logger.info("Leader Revoked with keys={}", event.getRole());
    }



    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DataSource ds = applicationContext.getBean(DataSource.class);
        if (ds instanceof HikariDataSource) {
            @SuppressWarnings("resource")   // DataSource lifecyle managed by Spring
            HikariDataSource hds = (HikariDataSource) ds;
            logger.info("maximum-pool-size: {}", hds.getMaximumPoolSize());
        }
    }
}
