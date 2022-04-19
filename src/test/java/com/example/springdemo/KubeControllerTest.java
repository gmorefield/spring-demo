package com.example.springdemo;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.springdemo.controller.KubeController;
import com.example.springdemo.service.AvailabilityService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@ActiveProfiles("test")
@WebMvcTest(controllers = {KubeController.class},properties = {"spring.application.admin.enabled=false"})
public class KubeControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    AvailabilityService availabilityService;

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
