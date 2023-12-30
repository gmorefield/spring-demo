package com.example.springdemo.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * Simple example allowing anonymous access to all calls
 */
@Profile("anonymous")
@Configuration
@EnableWebSecurity
@Import(SecurityAutoConfiguration.class) // needed because autoconfig excluded in application.yml
@Slf4j
public class AnonymousSecurityConfig extends WebSecurityConfigurerAdapter {
    protected void configure(HttpSecurity http) throws Exception {
        log.info("AnonymousSecurity configured");
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().authorizeRequests().anyRequest().permitAll()
                .and()
                .csrf().disable()
                .headers().frameOptions().sameOrigin();
    }
}
