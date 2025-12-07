package com.example.springdemo.data;

import com.example.springdemo.controller.QueueController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    public static final String CLEAR_ORDEREDQUEUE_ITEMS = """
            DELETE FROM orderedqueue
             WHERE wid IN ('123e4567-e89b-12d3-a456-426614174000',
                           '223e4567-e89b-12d3-a456-426614174000',
                           '323e4567-e89b-12d3-a456-426614174000');
            """;
    public static final String ADD_ORDEREDQUEUE_ITEMS = """
            INSERT INTO orderedqueue (wid, order_id, status, retry_cnt, msg, create_dt, update_dt)
            VALUES
                ('123e4567-e89b-12d3-a456-426614174000', 1, 'R', 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('223e4567-e89b-12d3-a456-426614174000', 1, 'R', 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                ('323e4567-e89b-12d3-a456-426614174000', 1, 'R', 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
            """;

    public static final String UPDATE_ORDERQUEUE_ITEM = """
            UPDATE orderedqueue
               SET status = :status,
                   order_id = :order_id
             WHERE wid = :wid
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
        namedJdbcTemplate.update(CLEAR_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
    }

    @AfterEach
    public void teardown() {
        namedJdbcTemplate.update(CLEAR_WORKQUEUE_ITEMS, Collections.emptyMap());
        namedJdbcTemplate.update(CLEAR_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
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
        verifyFirstItemInReadyStatus(actual, "workqueue");
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
        verifyFirstItemInReadyStatus(actual, "workqueue");
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


    // -----------------------------------------------------
    // Ordered queue methods
    // -----------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class, mode = EnumSource.Mode.EXCLUDE, names = {"OUTPUT_NOT_EXISTS", "SELECT_NOT_EXISTS", "OUTPUT_JOIN", "SELECT_JOIN"})
    public void testOrderedSelectMany_whenMultipleRowsWithSameOrder_returnsFirstAvailableItems(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(3, fetchType);
        assertThat(actuals).hasSize(1);
        verifyFirstItemInReadyStatus(actuals.get(0), "orderedqueue");
        verifyProcessOrder();
    }

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class, mode = EnumSource.Mode.EXCLUDE, names = {"OUTPUT_NOT_EXISTS", "SELECT_NOT_EXISTS", "OUTPUT_JOIN", "SELECT_JOIN"})
    public void testOrderedSelectMany_whenMultipleRowsWithFirstInError_returnsNoItems(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        namedJdbcTemplate.update(UPDATE_ORDERQUEUE_ITEM, Map.of(
                "status", "E",
                "order_id", 1,
                "wid", "123e4567-e89b-12d3-a456-426614174000"));
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(3, fetchType);
        assertThat(actuals).isEmpty();
        verifyProcessOrder();
    }

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class, mode = EnumSource.Mode.EXCLUDE, names = {"OUTPUT_NOT_EXISTS", "SELECT_NOT_EXISTS", "OUTPUT_JOIN", "SELECT_JOIN"})
    public void testOrderedSelectMany_whenMultipleRowsWithFirstInProgress_returnsNoItems(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        namedJdbcTemplate.update(UPDATE_ORDERQUEUE_ITEM, Map.of(
                "status", "I",
                "order_id", 1,
                "wid", "123e4567-e89b-12d3-a456-426614174000"));
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(3, fetchType);
        assertThat(actuals).isEmpty();
        verifyProcessOrder();
    }

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class, mode = EnumSource.Mode.EXCLUDE, names = {"OUTPUT_NOT_EXISTS", "SELECT_NOT_EXISTS", "OUTPUT_JOIN", "SELECT_JOIN"})
    public void testOrderedSelectMany_whenMultipleRowsWithErrorOnDifferentOrder_returnsFirstItem(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        // place second item in error for different order so first record will be returned
        namedJdbcTemplate.update(UPDATE_ORDERQUEUE_ITEM, Map.of(
                "status", "E",
                "order_id", 2,
                "wid", "223e4567-e89b-12d3-a456-426614174000"));
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(3, fetchType);
        assertThat(actuals).hasSize(1);
        verifyFirstItemInReadyStatus(actuals.get(0), "orderedqueue");
        verifyProcessOrder();
    }

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class, mode = EnumSource.Mode.EXCLUDE, names = {"OUTPUT_NOT_EXISTS", "SELECT_NOT_EXISTS", "OUTPUT_JOIN", "SELECT_JOIN"})
    public void testOrderedSelectMany_whenMultipleRowsWithInProgressOnDifferentOrder_returnsFirstItem(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        // place second item in error for different order so first record will be returned
        namedJdbcTemplate.update(UPDATE_ORDERQUEUE_ITEM, Map.of(
                "status", "I",
                "order_id", 2,
                "wid", "223e4567-e89b-12d3-a456-426614174000"));
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(3, fetchType);
        assertThat(actuals).hasSize(1);
        verifyFirstItemInReadyStatus(actuals.get(0), "orderedqueue");
        verifyProcessOrder();
    }

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class, mode = EnumSource.Mode.EXCLUDE, names = {"OUTPUT_NOT_EXISTS", "SELECT_NOT_EXISTS", "OUTPUT_JOIN", "SELECT_JOIN"})
    public void testOrderedSelectMany_whenMultipleRowsExistWithDifferentOrder_returnOneItemPerOrder(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        // change second item to different order
        namedJdbcTemplate.update(UPDATE_ORDERQUEUE_ITEM, Map.of(
                "status", "R",
                "order_id", 2,
                "wid", "223e4567-e89b-12d3-a456-426614174000"));
        // try to grab three items, but should only get two (one per order)
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(3, fetchType);
        assertThat(actuals).hasSize(2);
        assertThat(actuals).extracting("wid")
                .containsExactlyInAnyOrder(
                        "123e4567-e89b-12d3-a456-426614174000",
                        "223e4567-e89b-12d3-a456-426614174000");
        verifyProcessOrder();
    }

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class)
    public void testOrderedSelectOne_whenMultipleRowsWithSameOrder_returnsFirstAvailableItem(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(1, fetchType);
        assertThat(actuals).hasSize(1);
        verifyFirstItemInReadyStatus(actuals.get(0), "orderedqueue");
        verifyProcessOrder();
    }

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class)
    public void testOrderedSelectOne_whenMultipleRowsWithFirstInError_returnsNoItems(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        namedJdbcTemplate.update(UPDATE_ORDERQUEUE_ITEM, Map.of(
                "status", "E",
                "order_id", 1,
                "wid", "123e4567-e89b-12d3-a456-426614174000"));
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(1, fetchType);
        assertThat(actuals).isEmpty();
        verifyProcessOrder();
    }

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class)
    public void testOrderedSelectOne_whenMultipleRowsWithFirstInProgress_returnsNoItems(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        namedJdbcTemplate.update(UPDATE_ORDERQUEUE_ITEM, Map.of(
                "status", "I",
                "order_id", 1,
                "wid", "123e4567-e89b-12d3-a456-426614174000"));
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(1, fetchType);
        assertThat(actuals).isEmpty();
        verifyProcessOrder();
    }

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class)
    public void testOrderedSelectOne_whenMultipleRowsWithErrorOnDifferentOrder_returnsFirstItem(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        // place second item in error for different order so first record will be returned
        namedJdbcTemplate.update(UPDATE_ORDERQUEUE_ITEM, Map.of(
                "status", "E",
                "order_id", 2,
                "wid", "223e4567-e89b-12d3-a456-426614174000"));
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(1, fetchType);
        assertThat(actuals).hasSize(1);
        verifyFirstItemInReadyStatus(actuals.get(0), "orderedqueue");
        verifyProcessOrder();
    }

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class)
    public void testOrderedSelectOne_whenMultipleRowsWithInProgressOnDifferentOrder_returnsFirstItem(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        // place second item in error for different order so first record will be returned
        namedJdbcTemplate.update(UPDATE_ORDERQUEUE_ITEM, Map.of(
                "status", "I",
                "order_id", 2,
                "wid", "223e4567-e89b-12d3-a456-426614174000"));
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(1, fetchType);
        assertThat(actuals).hasSize(1);
        verifyFirstItemInReadyStatus(actuals.get(0), "orderedqueue");
        verifyProcessOrder();
    }

    @ParameterizedTest
    @EnumSource(value = QueueRepository.FETCH_TYPE.class)
    public void testOrderedSelectOne_whenMultipleRowsExistWithDifferentOrder_returnOneItemPerOrder(QueueRepository.FETCH_TYPE fetchType) {
        namedJdbcTemplate.update(ADD_ORDEREDQUEUE_ITEMS, Collections.emptyMap());
        // change second item to different order
        namedJdbcTemplate.update(UPDATE_ORDERQUEUE_ITEM, Map.of(
                "status", "R",
                "order_id", 2,
                "wid", "223e4567-e89b-12d3-a456-426614174000"));
        // try to grab three items, but should only get two (one per order)
        List<QueueController.OrderedWorkItem> actuals = queueRepository.orderFetchMany(1, fetchType);
        assertThat(actuals).hasSize(1);
        verifyFirstItemInReadyStatus(actuals.get(0), "orderedqueue");
        verifyProcessOrder();
    }

    // -----------------------------------------------------
    // Private helper methods
    // -----------------------------------------------------

    private void verifyProcessOrder() {
        List<Map<String, Object>> misorederedItems = namedJdbcTemplate.queryForList("""
                WITH OrderedItems AS (
                    SELECT
                        order_id,
                        id,
                        LAG(id) OVER (PARTITION BY order_id ORDER BY update_dt) AS previous_id
                    FROM orderedqueue
                    WHERE status != 'R'
                )
                SELECT
                    order_id,
                    id,
                    previous_id
                FROM OrderedItems
                WHERE previous_id IS NOT NULL AND id <= previous_id;
                """, Collections.emptyMap());
        assertThat(misorederedItems).isEmpty();
    }

    private void verifyFirstItemInReadyStatus(QueueController.OrderedWorkItem actual, String tableName) {
        assertNotNull(actual);
        assertThat(actual.getWid()).isEqualTo("123e4567-e89b-12d3-a456-426614174000");

        namedJdbcTemplate.query("SELECT * FROM #TBL# WHERE wid = '123e4567-e89b-12d3-a456-426614174000'"
                .replace("#TBL#", tableName), rs -> {
            assertThat(rs.getString("status")).isEqualTo("I");
        });
//        namedJdbcTemplate.query("""
//                SELECT * FROM #TBL# WHERE wid IN (
//                    '223e4567-e89b-12d3-a456-426614174000',
//                    '323e4567-e89b-12d3-a456-426614174000')""".replace("#TBL#", tableName), rs -> {
//            assertThat(rs.getString("status")).isIn("R", "E");
//        });
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