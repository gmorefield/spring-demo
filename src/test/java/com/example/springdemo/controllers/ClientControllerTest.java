package com.example.springdemo.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.example.springdemo.controller.ClientController;
import com.example.springdemo.model.Person;

public class ClientControllerTest {
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ClientController clientController;

    private final Person expectedPerson = new Person(123, "John", "Doe");
    private final String MOCK_XML_RESPONSE = "<person><id>123</id><firstName>John</firstName><lastName>Doe</lastName></person>";

    @BeforeEach
    public void setup() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        clientController = new ClientController(restTemplate, null);
    }

    @Test
    public void getXml_returns_Xml() {
        mockServer.expect(ExpectedCount.once(), requestTo("/data/xml"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(MOCK_XML_RESPONSE, MediaType.APPLICATION_XML));

        String actualXml = clientController.getXml();

        mockServer.verify();
        assertThat(actualXml).isEqualTo(MOCK_XML_RESPONSE);
    }

    @Test
    public void getPerson_returns_Person() {
        mockServer.expect(ExpectedCount.once(), requestTo("/data/xml"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(MOCK_XML_RESPONSE, MediaType.APPLICATION_XML));

        Person actualPerson = clientController.getPerson();

        mockServer.verify();
        assertThat(actualPerson)
                .usingRecursiveComparison()
                .isEqualTo(expectedPerson);
    }
}
