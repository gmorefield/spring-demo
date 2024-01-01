package com.example.springdemo.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Simple example allowing anonymous access to all calls
 */
@Profile("anonymous")
@Configuration
@EnableWebSecurity
@Import(SecurityAutoConfiguration.class) // needed because autoconfig excluded in application.yml
@Slf4j
public class AnonymousSecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("AnonymousSecurity configured");
        http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(o -> o.sameOrigin()));
        return http.build();
    }
}
