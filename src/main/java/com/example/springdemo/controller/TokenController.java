package com.example.springdemo.controller;

import com.example.springdemo.config.security.JwtProperties;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;

@Profile({ "jws", "jwt" })
@RestController
@ConditionalOnProperty(name="spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class TokenController {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public TokenController(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @PostMapping("/token/jws")
    public String secureToken(Authentication authentication) {
        Instant now = Instant.now();
        long expiry = 36000L;

        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .subject(authentication.getName())
                .claim("scope", scope)
                .build();

        return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @PostMapping("/token/jwt")
    public String plainToken(Authentication authentication) {
        Instant now = Instant.now();
        long expiry = 36000L;

        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("self")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(expiry)))
                // .subject(authentication.getName())
                // .claim("scope", scope)
                .claim(properties.getPrincipalClaimName(), authentication.getName())
                .claim(properties.getAuthoritiesClaimName(), scope)
                .build();

        return new PlainJWT(claims).serialize();
    }

}
