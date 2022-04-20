package com.example.springdemo.controllers;

import com.example.springdemo.controller.KubeController;
import com.example.springdemo.controller.PersonController;
import com.example.springdemo.data.PersonRepository;
import com.example.springdemo.service.AvailabilityService;

import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@Tag("appcontext")
@Tag("controller")
@ActiveProfiles("test")
@WebMvcTest(controllers = {
        KubeController.class,
        PersonController.class
}, properties = {
        "spring.application.admin.enabled=false"
})
public abstract class BaseControllerTest {
    @Autowired
    protected MockMvc mvc;

    @MockBean
    protected AvailabilityService availabilityService;

    @MockBean
    protected PersonRepository personRepository;
}