package com.example.springdemo.controller;

import com.example.springdemo.config.StateMachineConfig;
import com.example.springdemo.config.StateMachineConfig.Events;
import com.example.springdemo.config.StateMachineConfig.States;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/state")
@RestController
@ConditionalOnProperty(name="spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class StateController {
    private final StateMachine<StateMachineConfig.States, StateMachineConfig.Events> stateMachine;

    public StateController(StateMachine<States, Events> stateMachine) {
        this.stateMachine = stateMachine;
    }

    @GetMapping("demo")
    public void demo() {
        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(StateMachineConfig.Events.E1).build())).subscribe();
        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(StateMachineConfig.Events.E2).build())).subscribe();
        // stateMachine.sendEvent(StateMachineConfig.Events.E1);
        // stateMachine.sendEvent(StateMachineConfig.Events.E2);
    }
}
