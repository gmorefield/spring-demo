package com.example.springdemo.controllers;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockConfiguration;
import org.springframework.cloud.contract.wiremock.WireMockRestServiceServer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.springdemo.config.WebClientConfig;
import com.example.springdemo.controller.ClientController;
import com.example.springdemo.model.Person;
import com.github.tomakehurst.wiremock.WireMockServer;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = WireMockConfiguration.class)
@AutoConfigureWireMock(port = 0)
public class ClientControllerWireMockTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    private final Person expectedPerson = new Person(123, "John", "Doe");
    private final String MOCK_XML_RESPONSE = "<person><id>123</id><firstName>John</firstName><lastName>Doe</lastName></person>";

    // @Value("${wiremock.server.port}")
    // String wireMockServerPort;

    @Autowired
    WireMockServer wireMockServer;

    ClientController clientController;
    WebClient msgWebClient;

    @BeforeEach
    public void setupTest() throws SSLException {
        // needed to support https returned by WireMockServer.baseUrl().
        // Note: can use @AutoConfigureHttpClient for RestTemplate
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        msgWebClient = new WebClientConfig().messageWebClient(webClientBuilder, wireMockServer.baseUrl());
        restTemplate = new RestTemplate();
        clientController = new ClientController(restTemplate, msgWebClient);

        wireMockServer.resetToDefaultMappings();
    }

    @Test
    public void getXml_returns_Xml() {
        // GIVEN
        mockServer = WireMockRestServiceServer.with(restTemplate)
                .stubs("file:src/test/resources/mappings/dataController.getXml.json")
                .build();

        // WHEN
        String actualXml = clientController.getXml();

        // THEN
        mockServer.verify();
        assertThat(actualXml).isEqualTo(MOCK_XML_RESPONSE);
    }

    @Test
    public void getPerson_returns_Person() {
        // GIVEN
        mockServer = WireMockRestServiceServer.with(restTemplate)
                .stubs("file:src/test/resources/mappings/dataController.getXml.json")
                .build();

        // WHEN
        Person actualPerson = clientController.getPerson();

        // THEN
        mockServer.verify();
        assertThat(actualPerson).usingRecursiveComparison().isEqualTo(expectedPerson);
    }

    @Test
    public void getXmlUsingFlux_returns_Xml() {
        // WHEN
        String actualXml = clientController.getXmlUsingFlux();

        // THEN
        assertThat(actualXml).isEqualTo(MOCK_XML_RESPONSE);
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/data/xml")));
    }

    @Test
    public void getPersonUsingFlux_returns_Person() {
        // WHEN
        Person actualPerson = clientController.getPersonUsingFlux();

        // THEN
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/data/xml")));
        assertThat(actualPerson).usingRecursiveComparison().isEqualTo(expectedPerson);
    }
}
