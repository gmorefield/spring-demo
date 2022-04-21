package com.example.springdemo.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AppConfig implements ApplicationContextAware {
    private static Logger logger = LoggerFactory.getLogger(AppConfig.class);

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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DataSource ds = applicationContext.getBean(DataSource.class);
        if (ds instanceof HikariDataSource) {
            HikariDataSource hds = (HikariDataSource) ds;
            LoggerFactory.getLogger(DataSourceConfig.class).info("maximum-pool-size: {}", hds.getMaximumPoolSize());
        }
    }
}
