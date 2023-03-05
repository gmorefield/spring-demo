package com.example.springdemo.config.security;

import static java.util.Collections.singletonMap;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

@Profile({ "jws", "jwt" })
@EnableWebSecurity
@Configuration
public class JwtSecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(JwtSecurityConfig.class);
    private JwtProperties jwtProperties;

    public JwtSecurityConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

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
    JwtDecoder jwtDecoder() {
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
                Map<String, Object> claims = jwtClaimSetConverter().convert(jwt.getJWTClaimsSet().getClaims());
                return Jwt.withTokenValue(token)
                        .headers((h) -> h.putAll(headers))
                        .claims((c) -> c.putAll(claims))
                        .build();
            } catch (ParseException e) {
                throw new BadJwtException("Failed to decode token", e);
            }
        };
    }

    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        if (StringUtils.hasText(jwtProperties.getAuthoritiesClaimName())) {
            converter.setAuthoritiesClaimName(jwtProperties.getAuthoritiesClaimName());
        }
        if (StringUtils.hasText(jwtProperties.getAuthorityPrefix())) {
            converter.setAuthorityPrefix(jwtProperties.getAuthorityPrefix());
        }
        return converter;
    }

    @Bean
    public JwtAuthenticationConverter customJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        if (StringUtils.hasText(jwtProperties.getPrincipalClaimName())) {
            converter.setPrincipalClaimName(jwtProperties.getPrincipalClaimName());
        }

        return converter;
    }

    @Bean
    public MappedJwtClaimSetConverter jwtClaimSetConverter() {
        return MappedJwtClaimSetConverter.withDefaults(singletonMap("memberOf", memberOfConverter()));
    }

    @Bean
    public Converter<Object, ?> memberOfConverter() {
        return new Converter<Object,String>() {

            @Override
            @Nullable
            public String convert(Object source) {
                return Arrays.stream(source.toString().split("[\\s,]"))
                .filter(StringUtils::hasText)
                .map(group -> group.substring(2).toLowerCase())
                .collect(Collectors.joining(" "));
            };
        };
    }
}
