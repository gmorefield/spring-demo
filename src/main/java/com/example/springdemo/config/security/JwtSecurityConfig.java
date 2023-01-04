package com.example.springdemo.config.security;

import static org.springframework.security.config.Customizer.withDefaults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;

@Profile("jwt")
@Configuration
public class JwtSecurityConfig extends WebSecurityConfiguration {
    Logger logger = LoggerFactory.getLogger(JwtSecurityConfig.class);

    // @Bean
    // public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    protected void configure(HttpSecurity http) throws Exception {
        logger.info("Configuring JwtSecurity");
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasAuthority("SCOPE_admin")
                        .anyRequest().hasAuthority("SCOPE_execute"))
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(withDefaults()));
        // http.build();
    }
}
