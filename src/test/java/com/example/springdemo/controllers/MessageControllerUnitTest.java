package com.example.springdemo.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.core.JmsTemplate;

import com.example.springdemo.controller.MessageController;

@ExtendWith(MockitoExtension.class)
public class MessageControllerUnitTest {

    private MessageController controller;

    @Mock
    private ConnectionFactory mockConnectionFactory;
    @Mock
    private JmsListenerEndpointRegistry mockRegistry;
    @Mock
    private JmsTemplate jmsTemplate;

    @Captor
    private ArgumentCaptor<Object> objectMessage;

    @BeforeEach
    public void testSetup() {
        // mockConnectionFactory = mock(ConnectionFactory.class);
        // mockRegistry = mock(JmsListenerEndpointRegistry.class);
        // JmsTemplate jmsTemplate = new JmsTemplate(mockConnectionFactory);
        controller = new MessageController(jmsTemplate, "testQueue", mockRegistry);
    }

    @Test
    public void testSendNow_sendsMessage() throws JMSException {
        controller.sendNow(1, "test");
        verify(jmsTemplate, times(1))
                .convertAndSend(eq("testQueue"), objectMessage.capture(), any());

        assertThat(objectMessage.getValue())
                .isInstanceOfSatisfying(String.class, val -> {
                    assertThat(val).containsPattern("Message at [0-9/:T\\-\\.]+ \\[t-[0-9]+\\]");
                });
    }

}
