package com.example.springdemo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Component
public class PersonService {
    private JdbcTemplate jdbcTemplate;

    public PersonService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Person save(Person person) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        int rowsUpdated = jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "insert into Person (first_Name, Last_Name) values (?, ?)", new String[] { "ID" });
                    ps.setString(1, person.getFirstName());
                    ps.setString(2, person.getLastName());
                    return ps;
                }, keyHolder);

        Number key = keyHolder.getKey();
        return get(key.longValue());
    }

    public List<Person> loadAll() {
        return jdbcTemplate.query("select * from Person", (resultSet, i) -> {
            return toPerson(resultSet);
        });
    }

    public Person get(long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "select * from Person where ID = ?",
                    new Object[] { id }, new int[] { java.sql.Types.INTEGER },
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
