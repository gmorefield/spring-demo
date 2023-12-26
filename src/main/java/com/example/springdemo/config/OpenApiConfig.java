package com.example.springdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;

@Configuration
@OpenAPIDefinition(info = @io.swagger.v3.oas.annotations.info.Info(title = "Spring Demo", description = "Kitchen sink of various Spring components"))
public class OpenApiConfig {
        // @Bean
        // public OpenAPI customOpenAPI() {
        // return new OpenAPI().addServersItem(new Server().description("Default"))
        // .addServersItem(new
        // Server().url("http://localhost:8080").description("Secondary"))
        // .components(new Components().addSecuritySchemes("basicScheme",
        // new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
        // .info(new Info().title("SpringDemo API").version("V1")
        // .license(new License().name("Apache 2.0").url("http://springdoc.org")));
        // }

        @Bean
        public CorsFilter corsFilter() {
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                // Allow anyone and anything access. Probably ok for Swagger spec
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowCredentials(false);
                config.addAllowedOrigin("*");
                config.addAllowedHeader("*");
                config.addAllowedMethod(HttpMethod.GET);

                source.registerCorsConfiguration("/v3/api-docs", config);
                source.registerCorsConfiguration("/swagger-ui/*", config);
                return new CorsFilter(source);
        }
}
