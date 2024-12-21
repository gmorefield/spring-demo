package com.example.springdemo.service;

import com.example.springdemo.controller.QueueController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class QueueService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public QueueService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Retryable(retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class, TransientDataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000L, multiplier = 2, random = true))
    public synchronized QueueController.OrderedWorkItem fetchNext() {
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
//        }
//        catch (Exception e) {
//            log.warn("fetchNext caught exception", e);
//            throw e;
//        }
    }
}
