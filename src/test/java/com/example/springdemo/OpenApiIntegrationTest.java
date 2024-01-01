package com.example.springdemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"h2", "test"})
public class OpenApiIntegrationTest {
    @Autowired
    TestRestTemplate testRestTemplate;

    @Test
    public void swaggerUI_should_be_available() throws Exception {
        ResponseEntity<String> response = testRestTemplate.exchange("/swagger-ui/index.html",
                HttpMethod.GET, null,
                String.class, "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("id=\"swagger-ui\"");
    }

    @Test
    public void apiSchema_should_be_available() throws Exception {
        ResponseEntity<String> response = testRestTemplate.exchange("/v3/api-docs",
                HttpMethod.GET, null,
                String.class, "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Kitchen sink of various Spring components");
    }

}
