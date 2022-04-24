package com.example.springdemo.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.example.springdemo.model.Person;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Component
public class PersonRepository {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public PersonRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Person save(Person person) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        int rowsUpdated = jdbcTemplate.update(
            "insert into Person (first_Name, Last_Name) values (:firstName, :lastName)",
            new BeanPropertySqlParameterSource(person),
            keyHolder,
            new String[] { "ID" });

        Number key = keyHolder.getKey();
        return findById(key.longValue());
    }

    public List<Person> findAll() {
        return jdbcTemplate.query("select * from Person", (resultSet, i) -> {
            return toPerson(resultSet);
        });
    }

    public Person findById(long id) {
        try {
            return jdbcTemplate.queryForObject(
                "select * from Person where ID = :id",
                new MapSqlParameterSource().addValue("id", id),
                (resultSet, row) -> {
                    return toPerson(resultSet);
                });
        } catch (IncorrectResultSizeDataAccessException e) {
            if (e.getActualSize() == 0) {
                return null;
            }
            throw e;
        }
    }

    private Person toPerson(ResultSet resultSet) throws SQLException {
        return new Person(resultSet.getLong("ID"), resultSet.getString("FIRST_NAME"), resultSet.getString("LAST_NAME"));
    }
}
