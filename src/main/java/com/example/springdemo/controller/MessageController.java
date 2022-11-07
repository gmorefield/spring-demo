package com.example.springdemo.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/message")
@Profile({ "activemq", "mq" })
public class MessageController {
    private static Logger log = LoggerFactory.getLogger(MessageController.class);

    private JmsTemplate jmsTemplate;
    private String defaultQueueName;
    private List<String> messages = new ArrayList<>();

    public MessageController(JmsTemplate jmsTemplate,
            @Value("${springdemo.default.queueName}") String defaultQueueName) {
        this.jmsTemplate = jmsTemplate;
        this.defaultQueueName = defaultQueueName;
        log.info("MessageController initialized");
    }

    @GetMapping("sendNow")
    public String sendNow() {
        jmsTemplate.convertAndSend(defaultQueueName, "Message at " + LocalDateTime.now());
        return "success";
    }

    @GetMapping("getOne")
    public String getOne() {
        return messages.isEmpty() ? "tbd" : messages.remove(0);
    }

    @JmsListener(destination = "${springdemo.default.queueName}")
    public void activeMqListener(TextMessage message) throws Exception {
        messages.add(message.getText());
        message.acknowledge();
    }
}
