package com.example.springdemo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * Simple example using Basic Authentication allowing:
 * anonymous access to Health and Info actuator, favicon, and error response
 * authenticated access to all other calls
 */
@Profile("basic")
@Configuration
@EnableWebSecurity
@Import(SecurityAutoConfiguration.class) // needed because autoconfig excluded in application.yml
public class BasicSecurityConfig extends WebSecurityConfigurerAdapter {
    Logger logger = LoggerFactory.getLogger(BasicSecurityConfig.class);

    protected void configure(HttpSecurity http) throws Exception {
        logger.info("BasicSecurity configured");

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
                .antMatchers("/favicon.ico", "/error").permitAll()
                .anyRequest().authenticated()
                .and().httpBasic()
                .and()
                .csrf().disable()
                .headers().frameOptions().sameOrigin();
    }
}
