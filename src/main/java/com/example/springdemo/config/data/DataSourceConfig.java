package com.example.springdemo.config.data;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @Profile({ "builder", "test-ds" })
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    @Validated
    public DataSource builderDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:builder")
                .username("sa")
                .build();
    }

    @Bean
    @Profile({ "props", "test-ds" })
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    @Validated
    public DataSource propsDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @Profile({ "custom", "test-ds" })
    @ConfigurationProperties(prefix = "sample.datasource")
    @Validated
    public DataSource customDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Profile({ "hikari", "test-ds" })
    @ConfigurationProperties(prefix = "sample.hikari.datasource")
    @Validated
    public DataSource hikariDataSource() {
        return new HikariDataSource();
    }
}
