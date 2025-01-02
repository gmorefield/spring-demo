package com.example.springdemo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1EndpointAddress;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.util.Config;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.HandlerMapping;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Profile("kubernetes")
@RestController
@RequestMapping("/broadcast")
@Slf4j
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class BroadcastController {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private BroadcastController(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @GetMapping("/servers/{serviceName}")
    public List<String> getServers(@PathVariable String serviceName) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(serviceName);
        return Arrays.stream(addresses).map(InetAddress::toString).collect(Collectors.toList());
    }

    @GetMapping("/info/{serviceName}")
    public List<Map<?, ?>> getInfo(@PathVariable String serviceName) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(serviceName);
        return Arrays.stream(addresses).map(addr -> {
            Map<?, ?> response = webClient.get()
                    .uri("http://{host}:8080/actuator/info", addr.getHostAddress())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response;
        }).collect(Collectors.toList());
    }

    @GetMapping("/proxy/{serviceName}/**")
    public List<Map<?, ?>> getProxy(@PathVariable String serviceName, HttpServletRequest request)
            throws UnknownHostException {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        log.info("proxy path = {}", path);

        int thirdSlash = ordinalIndexOf(path, "/", 4);
        String restOfUrl = thirdSlash == -1 ? "" : path.substring(thirdSlash);
        log.info("restOfUrl = {}", restOfUrl);

        InetAddress[] addresses = InetAddress.getAllByName(serviceName);
        return Arrays.stream(addresses).map(addr -> {
            Map<?, ?> response = webClient.get()
                    .uri("http://{host}:8080/" + restOfUrl, addr.getHostAddress())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response;
        }).collect(Collectors.toList());
    }

    @GetMapping("/endpoint/{serviceName}/**")
    public List<JsonNode> broadcastGetToServiceUsingEndpoint(@PathVariable String serviceName, HttpServletRequest request)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String restOfUrl = getUrlPathForBroadcast(request);
        List<String> addresses = gatherEndpoints(serviceName, request);

        return addresses.stream().map(addr -> {
            JsonNode response = webClient.get()
                    .uri("http://{host}:8080/" + restOfUrl, addr)
                    .headers(headers -> {
                        Enumeration<String> headerEnum = request.getHeaderNames();
                        while (headerEnum.hasMoreElements()) {
                            String name = headerEnum.nextElement();
                            headers.add(name, request.getHeader(name));
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return response;
        }).collect(Collectors.toList());
    }

    @PostMapping("/endpoint/{serviceName}/**")
    public List<JsonNode> broadcastPostToServiceUsingEndpoint(@PathVariable String serviceName, @RequestBody Optional<JsonNode> body, final HttpServletRequest request)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        String restOfUrl = getUrlPathForBroadcast(request);
        List<String> addresses = gatherEndpoints(serviceName, request);

        return addresses.stream().map(addr -> {
            JsonNode response = webClient.post()
                    .uri("http://{host}:8080/" + restOfUrl, addr)
                    .headers(headers -> {
                        Enumeration<String> headerEnum = request.getHeaderNames();
                        while (headerEnum.hasMoreElements()) {
                            String name = headerEnum.nextElement();
                            headers.add(name, request.getHeader(name));
                        }
                    })
                    .bodyValue(body.orElse(objectMapper.createObjectNode()))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return response;
        }).collect(Collectors.toList());
    }

    @DeleteMapping("/endpoint/{serviceName}/**")
    public List<JsonNode> broadcastDeleteToServiceUsingEndpoint(@PathVariable String serviceName, @RequestBody Optional<JsonNode> body, final HttpServletRequest request)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        String restOfUrl = getUrlPathForBroadcast(request);
        List<String> addresses = gatherEndpoints(serviceName, request);

        return addresses.stream().map(addr -> {
            JsonNode response = webClient.delete()
                    .uri("http://{host}:8080/" + restOfUrl, addr)
                    .headers(headers -> {
                        Enumeration<String> headerEnum = request.getHeaderNames();
                        while (headerEnum.hasMoreElements()) {
                            String name = headerEnum.nextElement();
                            headers.add(name, request.getHeader(name));
                        }
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return response;
        }).collect(Collectors.toList());
    }

    private String getUrlPathForBroadcast(final HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        log.info("proxy path = {}", path);

        int thirdSlash = ordinalIndexOf(path, "/", 4);
        String restOfUrl = thirdSlash == -1 ? "" : path.substring(thirdSlash);
        log.info("restOfUrl = {}", restOfUrl);

        return restOfUrl;
    }

    private List<String> gatherEndpoints(final String serviceName, final HttpServletRequest request) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        String token = Files.readString(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token"));

        File caCertFile = new File("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate caCert;
        try (InputStream is = new FileInputStream(caCertFile)) {
            caCert = (X509Certificate) certificateFactory.generateCertificate(is);
        }

        System.out.println("store=" + System.getProperty("javax.net.ssl.trustStore"));
        System.out.println("storeType=" + System.getProperty("javax.net.ssl.trustStoreType"));
        System.out.println("storePass=" + System.getProperty("javax.net.ssl.trustStorePassword"));

        // Create SSL Context with ServiceAccount Certificate
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("caCert", caCert);
        tmf.init(trustStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

        HttpClient httpClient = HttpClient.newBuilder().sslContext(sslContext).build();

        Endpoints endpoints = webClient.mutate().clientConnector(new JdkClientHttpConnector(httpClient)).build().get()
                .uri("https://kubernetes.default.svc.cluster.local/api/v1/namespaces/spring-demo/endpoints/{service}", serviceName)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Endpoints.class)
                .block();

        List<String> addresses = endpoints.subsets.stream()
                .map(s -> s.addresses)
                .flatMap(Collection::stream)
                .map(EndpointAddress::ip)
                .collect(Collectors.toList());

        return addresses;
    }

    @GetMapping("/client-endpoint/{serviceName}/**")
    public List<Map<?, ?>> broadcastClientEndpoint(@PathVariable String serviceName, HttpServletRequest request)
            throws IOException, ApiException {

        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();
        V1Endpoints endpoints = api.readNamespacedEndpoints(serviceName, "spring-demo").execute();
        List<String> addresses = endpoints.getSubsets().stream()
                .map(s -> s.getAddresses())
                .flatMap(Collection::stream)
                .map(V1EndpointAddress::getIp)
                .collect(Collectors.toList());

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        log.info("proxy path = {}", path);

        int thirdSlash = ordinalIndexOf(path, "/", 4);
        String restOfUrl = thirdSlash == -1 ? "" : path.substring(thirdSlash);
        log.info("restOfUrl = {}", restOfUrl);

        return addresses.stream().map(addr -> {
            Map<?, ?> response = webClient.get()
                    .uri("http://{host}:8080/" + restOfUrl, addr)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response;
        }).collect(Collectors.toList());
    }

    private int ordinalIndexOf(String str, String substr, int n) {
        int pos = str.indexOf(substr);
        while (--n > 0 && pos != -1)
            pos = str.indexOf(substr, pos + 1);
        return pos;
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
