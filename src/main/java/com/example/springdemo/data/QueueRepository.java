package com.example.springdemo.data;

import com.example.springdemo.controller.QueueController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class QueueRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public QueueRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true))
    public QueueController.OrderedWorkItem fetchNext() {
        String sql = """
                DECLARE @itemTable TABLE (
                    wid varchar(36),
                    id int
                );
                update workqueue
                  with (ROWLOCK)
                   set status='I',
                       update_dt = getdate(),
                       msg = :msg
                output inserted.wid, inserted.id into @itemTable
                 where id = (
                        select top 1 w2.id
                          from workqueue w2
                          with (UPDLOCK, READPAST)
                         where (w2.status='R')
                         order by w2.id
                       )
                   and (status='R');
                select * from workqueue where id = (select id from @itemTable);
                """;
        List<QueueController.OrderedWorkItem> stream = jdbcTemplate.query(sql, Map.of("msg", Thread.currentThread().getId()), (row, index) -> {
            QueueController.OrderedWorkItem item = new QueueController.OrderedWorkItem();
            item.setWid(row.getString("wid"));
            item.setId(row.getString("id"));
            return item;
        });
        Optional<QueueController.OrderedWorkItem> result = stream.stream().findFirst();
        return result.orElse(new QueueController.OrderedWorkItem());
    }

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true))
    public List<QueueController.OrderedWorkItem> fetchMany(Integer count) {
        String sql = """
                DECLARE @itemTable TABLE (
                    wid varchar(36),
                    id int
                );
                update workqueue
                  with (ROWLOCK)
                   set status='I',
                       update_dt = getdate(),
                       msg = :msg
                output inserted.wid, inserted.id into @itemTable
                 where id in (
                        select top #FETCH_COUNT# w2.id
                          from workqueue w2
                         where w2.status = 'R'
                         order by w2.id
                       )
                   and (status='R');
                select * from workqueue where id in (select id from @itemTable);
                """.replace("#FETCH_COUNT#", String.valueOf(count));
        List<QueueController.OrderedWorkItem> items = jdbcTemplate.query(sql, Map.of("msg", Thread.currentThread().getId()), (row, index) -> {
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
                            update orderedqueue
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

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true))
    public QueueController.OrderedWorkItem orderedFetchNext() {
        String sql = """
                --DECLARE @item varchar(36);
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
                --where wid = (
                --    select top 1 w2.wid
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
                     --order by w2.create_dt
                     order by w2.id
                   )
                   and (status='R');
                --select @item = wid from @itemTable;
                --select * from orderedqueue where wid = (select wid from @itemTable); --@item;
                select * from orderedqueue where id = (select id from @itemTable); --@item;
                """;
//        try (Stream<OrderedWorkItem> stream = jdbcTemplate.queryForStream(sql, Map.of("msg", Thread.currentThread().getId()), (row, index) -> {
//        try {
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

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true))
    public List<QueueController.OrderedWorkItem> orderedFetchMany(Integer count) {
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
                 where id in (
                        select top #FETCH_COUNT# s.id
                          from (
                            select row_number() over (partition by order_id order by id) as RowNum, *
                              from orderedqueue o
                             where o.status != 'C'
                          ) as s
                         where s.RowNum = 1
                           and s.status = 'R'
                         order by id
                       )
                   and (status='R');
                select * from orderedqueue where id in (select id from @itemTable);
                """.replace("#FETCH_COUNT#", String.valueOf(count));
        List<QueueController.OrderedWorkItem> items = jdbcTemplate.query(sql, Map.of("msg", Thread.currentThread().getId()), (row, index) -> {
            QueueController.OrderedWorkItem item = new QueueController.OrderedWorkItem();
            item.setWid(row.getString("wid"));
            item.setOrderId(row.getString("order_id"));
            item.setId(row.getString("id"));
            return item;
        });

        log.debug("Returned orders: {}", items.stream()
                .map(QueueController.OrderedWorkItem::getOrderId)
                .collect(Collectors.toList()));
        return items;
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
