package com.example.springdemo;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest()
@ActiveProfiles({ "h2", "test" })
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
