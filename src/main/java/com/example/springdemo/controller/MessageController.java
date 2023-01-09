package com.example.springdemo.controller;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/message")
@Profile({ "activemq", "mq" })
public class MessageController {
    private static Logger log = LoggerFactory.getLogger(MessageController.class);

    private JmsTemplate jmsTemplate;
    private String defaultQueueName;
    private Queue<String> messages = new ConcurrentLinkedQueue<>();
    private Queue<String> subMessages = new ConcurrentLinkedQueue<>();
    private JmsListenerEndpointRegistry registry;
    private Random random = new SecureRandom();

    public MessageController(JmsTemplate jmsTemplate,
            @Value("${springdemo.default.queueName}") String defaultQueueName,
            JmsListenerEndpointRegistry registry) {
        this.jmsTemplate = jmsTemplate;
        this.defaultQueueName = defaultQueueName;
        this.registry = registry;
        MDC.put("random", String.valueOf(Math.random()));
        log.info("MessageController initialized");
    }

    @GetMapping("sendNow")
    public String sendNow(@RequestHeader(name = "x-count", defaultValue = "1") int count,
    @RequestHeader(name="x-env", defaultValue = "${sample.env}") String env) {
        // jmsTemplate.setDeliveryDelay(30000L);
        IntStream.rangeClosed(1, count)
                .parallel()
                .forEach(i -> {
                    jmsTemplate.convertAndSend(defaultQueueName, "Message"
                            + (count == 1 ? "" : " (" + i + "/" + count + ")")
                            + " at " + LocalDateTime.now()
                            + " [t-" + Thread.currentThread().getId() + "]",
                            message -> {
                                message.setStringProperty("env", env);;
                                return message;
                            });
                });

        return "success";
    }

    @GetMapping("publishNow")
    public String publishNow(@RequestHeader(name = "x-count", defaultValue = "1") int count) {
        IntStream.rangeClosed(1, count)
                .parallel()
                .forEach(i -> {
                    jmsTemplate.convertAndSend("sampleTopic", "Message"
                            + (count == 1 ? "" : " (" + i + "/" + count + ")")
                            + " at " + LocalDateTime.now()
                            + " [t-" + Thread.currentThread().getId() + "]");
                });

        return "success";
    }

    @GetMapping("sendLater")
    public String sendLater() {
        // jmsTemplate.setDeliveryDelay(30000L);
        jmsTemplate.send("later", session -> {
            TextMessage msg = session.createTextMessage("Message at " + LocalDateTime.now());
            msg.setLongProperty("deliveryDelay", 30000);
            return msg;
        });
        // jmsTemplate.convertAndSend("later", "Message at " + LocalDateTime.now());
        return "success";
    }

    @GetMapping("sendBackoff")
    public String sendBackoff() {
        jmsTemplate.send("tbd", session -> {
            TextMessage msg = session.createTextMessage("Message at " + LocalDateTime.now());
            return msg;
        });
        return "success";
    }

    @GetMapping("getOne")
    public String getOne() {
        String msg = messages.poll();
        return msg == null ? "tbd" : msg;
    }

    @GetMapping("getAll")
    public List<String> getAll() {
        ArrayList<String> allMessages = new ArrayList<>(messages.size());
        String msg;
        while ((msg = messages.poll()) != null) {
            allMessages.add(msg);
        }
        return allMessages;
    }

    @GetMapping("getAllSubscriptions")
    public List<String> getAllSubscriptions() {
        ArrayList<String> allMessages = new ArrayList<>(subMessages.size());
        String msg;
        while ((msg = subMessages.poll()) != null) {
            allMessages.add(msg);
        }
        return allMessages;
    }

    @GetMapping("count")
    public int countPendingMessages() {
        // to an Integer because the response of .browse may be null
        Integer totalPendingMessages = this.jmsTemplate.browse(defaultQueueName,
                (session, browser) -> Collections.list(browser.getEnumeration()).size());

        return totalPendingMessages == null ? 0 : totalPendingMessages;
    }

    @GetMapping("registry/listeners")
    public List<String> getListeners() {
        return registry.getListenerContainerIds()
                .stream().map(id -> {
                    DefaultMessageListenerContainer c = (DefaultMessageListenerContainer) registry
                            .getListenerContainer(id);
                    return id + ":" + c.getActiveConsumerCount() + "/" + c.getConcurrentConsumers();
                }).collect(Collectors.toList());
    }

    @GetMapping("registry/start")
    public String startListeners() {
        registry.getListenerContainers()
                .forEach(c -> c.start());
        return "success";
    }

    @GetMapping("registry/stop")
    public String stopListeners() {
        registry.getListenerContainers()
                .forEach(c -> c.stop());
        return "success";
    }

    @JmsListener(id = "nowListener", destination = "${springdemo.default.queueName}", selector = "env = '${sample.env}'")
    public void activeMqListener(TextMessage message, Session session) throws Exception {
        log.info("now session [transacted={}],[acknowledgeMode={}],[redelivered={},[priority={}]]", session.getTransacted(),
                session.getAcknowledgeMode(), message.getJMSRedelivered(), message.getJMSPriority());
        // if (message.getJMSRedelivered()) {
        String msg = message.getText() + " -- Received at " + LocalDateTime.now()
                + " [t-" + Thread.currentThread().getId() + "]";
        messages.add(msg);
        jmsTemplate.convertAndSend("sampleMessage", msg);
        if (random.nextInt(100) > 90) {
            log.warn("message {} rolled back", message.getJMSMessageID());
            session.rollback();
        } else {
            session.commit();
        }
    }

    @JmsListener(id = "subscribeListener", destination = "sampleTopic", concurrency = "1")
    public void topicListener(TextMessage message, Session session) throws Exception {
        log.info("topic session [transacted={}],[acknowledgeMode={}]", session.getTransacted(),
                session.getAcknowledgeMode());
        // if (message.getJMSRedelivered()) {
        subMessages.add(message.getText() + " -- Received at " + LocalDateTime.now()
                + " [t-" + Thread.currentThread().getId() + "]");
        session.commit();
        // } else {
        // session.rollback();
        // }
        // message.acknowledge();
    }

    @JmsListener(id = "laterListener", destination = "later")
    public void laterMqListener(@Payload TextMessage message, @Headers Map headers, Session session) throws Exception {
        log.info("later session [transacted={}][acknowledgeMode={}][headers={}][deliveryCount={}]",
                session.getTransacted(),
                session.getAcknowledgeMode(), headers.toString(), message.getIntProperty("JMSXDeliveryCount"));
        session.rollback();
        throw new RuntimeException("simulated listener failure");
    }

    // @JmsListener(destination = "tbd")
    // public void tbdMqListener(@Payload TextMessage message, @Headers Map headers,
    // Session session) throws Exception {
    // log.info("tbd session
    // [transacted={}][acknowledgeMode={}][headers={}][deliveryCount={}]",
    // session.getTransacted(),
    // session.getAcknowledgeMode(), headers.toString(),
    // message.getIntProperty("JMSXDeliveryCount"));
    // // session.rollback();
    // // throw new RuntimeException("simulated listener failure");
    // }
}
