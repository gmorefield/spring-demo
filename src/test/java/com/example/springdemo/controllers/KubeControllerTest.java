package com.example.springdemo.controllers;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class KubeControllerTest extends BaseControllerTest {

    @Test
    public void ignore_should_confirmTimeSpanForDelay() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/kube/ignore")
                .contentType(APPLICATION_JSON)
                .content("5")
                .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Ignoring traffic for 5 seconds")));
    }
}
