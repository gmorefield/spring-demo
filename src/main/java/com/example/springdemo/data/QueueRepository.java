package com.example.springdemo.data;

import com.example.springdemo.controller.QueueController;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class QueueRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public enum FETCH_TYPE_ORDERED {
        OUTPUT_NOT_EXISTS,
        OUTPUT_PARTITION,
        OUTPUT_CROSS_APPLY,
        OUTPUT_SUB_SELECT,
        SELECT_NOT_EXISTS,
        SELECT_PARTITION,
        SELECT_CROSS_APPLY,
        SELECT_SUB_SELECT
    }

    public QueueRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Integer> getStatusCounts() {
        String sql = """
                SELECT status, COUNT(*) AS count
                FROM workqueue with (nolock)
                GROUP BY status
                """;

        return jdbcTemplate.query(sql, rs -> {
            Map<String, Integer> resultMap = new HashMap<>();
            while (rs.next()) {
                resultMap.put(rs.getString("status"), rs.getInt("count"));
            }
            return resultMap;
        });
    }

    public int addMany(final int itemCount) {
        String sql = """
                declare @numcreated int = 0, @order int = 1
                while (@numcreated < #count#)
                begin
                	insert into workqueue (wid,status) values (newid(),'R');
                	select @numcreated = @numcreated + 1;
                end;
                select @numcreated;
                """;
        int count = jdbcTemplate.queryForObject(sql.replace("#count#", String.valueOf(itemCount)),
                Collections.emptyMap(),
                Integer.class);
        return count;
    }

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true), label = "selectNext")
    public QueueController.OrderedWorkItem retrieveNext(int count) {
        List<QueueController.OrderedWorkItem> stream = jdbcTemplate.query("{ CALL SELECT_NEXT(:count) }",
                Map.of("count", count), (row, index) -> {
                    QueueController.OrderedWorkItem item = new QueueController.OrderedWorkItem();
                    item.setWid(row.getString("wid"));
                    item.setId(row.getString("id"));
                    return item;
                });
        Optional<QueueController.OrderedWorkItem> result = stream.stream().findFirst();
        return result.orElse(new QueueController.OrderedWorkItem());
    }

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true), label = "selectNext")
    public QueueController.OrderedWorkItem selectNext() {
        return getOrderedWorkItem("{ CALL SELECT_NEXT(:msg) }");
    }

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true), label = "fetchNext")
    public QueueController.OrderedWorkItem fetchNext() {
        return getOrderedWorkItem("{ CALL OUTPUT_NEXT(:msg) }");
    }

    @NotNull
    private QueueController.OrderedWorkItem getOrderedWorkItem(String sql) {
        List<QueueController.OrderedWorkItem> stream = jdbcTemplate.query(sql, Map.of("msg", Thread.currentThread().getId()), (row, index) -> {
            QueueController.OrderedWorkItem item = new QueueController.OrderedWorkItem();
            item.setWid(row.getString("wid"));
            item.setId(row.getString("id"));
            return item;
        });
        Optional<QueueController.OrderedWorkItem> result = stream.stream().findFirst();
        return result.orElse(new QueueController.OrderedWorkItem());
    }

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true), label = "selectMany")
    public List<QueueController.OrderedWorkItem> selectMany(Integer count) {
        return getWorkItems(count, "{ CALL SELECT_MANY(:count, :msg) }");
    }

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true), label = "fetchMany")
    public List<QueueController.OrderedWorkItem> fetchMany(Integer count) {
        return getWorkItems(count, "{ CALL OUTPUT_MANY(:count, :msg) }");
    }

    @NotNull
    private List<QueueController.OrderedWorkItem> getWorkItems(Integer count, String sql) {
        List<QueueController.OrderedWorkItem> items = jdbcTemplate.query(sql, Map.of(
                        "msg", Thread.currentThread().getId(),
                        "count", count),
                (row, index) -> {
                    QueueController.OrderedWorkItem item = new QueueController.OrderedWorkItem();
                    item.setWid(row.getString("wid"));
                    item.setId(row.getString("id"));
                    return item;
                });

        log.debug("Returned items: {}", items.stream()
                .map(QueueController.OrderedWorkItem::getId)
                .collect(Collectors.toList()));
        return items;
    }

    public int resetErrors() {
        String sql = """
                update workqueue
                   set status = 'R'
                 where status = 'E'
                   and retry_cnt < 4
                   and dateadd(SECOND, 10*retry_cnt, update_dt) < getdate()
                """;
        return jdbcTemplate.update(sql, Collections.emptyMap());
    }

    public Map<String, Integer> getOrderedStatusCounts() {
        String sql = """
                SELECT status, COUNT(*) AS count
                FROM orderedqueue with (nolock)
                GROUP BY status
                """;

        return jdbcTemplate.query(sql, rs -> {
            Map<String, Integer> resultMap = new HashMap<>();
            while (rs.next()) {
                resultMap.put(rs.getString("status"), rs.getInt("count"));
            }
            return resultMap;
        });
    }

    // Add single ordered item with random order id between 1 and uniqueOrders, ignoring duplicates
    public int orderedMergeSingle(final UUID newId, final int orderId) {
        String sql = """
                merge into orderedqueue as oq
                using ( values( :wid, :order_id ) ) as src(wid, order_id)
                   on oq.wid = src.wid
                 when not matched by target then
                    insert (wid,order_id,status)
                    values (:wid,:order_id,'R');
                """;
        return jdbcTemplate.update(sql,
                Map.of("wid", newId.toString(), "order_id", String.valueOf(orderId))
        );
    }

    // Add single ordered item with random order id between 1 and uniqueOrders
    public int orderedAddSingle(final UUID newId, final int orderId) {
        String sql = """
                insert into orderedqueue (wid,order_id,status) values (:wid,:order_id,'R');
                """;
        try {
            return jdbcTemplate.update(sql,
                    Map.of("wid", newId.toString(), "order_id", String.valueOf(orderId))
            );
        } catch (Exception e) {
            return 0;
        }
    }

    // Add single ordered item with random order id between 1 and uniqueOrders
    public int orderedAddUniqueSingle(final UUID newId, final int orderId) {
        String sql = """
                insert into orderedqueue (wid,order_id,status)
                select :wid, :order_id, 'R'
                where not exists (
                    select 1 from orderedqueue where wid = :wid
                );
                """;
        try {
            return jdbcTemplate.update(sql,
                    Map.of("wid", newId.toString(), "order_id", String.valueOf(orderId))
            );
        } catch (Exception e) {
            return 0;
        }
    }

    public int orderedAddMany(final int itemCount, final int uniqueOrders) {
        String sql = """
                declare @numcreated int = 0, @order int = 1
                while (@numcreated < #count#)
                begin
                	select @order = FLOOR(RAND()*(#order#));
                	insert into orderedqueue (wid,order_id,status) values (newid(),@order,'R');
                	select @numcreated = @numcreated + 1, @order = @order + 1;
                    --if (@order > 10) set @order = 1;
                end;
                select @numcreated;
                """;
        int count = jdbcTemplate.queryForObject(sql.replace("#count#", String.valueOf(itemCount))
                        .replace("#order#", String.valueOf(uniqueOrders)),
                Collections.emptyMap(),
                Integer.class);

        return count;
    }

    public void orderedSetStatus(QueueController.OrderedWorkItem item, String status) {
        try {
            jdbcTemplate.update(
                    """
                            update orderedqueue with (ROWLOCK)
                               set status=:status
                                 , update_dt=:now
                                 , retry_cnt=%s
                             --where wid=:wid
                             where id=:id
                            """.formatted(status.equals("C") ? String.valueOf(fetchCounter.get()) : "retry_cnt+1"),
                    Map.of("wid", item.getWid(), "id", item.getId(),
                            "status", status, "now", LocalDateTime.now()));

//            String now = LocalDateTime.now().toString();
//            fetchOrder.add(item.getLongKey() + " END " + fetchCounter.get() + " " + now + " " + Thread.currentThread().getName());
        } catch (Exception e) {
            log.warn("setStatus failed {}", e.getMessage());
            throw e;
        }
    }

    public void setStatus(QueueController.OrderedWorkItem item, String status) {
        try {
            jdbcTemplate.update(
                    """
                            update workqueue
                              with (ROWLOCK) 
                               set status=:status
                                 , update_dt=getdate()
                                 , retry_cnt=%s
                             --where wid=:wid
                             where id=:id
                            """.formatted(status.equals("C") ? "0" : "retry_cnt+1"),
                    Map.of("wid", item.getWid(), "id", item.getId(), "status", status));
        } catch (Exception e) {
            log.warn("setStatus failed {}", e.getMessage());
            throw e;
        }
    }

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true), label = "orderedFetchNext")
    public QueueController.OrderedWorkItem orderedFetchNext() {
        String sql = """
                DECLARE @itemTable TABLE (
                    wid varchar(36),
                    id int
                );
                update orderedqueue
                  with (ROWLOCK)
                   set status='I',
                       update_dt = getdate(),
                       msg = :msg
                output inserted.wid, inserted.id into @itemTable
                 where id = (
                    select top 1 w2.id
                      from orderedqueue  w2
                     where (w2.status='R')
                       and not exists (
                                select order_id
                                  from orderedqueue w3
                                 where w3.order_id = w2.order_id
                                   and w3.status in ('I','E')
                           )
                     order by w2.id
                   )
                   and (status='R');
                select * from orderedqueue where id = (select id from @itemTable); --@item;
                """;
        List<QueueController.OrderedWorkItem> stream = jdbcTemplate.query(sql, Map.of("msg", Thread.currentThread().getId()), (row, index) -> {
            QueueController.OrderedWorkItem item = new QueueController.OrderedWorkItem();
            item.setWid(row.getString("wid"));
            item.setOrderId(row.getString("order_id"));
            item.setId(row.getString("id"));
            return item;
        });
        Optional<QueueController.OrderedWorkItem> result = stream.stream().findFirst();
        return result.orElse(new QueueController.OrderedWorkItem());
    }

    public final static AtomicInteger fetchCounter = new AtomicInteger(0);
