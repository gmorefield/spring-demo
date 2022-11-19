package com.example.springdemo.config.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

import com.ibm.mq.spring.boot.MQAutoConfiguration;

public class IbmMqConfig {
    public static final Logger log = LoggerFactory.getLogger(IbmMqConfig.class);

    @Configuration
    @ConditionalOnExpression("T(org.springframework.util.ObjectUtils).isEmpty('${ibm.mq.queueManager:}')")
    @ImportAutoConfiguration(exclude = { MQAutoConfiguration.class })
    public static class Disabled {
        public Disabled() {
            log.info("IbmMqConfig Disabled");
        }
    }

    @Configuration
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${ibm.mq.queueManager:}')")
    public static class Enabled {
        public Enabled() {
            log.info("IbmMqConfig Enabled");
        }
    }

}
