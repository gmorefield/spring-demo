package com.example.springdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("msgWebClient")
    public WebClient messageWebClient(WebClient.Builder builder, @Value("${base.url:http://localhost:8080}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .exchangeStrategies(ExchangeStrategies.builder().codecs((configurer) -> {
                    // used to decode xml responses to objects
                    configurer.defaultCodecs().jaxb2Encoder(new Jaxb2XmlEncoder());
                    configurer.defaultCodecs().jaxb2Decoder(new Jaxb2XmlDecoder());
                }).build())
                .build();
    }
}
