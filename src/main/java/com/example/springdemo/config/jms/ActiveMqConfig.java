package com.example.springdemo.config.jms;

import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class ActiveMqConfig {
    public static final Logger log = getLogger(lookup().lookupClass());

    @Configuration
    @ConditionalOnExpression("T(org.springframework.util.ObjectUtils).isEmpty('${spring.activemq.broker-url:}')")
    @ImportAutoConfiguration(exclude = { ActiveMQAutoConfiguration.class })
    public static class Disabled {
        public Disabled() {
            log.info("ActiveMq Disabled");
        }
    }

    @Configuration
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${spring.activemq.broker-url:}')")
    public static class Enabled {
        public Enabled() {
            log.info("ActiveMq Enabled");
        }
    }

}
