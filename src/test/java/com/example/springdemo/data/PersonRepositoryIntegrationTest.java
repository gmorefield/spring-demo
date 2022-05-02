package com.example.springdemo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.springdemo.model.Person;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@JdbcTest
@ActiveProfiles("test")
@Sql({ "/test-person-repo-data.sql" })
public class PersonRepositoryIntegrationTest {
    private PersonRepository personRepository;

    public PersonRepositoryIntegrationTest(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        personRepository = new PersonRepository(jdbcTemplate);
    }

    @Test
    public void testFindById_withKnownPerson_returnsPerson() {
        Person actual = personRepository.findById(1);
        assertNotNull(actual);
        assertEquals(1, actual.getId());
        assertEquals("Luke", actual.getFirstName());
        assertEquals("Skywalker", actual.getLastName());
    }

    @Test
    public void testSave_withKnownPerson_returnsUpdatedPerson() {
        Person expected = personRepository.findById(1);
        assertNotNull(expected);
        expected.setFirstName("Lucas");
        expected.setLastName("Vader");

        Person actual = personRepository.save(expected);
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
    }

    @Test
    public void testSave_withUnknownPerson_returnsNewPerson() {
        Person expected = new Person(0, "Hans", "Solo");

        Person actual = personRepository.save(expected);
        assertEquals(2, actual.getId());
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
    }
}