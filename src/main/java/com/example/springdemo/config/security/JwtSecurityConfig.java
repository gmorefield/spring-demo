package com.example.springdemo.config.security;

import java.security.interfaces.RSAPublicKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Profile("jwt")
@EnableWebSecurity
@Configuration
public class JwtSecurityConfig {
	private Logger logger = LoggerFactory.getLogger(JwtSecurityConfig.class);

	@Bean(name = "jwtSecurityFilterChain")
	public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
		logger.info("JWT security configured");
		http
				.authorizeHttpRequests((authorize) -> authorize
						.requestMatchers(new AntPathRequestMatcher("/token")).permitAll()
						.requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
						.requestMatchers(EndpointRequest.toAnyEndpoint()).hasAuthority("SCOPE_admin")
						.anyRequest().hasAuthority("SCOPE_execute"))
				.csrf().disable()
				.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
				.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling((exceptions) -> exceptions
						.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
						.accessDeniedHandler(new BearerTokenAccessDeniedHandler()));
		return http.build();
	}

	@Bean
	JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
		return NimbusJwtDecoder.withPublicKey(publicKey).build();
	}
}
