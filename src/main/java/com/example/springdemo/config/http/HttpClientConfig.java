package com.example.springdemo.config.http;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;

@Configuration
public class HttpClientConfig {
    @Bean
    public HttpClient httpClient() {
        return HttpClient.create()
                // .secure(sslSpec -> sslSpec.);
                // .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                // .doOnConnected(connection -> {
                // connection.addHandlerLast(new ReadTimeoutHandler(TIMEOUT, MILLISECONDS));
                // connection.addHandlerLast(new WriteTimeoutHandler(TIMEOUT, MILLISECONDS));
                // })
                .compress(false);

    }
}
