package com.example.springdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean("msgWebClient")
    public WebClient messageWebClient(WebClient.Builder builder,
            @Value("${base.url:http://localhost:8080}") String baseUrl) {
                
        HttpClient httpClient = HttpClient.create()
                // .secure(sslSpec -> ...);
                // .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                // .doOnConnected(connection -> {
                //     connection.addHandlerLast(new ReadTimeoutHandler(TIMEOUT, MILLISECONDS));
                //     connection.addHandlerLast(new WriteTimeoutHandler(TIMEOUT, MILLISECONDS));
                // })
                .compress(false);

        return builder
                .baseUrl(baseUrl)
                .exchangeStrategies(ExchangeStrategies.builder().codecs((configurer) -> {
                    // used to decode xml responses to objects
                    configurer.defaultCodecs().jaxb2Encoder(new Jaxb2XmlEncoder());
                    configurer.defaultCodecs().jaxb2Decoder(new Jaxb2XmlDecoder());
                }).build())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public ReactorResourceFactory reactorResourceFactory() {
        return new ReactorResourceFactory();
    }
}
