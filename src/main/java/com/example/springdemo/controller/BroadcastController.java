package com.example.springdemo.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.HandlerMapping;

import lombok.extern.slf4j.Slf4j;

@Profile("kubernetes")
@RestController
@RequestMapping("/broadcast")
@Slf4j
public class BroadcastController {

    private WebClient webClient;

    private BroadcastController(WebClient.Builder webClientBuilder) {
        webClient = webClientBuilder.build();
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

    private int ordinalIndexOf(String str, String substr, int n) {
        int pos = str.indexOf(substr);
        while (--n > 0 && pos != -1)
            pos = str.indexOf(substr, pos + 1);
        return pos;
    }

}
