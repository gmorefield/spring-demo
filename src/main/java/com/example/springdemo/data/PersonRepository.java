package com.example.springdemo.data;

import com.example.springdemo.model.Person;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class PersonRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PersonRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @CachePut(cacheNames = "person", key = "#result.id")
    @CacheEvict(cacheNames = "persons", allEntries = true)
    public Person save(Person person) {
        int rowsUpdated = 0;
        long id = person.getId();

        if (person.getId() > 0) {
            rowsUpdated = jdbcTemplate.update(
                    "update Person set first_name = :firstName, last_name = :lastName Where id = :id",
                    new BeanPropertySqlParameterSource(person));
        }

        if (rowsUpdated == 0) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            rowsUpdated = jdbcTemplate.update(
                    "insert into Person (FIRST_NAME, LAST_NAME) values (:firstName, :lastName)",
                    new BeanPropertySqlParameterSource(person),
                    keyHolder,
                    new String[]{"ID"});

            Number key = keyHolder.getKey();
            id = key.longValue();
        }

        return findById(id);
    }

    @Cacheable("persons")
    public List<Person> findAll() {
        return jdbcTemplate.query("select * from Person", (resultSet, i) -> toPerson(resultSet));
    }

    @Cacheable(cacheNames = "person", key = "#id")
    public Person findById(long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "select * from Person where ID = :id",
                    new MapSqlParameterSource().addValue("id", id),
                    (resultSet, row) -> toPerson(resultSet));
        } catch (IncorrectResultSizeDataAccessException e) {
            if (e.getActualSize() == 0) {
                return null;
            }
            throw e;
        }
    }

    public static class RetryableException extends RuntimeException {
        public RetryableException() {
        }

        public RetryableException(String message) {
            super(message);
        }

        public RetryableException(String message, Throwable cause) {
            super(message, cause);
        }

        public RetryableException(Throwable cause) {
            super(cause);
        }

        public RetryableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

    private Random r = new SecureRandom();

    @ConditionalOnProperty(name = "spring.sql.init.platform", havingValue = "mssql")
    @Transactional
    @Retryable(retryFor = {RetryableException.class}, maxAttempts = 3, listeners = {"sampleRetryListener"})
    public List<Person> addMultiple(int count) {
        List<Person> persons = new ArrayList<>();
        int maxAttempts = 3;
        log.info("adding multiple");
        persons.add(save(new Person(0, "Test101", "Last101")));
        for (int i = 0; i < maxAttempts; i++) {
            int chance = r.nextInt(0, 10);
            System.out.println("chance = " + chance);
            String testSql = "insert into person (first_name,last_name) values ('testScript', 'lastScript');";
            if (chance >= 6) {
                testSql = """
                        begin tran;
                        insert into person (first_name,last_name) values ('testScript', 'lastScript');
                        CREATE TYPE dbo.IntIntSet AS TABLE(
                            Value0 Int NOT NULL,
                            Value1 Int NOT NULL
                        );
                        declare @myPK dbo.IntIntSet;
                        rollback""";
            }
            try (Connection conn = jdbcTemplate.getJdbcTemplate().getDataSource().getConnection()) {
                ScriptUtils.executeSqlScript(conn, new ByteArrayResource(testSql.getBytes()));
                persons.add(findById(persons.get(0).getId() + 1));
//                    persons.add(save(new Person( 0, "Test102", "Last102")));
                break;
            } catch (Exception e) {
//                if (i >= (maxAttempts - 1)) {
                throw new RetryableException("Oops", e);
//                }
            }

        }
        return persons;
    }

    private Person toPerson(ResultSet resultSet) throws SQLException {
        return new Person(resultSet.getLong("ID"), resultSet.getString("FIRST_NAME"), resultSet.getString("LAST_NAME"));
    }
}
