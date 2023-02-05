package com.example.springdemo.config.security;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

@Profile({ "jws", "jwt" })
@EnableWebSecurity
@Configuration
public class JwtSecurityConfig {
	private Logger logger = LoggerFactory.getLogger(JwtSecurityConfig.class);

	private Converter<Map<String, Object>, Map<String, Object>> claimSetConverter = MappedJwtClaimSetConverter
			.withDefaults(Collections.emptyMap());

	@Bean(name = "jwtSecurityFilterChain")
	public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
		logger.info("JWT security configured");
		http
				.authorizeHttpRequests((authorize) -> authorize
						.requestMatchers(
								new AntPathRequestMatcher("/token/*"),
								new AntPathRequestMatcher("/ws/**"))
						.permitAll()
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

	@Profile("jws")
	@Bean
	JwtDecoder jwsDecoder(RSAPublicKey publicKey) {
		return NimbusJwtDecoder.withPublicKey(publicKey).build();
	}

	@Profile("jwt")
	@Bean
	JwtDecoder jwpDecoder() {
		return (token) -> {
			try {
				JWT jwt = JWTParser.parse(token);
				if (!"self".equals(jwt.getJWTClaimsSet().getIssuer())) {
					throw new BadJwtException("Invalid issuer");
				}

				if (jwt.getJWTClaimsSet().getExpirationTime().before(Date.from(Instant.now()))) {
					throw new BadJwtException("Token expired");
				}

				Map<String, Object> headers = new LinkedHashMap<>(jwt.getHeader().toJSONObject());
				Map<String, Object> claims = this.claimSetConverter.convert(jwt.getJWTClaimsSet().getClaims());
				return Jwt.withTokenValue(token)
						.headers((h) -> h.putAll(headers))
						.claims((c) -> c.putAll(claims))
						.build();
			} catch (ParseException e) {
				throw new BadJwtException("Failed to decode token", e);
			}
		};
	}
}