//    public final static ConcurrentLinkedQueue<String> fetchOrder = new ConcurrentLinkedQueue<>();

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true), label = "orderFetchMany")
    public List<QueueController.OrderedWorkItem> orderFetchMany(Integer count, FETCH_TYPE_ORDERED fetchType) {
        String sql = switch (fetchType) {
            case OUTPUT_NOT_EXISTS -> "{ CALL OUTPUT_MANY_ORDERED_NOTEXISTS(:count, :msg) }";
            case OUTPUT_PARTITION -> "{ CALL OUTPUT_MANY_ORDERED_PART(:count, :msg) }";
            case OUTPUT_CROSS_APPLY -> "{ CALL OUTPUT_MANY_ORDERED_CROSS(:count, :msg) }";
            case OUTPUT_SUB_SELECT -> "{ CALL OUTPUT_MANY_ORDERED_SUB(:count, :msg) }";
            case SELECT_NOT_EXISTS -> "{ CALL SELECT_MANY_ORDERED_NOTEXISTS(:count, :msg) }";
            case SELECT_PARTITION -> "{ CALL SELECT_MANY_ORDERED_PART(:count, :msg) }";
            case SELECT_CROSS_APPLY -> "{ CALL SELECT_MANY_ORDERED_CROSS(:count, :msg) }";
            case SELECT_SUB_SELECT -> "{ CALL SELECT_MANY_ORDERED_SUB(:count, :msg) }";
        };

        int currentFetch = fetchCounter.incrementAndGet();
        List<QueueController.OrderedWorkItem> items = jdbcTemplate.query(sql, Map.of(
                "msg", System.currentTimeMillis() + "-" + fetchType.name().substring(0, 4) + "-" + currentFetch,
                "count", count), (row, index) -> {
            QueueController.OrderedWorkItem item = new QueueController.OrderedWorkItem();
            item.setWid(row.getString("wid"));
            item.setOrderId(row.getString("order_id"));
            item.setId(row.getString("id"));
            return item;
        });

//        if (items.stream().map(QueueController.OrderedWorkItem::getOrderId).distinct().count() < items.size()) {
//            log.warn("***orderFetchMany {} - items contain duplicate order IDs: {}", currentFetch, items.stream()
//                    .map(QueueController.OrderedWorkItem::getShortKey)
//                    .collect(Collectors.toList()));
//        }
//
//        String now = LocalDateTime.now().toString();
//        items.stream().map(i->i.getLongKey() + " BEG " + (currentFetch) + " " + now + " " + Thread.currentThread().getName()).forEach(fetchOrder::add);

        log.debug("Returned orders: {}", items.stream()
                .map(QueueController.OrderedWorkItem::getOrderId)
                .collect(Collectors.toList()));
        return items;
    }

    public List<Map<String, Object>> orderVerify() {
        String sql = """
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
                """;
        return jdbcTemplate.queryForList(sql, Collections.emptyMap());
    }

    public int orderedResetErrors() {
        String sql = """
                update orderedqueue
                   set status = 'R'
                 where status = 'E'
                   and retry_cnt < 4
                   and dateadd(SECOND, 10*retry_cnt, update_dt) < getdate()
                """;
        return jdbcTemplate.update(sql, Collections.emptyMap());
    }
}
