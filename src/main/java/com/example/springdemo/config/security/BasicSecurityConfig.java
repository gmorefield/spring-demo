package com.example.springdemo.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    private final String basicUser;
    private final String basicPassword;

	// @Autowired
	// private AuthenticationEntryPoint authEntryPoint;

    public BasicSecurityConfig(@Value("${basic.user}") String user, @Value("${basic.password}") String password) {
        super();
        basicUser = user;
        basicPassword = password;
    }

    protected void configure(HttpSecurity http) throws Exception {
        logger.info("BasicSecurity configured");

        http.requestMatchers()
                .antMatchers("/authorize")
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
                // .antMatchers("/favicon.ico", "/error").permitAll()
                .anyRequest().authenticated()
                .and().httpBasic().realmName("spring-demo") //.authenticationEntryPoint(authEntryPoint)
                .and()
                .csrf().disable()
                .headers().frameOptions().sameOrigin();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder authentication)
            throws Exception
    {
        authentication.inMemoryAuthentication()
                .withUser(basicUser)
                .password(new BCryptPasswordEncoder().encode(basicPassword))
                .authorities("ROLE_USER");
    }

}