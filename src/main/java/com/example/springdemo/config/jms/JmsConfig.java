package com.example.springdemo.config.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.backoff.ExponentialBackOff;

import javax.jms.*;

import static java.lang.invoke.MethodHandles.lookup;
import static org.springframework.boot.context.properties.PropertyMapper.get;
import static org.springframework.jms.support.converter.MessageType.TEXT;

@Configuration
@Profile({ "activemq", "mq" })
public class JmsConfig {
    public static final Logger log = LoggerFactory.getLogger(lookup().lookupClass());

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
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    public JmsTemplate delayedJmsTemplate(ConnectionFactory connectionFactory,
            DynamicDestinationResolver dynamicDestinationResolver,
            ObjectProvider<MessageConverter> messageConverter) {

        PropertyMapper map = get();
        JmsTemplate template = new DelayedJmsTemplate(connectionFactory, dynamicDestinationResolver());
        map.from(messageConverter::getIfUnique).whenNonNull().to(template::setMessageConverter);
        return template;
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
