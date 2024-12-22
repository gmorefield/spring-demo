package com.example.springdemo.service;

import com.example.springdemo.controller.QueueController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QueueService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public QueueService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true))
    public synchronized QueueController.OrderedWorkItem fetchNext() {
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
                          from workqueue w2 (READPAST)
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

    @Retryable(retryFor = {PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true))
    public synchronized QueueController.OrderedWorkItem orderedFetchNext() {
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
}
