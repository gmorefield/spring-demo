package com.example.springdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("msgWebClient")
    public WebClient messageWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://localhost:8080")
                .exchangeStrategies(ExchangeStrategies.builder().codecs((configurer) -> {
                    System.out.println(configurer.defaultCodecs().toString());
                    // configurer.defaultCodecs().jaxb2Encoder(new Jaxb2XmlEncoder());
                    // configurer.defaultCodecs().jaxb2Decoder(new Jaxb2XmlDecoder());
                }).build())
                .build();
    }
}
