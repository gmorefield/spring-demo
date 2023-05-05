package com.example.springdemo.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@ConfigurationProperties("sample.jwt")
@Validated
public class JwtProperties {
    // name of principal claim
    private String principalClaimName = JwtClaimNames.SUB;

    // name of authorities claim (defaults to JwtGrantedAuthoritiesConverter.WELL_KNOWN_AUTHORITIES_CLAIM_NAMES - scope,scp)
    private String authoritiesClaimName = "scope";

    // prefix for Jwt Authorities (defaults to JwtGrantedAuthoritiesConverter.DEFAULT_AUTHORITY_PREFIX - SCOPE_)
    private String authorityPrefix = "SCOPE_";
}
