package com.example.springdemo.config.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@Profile({ "activemq", "mq" })
public class JmsConfig {
    public static final Logger log = LoggerFactory.getLogger(JmsConfig.class);

    public JmsConfig() {
        log.info("JmsConfig initialized");
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        // factory.setConnectionFactory(connectionFactory);
        factory.setErrorHandler(t -> {
            log.error("Caught error {}", t.getMessage());
        });
        factory.setSessionTransacted(true);

        ExponentialBackOff backOff = new ExponentialBackOff(5000, 1.5);
        backOff.setMaxInterval(60000);
        factory.setBackOff(backOff);
        factory.setConcurrency("3-10");

        // factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        return factory;
    }

    @Bean
    @Primary
    public DynamicDestinationResolver dynamicDestinationResolver() {
        return new DynamicDestinationResolver() {
            @Override
            public Destination resolveDestinationName(@Nullable Session session, String destinationName,
                    boolean pubSubDomain) throws JMSException {
                if (destinationName.endsWith("Topic")) {
                    pubSubDomain = true;
                }
                return super.resolveDestinationName(session, destinationName, pubSubDomain);
            }
        };
    }

    @Bean
    public JmsTemplate delayedJmsTemplate(ConnectionFactory connectionFactory,
            DynamicDestinationResolver dynamicDestinationResolver) {
        return new DelayedJmsTemplate(connectionFactory, dynamicDestinationResolver());
    }

    // @Component
    public static class DelayedJmsTemplate extends JmsTemplate {
        public static String DELAY_PROPERTY_NAME = "deliveryDelay";

        public DelayedJmsTemplate(ConnectionFactory connectionFactory,
                DynamicDestinationResolver dynamicDestinationResolver) {
            super(connectionFactory);
            setDestinationResolver(dynamicDestinationResolver);
        }

        @Override
        protected void doSend(MessageProducer producer, Message message) throws JMSException {
            long delay = -1;
            if (message.propertyExists(DELAY_PROPERTY_NAME)) {
                delay = message.getLongProperty(DELAY_PROPERTY_NAME);
            }
            if (delay >= 0) {
                producer.setDeliveryDelay(delay);
            }
            super.doSend(producer, message);
            // if (isExplicitQosEnabled()) {
            // producer.send(message, getDeliveryMode(), getPriority(), getTimeToLive());
            // } else {
            // producer.send(message);
            // }
        }
    }

}
