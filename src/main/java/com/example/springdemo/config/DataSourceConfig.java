package com.example.springdemo.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataSourceConfig {

    // @Bean
    // @Profile("test-ds")
    // public DataSource dataSource(DataSourceProperties dataSourceProperties) {
    // return new EmbeddedDatabaseBuilder().build();
    // }

    @Bean
    @Profile({ "builder", "test-ds" })
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
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
    public DataSource propsDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @Profile({ "custom", "test-ds" })
    @ConfigurationProperties(prefix = "sample.datasource")
    public DataSource customDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Profile({ "hikari", "test-ds" })
    @ConfigurationProperties(prefix = "sample.hikari.datasource")
    public DataSource hikariDataSource() {
        return new HikariDataSource();
    }
}
