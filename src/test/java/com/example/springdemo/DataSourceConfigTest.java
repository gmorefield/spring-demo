package com.example.springdemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;

import com.example.springdemo.config.data.DataSourceConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DataSourceConfig.class})
@EnableConfigurationProperties()
@ActiveProfiles({ "test", "test-ds" })
public class DataSourceConfigTest {

    @Autowired
    ApplicationContext context;

    @Test
    public void testBuilderDataSource(@Autowired @Qualifier("builderDataSource") DataSource builderDataSource) {
        assertNotNull(builderDataSource, "builderDataSource should not be null");
        assertInstanceOf(HikariDataSource.class, builderDataSource);
        HikariDataSource hds = (HikariDataSource) builderDataSource;
        assertTrue(hds.getJdbcUrl().contains("builder"), hds.getJdbcUrl());
        assertEquals(30, hds.getMaximumPoolSize());
    }

    @Test
    public void testPropsDataSource(@Autowired @Qualifier("propsDataSource") DataSource propsDataSource) {
        assertNotNull(propsDataSource, "propsDataSource should not be null");
        assertInstanceOf(HikariDataSource.class, propsDataSource);
        HikariDataSource hds = (HikariDataSource) propsDataSource;
        assertTrue(hds.getJdbcUrl().contains("props"), hds.getJdbcUrl());
        assertEquals(30, hds.getMaximumPoolSize());
    }

    @Test
    public void testCustomDataSource(@Autowired @Qualifier("customDataSource") DataSource customDataSource) {
        assertNotNull(customDataSource, "customDataSource should not be null");
        assertInstanceOf(HikariDataSource.class, customDataSource);
        HikariDataSource hds = (HikariDataSource) customDataSource;
        assertTrue(hds.getJdbcUrl().contains("custom"), hds.getJdbcUrl());
        assertEquals(30, hds.getMaximumPoolSize());
    }

    @Test
    public void testHikariDataSource(@Autowired @Qualifier("hikariDataSource") DataSource hikariDataSource) {
        assertNotNull(hikariDataSource, "hikariDataSource should not be null");
        assertInstanceOf(HikariDataSource.class, hikariDataSource);
        HikariDataSource hds = (HikariDataSource) hikariDataSource;
        assertTrue(hds.getJdbcUrl().contains("hikari"), hds.getJdbcUrl());
        assertEquals(30, hds.getMaximumPoolSize());
    }
}
