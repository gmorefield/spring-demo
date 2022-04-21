package com.example.springdemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {"spring.application.admin.enabled=false"})
@ActiveProfiles({ "test", "h2" })
public class DataSourceAutoConfigTest {

    @Test
    public void testDataSource(@Autowired DataSource dataSource) {
        assertNotNull(dataSource, "builderDataSource should not be null");
        assertInstanceOf(HikariDataSource.class, dataSource);
        HikariDataSource hds = (HikariDataSource) dataSource;
        assertTrue(hds.getJdbcUrl().contains("test"), hds.getJdbcUrl());
        assertEquals(30, hds.getMaximumPoolSize());
    }
}
