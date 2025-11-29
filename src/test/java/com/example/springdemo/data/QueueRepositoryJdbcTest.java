package com.example.springdemo.data;

import com.example.springdemo.controller.QueueController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@JdbcTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
// TODO: figure out why @Sql annotations are not working as expected with H2 (can randomly fail)
//@Sql(statements = {QueueRepositoryJdbcTest.CLEAR_WORKQUEUE_ITEMS},
//        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
//        config = @SqlConfig(transactionMode = ISOLATED))
//@Sql(statements = {QueueRepositoryJdbcTest.ADD_WORKQUEUE_ITEMS}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
//@Sql(statements = {QueueRepositoryJdbcTest.CLEAR_WORKQUEUE_ITEMS},
//        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class QueueRepositoryJdbcTest {
    public static final String CLEAR_WORKQUEUE_ITEMS = """
            DELETE FROM workqueue
             WHERE wid IN ('123e4567-e89b-12d3-a456-426614174000',
                           '223e4567-e89b-12d3-a456-426614174000',
                           '323e4567-e89b-12d3-a456-426614174000');
            """;
    public static final String ADD_WORKQUEUE_ITEMS = """
            INSERT INTO workqueue (wid, status, retry_cnt, msg, create_dt, update_dt)
            VALUES
                ('123e4567-e89b-12d3-a456-426614174000', 'R', 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('223e4567-e89b-12d3-a456-426614174000', 'R', 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('323e4567-e89b-12d3-a456-426614174000', 'R', 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
            """;
    private final QueueRepository queueRepository;

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public QueueRepositoryJdbcTest(@Autowired NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        queueRepository = new QueueRepository(namedJdbcTemplate);
    }

    @BeforeEach
    public void setup() {
        namedJdbcTemplate.update(CLEAR_WORKQUEUE_ITEMS, Collections.emptyMap());
    }

    @AfterEach
    public void teardown() {
        namedJdbcTemplate.update(CLEAR_WORKQUEUE_ITEMS, Collections.emptyMap());
    }

    @Test
//    @Sql(statements = {CLEAR_WORKQUEUE_ITEMS}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
//    @SqlMergeMode(SqlMergeMode.MergeMode.OVERRIDE)
    public void testSelectNext_whenEmpty_returnsNull() {
        QueueController.OrderedWorkItem actual = queueRepository.selectNext();
        assertNotNull(actual);
        assertThat(actual.getWid()).isNull();
        assertThat(actual.getId()).isNull();
    }

    @Test
//    @Sql(statements = {ADD_WORKQUEUE_ITEMS}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void testOutputNext_whenMultipleRows_returnsFirstItem() {
        namedJdbcTemplate.update(ADD_WORKQUEUE_ITEMS, Collections.emptyMap());
        QueueController.OrderedWorkItem actual = queueRepository.fetchNext();
        verifyFirstItemInReadyStatus(actual);
    }

    @Test
//    @Sql(statements = {CLEAR_WORKQUEUE_ITEMS}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
//    @SqlMergeMode(SqlMergeMode.MergeMode.OVERRIDE)
    public void testOutputNext_whenEmpty_returnsNull() {
        QueueController.OrderedWorkItem actual = queueRepository.fetchNext();
        assertNotNull(actual);
        assertThat(actual.getWid()).isNull();
        assertThat(actual.getId()).isNull();
    }

    @Test
//    @Sql(statements = {ADD_WORKQUEUE_ITEMS}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void testSelectNext_whenMultipleRows_returnsFirstItem() {
        namedJdbcTemplate.update(ADD_WORKQUEUE_ITEMS, Collections.emptyMap());
        QueueController.OrderedWorkItem actual = queueRepository.selectNext();
        verifyFirstItemInReadyStatus(actual);
    }

    @Test
//    @Sql(statements = {CLEAR_WORKQUEUE_ITEMS}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
//    @SqlMergeMode(SqlMergeMode.MergeMode.OVERRIDE)
    public void testSelectMany_whenEmpty_returnsNull() {
        List<QueueController.OrderedWorkItem> actual = queueRepository.selectMany(2);
        assertThat(actual).isEmpty();
    }

    @Test
//    @Sql(statements = {ADD_WORKQUEUE_ITEMS}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void testSelectMany_whenMultipleRows_returnsFirstAvailableItems() {
        namedJdbcTemplate.update(ADD_WORKQUEUE_ITEMS, Collections.emptyMap());
        List<QueueController.OrderedWorkItem> actual = queueRepository.selectMany(2);
        verifyTwoInReadyStatus(actual);
    }

    @Test
//    @Sql(statements = {CLEAR_WORKQUEUE_ITEMS}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
//    @SqlMergeMode(SqlMergeMode.MergeMode.OVERRIDE)
    public void testOutputMany_whenEmpty_returnsNull() {
        List<QueueController.OrderedWorkItem> actual = queueRepository.fetchMany(2);
        assertThat(actual).isEmpty();
    }

    @Test
//    @Sql(statements = {ADD_WORKQUEUE_ITEMS}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void testOutputMany_whenMultipleRows_returnsFirstAvailableItems() {
        namedJdbcTemplate.update(ADD_WORKQUEUE_ITEMS, Collections.emptyMap());
        List<QueueController.OrderedWorkItem> actual = queueRepository.fetchMany(2);
        verifyTwoInReadyStatus(actual);
    }

    private void verifyFirstItemInReadyStatus(QueueController.OrderedWorkItem actual) {
        assertNotNull(actual);
        assertThat(actual.getWid()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");

        namedJdbcTemplate.query("SELECT * FROM workqueue WHERE wid = '123e4567-e89b-12d3-a456-426614174000'", rs -> {
            assertThat(rs.getString("status")).isEqualTo("I");
        });
        namedJdbcTemplate.query("""
                SELECT * FROM workqueue WHERE wid IN (
                    '223e4567-e89b-12d3-a456-426614174000',
                    '323e4567-e89b-12d3-a456-426614174000')""", rs -> {
            assertThat(rs.getString("status")).isEqualTo("R");
        });
    }

    private void verifyTwoInReadyStatus(List<QueueController.OrderedWorkItem> actual) {
        assertThat(actual).hasSize(2);
        assertThat(actual).extracting("wid")
                .containsExactly("123e4567-e89b-12d3-a456-426614174000",
                        "223e4567-e89b-12d3-a456-426614174000");

        namedJdbcTemplate.query("""
                SELECT * FROM workqueue WHERE wid IN (
                    '123e4567-e89b-12d3-a456-426614174000',
                    '223e4567-e89b-12d3-a456-426614174000')""", rs -> {
            assertThat(rs.getString("status")).isEqualTo("I");
        });
        namedJdbcTemplate.query("SELECT * FROM workqueue WHERE wid = '323e4567-e89b-12d3-a456-426614174000'", rs -> {
            assertThat(rs.getString("status")).isEqualTo("R");
        });
    }
}