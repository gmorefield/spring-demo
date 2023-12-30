package com.example.springdemo.data;

import com.example.springdemo.config.CacheConfig;
import com.example.springdemo.data.PersonRepositoryCachingIntegrationTest.TestConfig;
import com.example.springdemo.model.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = { TestConfig.class, CacheConfig.class })
@TestPropertySource(properties = "sample.cache.enabled=true")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class PersonRepositoryCachingIntegrationTest {
    private static final Person JOHN = new Person(1, "John", "Doe");
    private static final Person JANE = new Person(2, "Jane", "Doe");
    private static final List<Person> PEOPLE = Arrays.asList(JOHN, JANE);

    private PersonRepository mock;

    // public PersonRepositoryCachingIntegrationTest(PersonRepository
    // personRepository, CacheManager cacheManager) {
    // mock = AopTestUtils.getTargetObject(personRepository);

    // when(mock.findById(eq(JOHN.getId())))
    // .thenReturn(JOHN)
    // .thenThrow(new RuntimeException("Person should be cached!"));

    // when(mock.save(any(Person.class)))
    // .thenAnswer(invocation -> invocation.getArguments()[0]);

    // when(mock.findAll())
    // .thenReturn(PEOPLE);
    // }

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        if (mock == null) {
            mock = AopTestUtils.getTargetObject(personRepository);

            // reset(mock);

            when(mock.findById(eq(JOHN.getId())))
                    .thenReturn(JOHN)
                    .thenThrow(new RuntimeException("Person should be cached!"));

            when(mock.save(any(Person.class)))
                    .thenAnswer(invocation -> invocation.getArguments()[0]);

            when(mock.findAll())
                    .thenReturn(PEOPLE);
        }

        clearInvocations(mock);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void givenCachedPerson_whenFindById_thenRepositoryShouldNotBeHit() {
        assertEquals(JOHN, personRepository.findById(JOHN.getId()));
        verify(mock).findById(JOHN.getId());

        assertEquals(JOHN, personRepository.findById(JOHN.getId()));
        assertEquals(JOHN, personRepository.findById(JOHN.getId()));

        verifyNoMoreInteractions(mock);
    }

    @Test
    void givenCachedPerson_whenSave_thenPersonCacheShouldBeUpdated() {
        assertEquals(JOHN, personRepository.findById(JOHN.getId()));
        verify(mock).findById(JOHN.getId());

        Person updated = new Person(JOHN.getId(), "John Mod", "Doe Mod");
        personRepository.save(updated);
        verify(mock).save(updated);

        assertEquals(updated, personRepository.findById(JOHN.getId()));
        assertEquals(updated, personRepository.findById(JOHN.getId()));

        verifyNoMoreInteractions(mock);
    }

    @Test
    void givenCachedPersons_whenFindAll_thenPersonsCacheShouldNotBeHit() {
        assertEquals(PEOPLE, personRepository.findAll());
        verify(mock).findAll();

        assertEquals(PEOPLE, personRepository.findAll());
        assertEquals(PEOPLE, personRepository.findAll());

        verifyNoMoreInteractions(mock);
    }

    @Test
    void givenCachedPersons_whenSave_thenPersonsCacheShouldBeCleared() {
        // cache user, update, and re-cache
        assertEquals(PEOPLE, personRepository.findAll());
        personRepository.save(JANE);
        assertEquals(PEOPLE, personRepository.findAll());

        verify(mock).save(JANE);
        verify(mock, times(2)).findAll();

        assertEquals(PEOPLE, personRepository.findAll()); // from cache
        assertEquals(PEOPLE, personRepository.findAll()); // from cache

        verifyNoMoreInteractions(mock);
    }

    @TestConfiguration
    public static class TestConfig {

        @Bean
        PersonRepository personRepository() {
            return mock(PersonRepository.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("person", "persons");
        }
    }
}