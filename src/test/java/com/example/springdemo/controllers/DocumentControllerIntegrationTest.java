package com.example.springdemo.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
public class DocumentControllerIntegrationTest {
    @Autowired
    TestRestTemplate testRestTemplate;

    @Test
    public void testUpload() {
        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("meta", Collections.singletonMap("metakey", "value"));
        parameters.add("file", new FileSystemResource("README.md"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, headers);

        ResponseEntity<String> response = testRestTemplate.exchange("/document/store", HttpMethod.POST, entity,
                String.class, "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

}
