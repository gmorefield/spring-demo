package com.example.springdemo.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
public class MtomClientConfig {

    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.example.springdemo.soap.model");
        marshaller.setMtomEnabled(true);
        return marshaller;
    }

    @Bean
    public RecordStorageClient recordStorageClient() {
        Jaxb2Marshaller marshaller = marshaller();

        RecordStorageClient client = new RecordStorageClient();
        client.setDefaultUri("http://localhost:8080/ws");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }
}
