package com.example.springdemo.config.jms;

import com.ibm.mq.spring.boot.MQAutoConfiguration;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class IbmMqConfig {
    public static final Logger log = getLogger(lookup().lookupClass());

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
