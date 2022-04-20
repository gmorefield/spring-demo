package com.example.springdemo.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.springdemo.model.Person;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@JdbcTest
@ActiveProfiles("test")
@Sql({"/test-person-repo-data.sql"})
public class PersonRepositoryIntegrationTest {
    private PersonRepository personRepository;

    public PersonRepositoryIntegrationTest(@Autowired JdbcTemplate jdbcTemplate) {
        personRepository = new PersonRepository(jdbcTemplate);
    }

    @Test
    public void testFindById_withKnownPerson_returnsPerson() {
        Person actual = personRepository.findById(1);
        assertNotNull(actual);
    }
}