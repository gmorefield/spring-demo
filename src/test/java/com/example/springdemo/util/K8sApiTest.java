package com.example.springdemo.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class K8sApiTest {
    @Test
    public void testEndpointApiResponse() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        Endpoints endpoints = objectMapper.readValue(new File("src/test/resources/__files/k8s.endpoints.json"), Endpoints.class);
        Assertions.assertThat(endpoints.subsets.stream()
                        .map(s -> s.addresses)
                        .flatMap(Collection::stream)
                        .map(EndpointAddress::ip)
                        .collect(Collectors.toList()))
                .containsExactlyInAnyOrder("10.1.2.173", "10.1.2.174");
    }

    public record ObjectMeta(
            String name,
            String generateName,
            String namespace,
            Map<String, String> labels,
            Map<String, String> annotations
    ) {
    }

    public record EndpointAddress(
            String ip, String hostname, String nodeName
    ) {
    }

    public record EndpointPort(
            int port, String protocol, String name, String appProtocol
    ) {
    }

    public record EndpointSubset(
            List<EndpointAddress> addresses,
            List<EndpointAddress> notReadyAddresses,
            List<EndpointPort> ports
    ) {
    }

    public record Endpoints(
            String apiVersion, String kind,
            ObjectMeta metadata,
            List<EndpointSubset> subsets
    ) {
    }
}
