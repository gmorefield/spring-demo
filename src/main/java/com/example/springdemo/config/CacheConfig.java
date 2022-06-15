package com.example.springdemo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "sample.cache.enabled", havingValue = "true", matchIfMissing = false)
public class CacheConfig implements ApplicationContextAware {
    private static Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        logger.info("cache-manager-exists: {}", applicationContext.containsBean("cacheManager"));
    }
}
