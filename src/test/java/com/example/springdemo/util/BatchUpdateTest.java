package com.example.springdemo.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@DirtiesContext
@ActiveProfiles("test")
public class BatchUpdateTest {
    private NamedParameterJdbcTemplate namedTemplate;

    public BatchUpdateTest(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        namedTemplate = jdbcTemplate;
    }

    @Test
    public void testBatchUpdate() {

        String sql = "INSERT into PERSON (first_name) SELECT :firstName WHERE NOT EXISTS(SELECT 1 from PERSON where first_name=:firstName)";

        List<Map<String, String>> persons = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            Map<String, String> person = new HashMap<>();
            person.put("firstName", RandomStringUtils.random(10, true, false));
            persons.add(person);
        }
        persons.addAll(persons);
        SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(persons.toArray());
        int[] updates = namedTemplate.batchUpdate(sql, batch);

        int[] expected = new int[50];
        Arrays.fill(expected, 0, 25, 1);
        Arrays.fill(expected, 25, 50, 0);

        Assertions.assertThat(updates)
                .hasSize(50)
                .containsExactly(expected);
    }
}
