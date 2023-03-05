package com.example.springdemo.config.security;

import static org.springframework.security.config.Customizer.withDefaults;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Profile({ "jws", "jwt" })
@Configuration
public class TokenConfig {
	private Logger logger = LoggerFactory.getLogger(TokenConfig.class);
	private KeyPair keyPair;

	public TokenConfig() throws NoSuchAlgorithmException, NoSuchProviderException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

		// Initialize KeyPairGenerator.
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		keyGen.initialize(2048, random);

		// Generate Key Pairs, a private key and a public key.
		keyPair = keyGen.generateKeyPair();
	}

	@Bean
	public RSAPublicKey jwtPublicKey() {
		return (RSAPublicKey) keyPair.getPublic();
	}

	@Bean
	public RSAPrivateKey jwtPrivateKey() {
		return (RSAPrivateKey) keyPair.getPrivate();
	}

	@Bean(name = "tokenSecurityFilterChain")
	@Order(100)
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		logger.info("Token generation security configured");
		http
				.antMatcher("/token/*")
				.authorizeHttpRequests((authorize) -> authorize
						.anyRequest().authenticated())
				.csrf().disable()
				.httpBasic(withDefaults())
				.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		return http.build();
	}

	@Bean
	UserDetailsService users(@Value("${basic.user}") String basicUser,
			@Value("${basic.admin}") String basicAdmin,
			@Value("${basic.password}") String basicPassword,
			@Value("#{'${basic.user.groups}'.split(',')}") String [] userGroups,
			@Value("#{'${basic.admin.groups}'.split(',')}") String [] adminGroups) {

		return new InMemoryUserDetailsManager(
				User.withUsername(basicUser)
						.password("{noop}" + basicPassword)
						.authorities(userGroups)
						.build(),
				User.withUsername(basicAdmin)
						.password("{noop}" + basicPassword)
						.authorities(adminGroups)
						.build());
	}

	@Bean
	JwtEncoder jwtEncoder() {
		JWK jwk = new RSAKey.Builder(jwtPublicKey())
				.privateKey(jwtPrivateKey())
				.build();
		JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
		return new NimbusJwtEncoder(jwks);
	}
}
