package com.example.springdemo.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Bean("msgRestClient")
    public RestTemplate messageRestClient(RestTemplateBuilder builder) {
        // ClientHttpRequestInterceptor interceptor = (HttpRequest request, byte[] body,
        // ClientHttpRequestExecution execution) -> {
        // request.getHeaders().setAccept(List.of(MediaType.APPLICATION_XML));
        // return execution.execute(request, body);
        // };

        return builder
                // .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
                .additionalInterceptors((HttpRequest request, byte[] body, ClientHttpRequestExecution execution) -> {
                    // request.getHeaders().setAccept(List.of(MediaType.APPLICATION_XML));
                    return execution.execute(request, body);
                })
                .rootUri("http://localhost:8080")
                .build();
    }
}
