package com.example.springdemo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * Simple example allowing anonymous access to all calls
 */
@Profile("!basic")
@Configuration
@EnableWebSecurity
public class AnonymousSecurityConfig extends WebSecurityConfigurerAdapter {
    Logger logger = LoggerFactory.getLogger(AnonymousSecurityConfig.class);

    protected void configure(HttpSecurity http) throws Exception {
        logger.info("AnonymousSecurity configured");
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().authorizeRequests().anyRequest().permitAll()
                .and().csrf().disable();
    }
}
