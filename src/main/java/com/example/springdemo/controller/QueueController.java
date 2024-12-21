package com.example.springdemo.controller;

import com.example.springdemo.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RequestMapping("/queue")
@RestController
@Slf4j
public class QueueController {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final QueueService queueService;
    private final SecureRandom random = new SecureRandom();

    public QueueController(NamedParameterJdbcTemplate jdbcTemplate, QueueService queueService) {
        this.jdbcTemplate = jdbcTemplate;
        this.queueService = queueService;
    }

    @GetMapping("next")
    public OrderedWorkItem next() {
        log.info("Fetching next...");
        return queueService.fetchNext();
    }

    @GetMapping("manyNext")
    public Map manyNext(@RequestParam(value = "threads", required = false) Optional<Integer> threads) throws InterruptedException {
        int threadCount = threads.orElse(10);
        log.info("Processing many items with {} threads...", threadCount);

        AtomicInteger itemsProcessed = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        Executor executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        long start = System.currentTimeMillis();
//        final SecureRandom random = new SecureRandom();

        final List<OrderedWorkItem> allNext = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
//                    IntStream.rangeClosed(1, 1000).forEach((loopCount) -> {
                    OrderedWorkItem item = this.queueService.fetchNext();
                    while (item.getWid() != null) {
//                        allNext.add(item);
                        try {
                            TimeUnit.MILLISECONDS.sleep(random.nextInt(0, 50));
                        } catch (InterruptedException ignored) {
                        }
                        String status = (random.nextInt(1, 101) == 3) ? "E" : "C";
                        setStatus(item, status);
                        itemsProcessed.incrementAndGet();
                        if ("E".equals(status)) {
                            errorCount.incrementAndGet();
                        }
                        item = this.queueService.fetchNext();
                    }
//                    });
                } catch (Exception e) {
                    log.error("Thread " + Thread.currentThread().getName() + " (" + Thread.currentThread().getId() + ") failed: {}", e.getClass().getSimpleName());
                } finally {
                    latch.countDown();
                }
            });
        }
        while (!latch.await(20, TimeUnit.SECONDS)) {
            log.info("--> {} threads active, {} items processed, {} errors, {} duration",
                    latch.getCount(),
                    itemsProcessed.intValue(),
                    errorCount.intValue(),
                    (System.currentTimeMillis() - start) / 1000);
        }
        log.info("manyNext complete");
        long duration = System.currentTimeMillis() - start;

        return Map.of("count", itemsProcessed.intValue(),
                "threads", threadCount,
                "total-s", duration,
                "avg-ms", Math.round(duration / itemsProcessed.intValue()));
    }

    @GetMapping("addMany")
    public Map addMany(@RequestParam(value = "items", required = false) Optional<Integer> items,
                       @RequestParam(value = "order", required = false) Optional<Integer> order) {
        String itemCount = String.valueOf(items.orElse(1000));
        log.info("Adding {} items...", itemCount);
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
        int count = jdbcTemplate.queryForObject(sql.replace("#count#", itemCount)
                        .replace("#order#", String.valueOf(order.orElse(25))),
                Collections.emptyMap(),
                Integer.class);
        return Map.of("rowsAdded", count);
    }

    @GetMapping("resetErrors")
    public Map resetErrors() {
        String sql = """
                update orderedqueue
                   set status = 'R'
                 where status = 'E'
                   and retry_cnt < 4
                   and dateadd(SECOND, 10*retry_cnt, update_dt) < getdate()
                """;
        log.info("Resetting errors...");
        int count = jdbcTemplate.update(sql, Collections.emptyMap());
        return Map.of("rowsReset", count);
    }


    private OrderedWorkItem fetchNext() {
        String sql = """
                DECLARE @item varchar(36);
                DECLARE @itemTable TABLE (
                    wid varchar(36)
                );
                update orderedqueue
                 set status='I',
                     update_dt = getdate(),
                     msg = :msg
                output inserted.wid into @itemTable
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
                select @item = wid from @itemTable;
                select * from orderedqueue where wid = @item;
                """;
//        try (Stream<OrderedWorkItem> stream = jdbcTemplate.queryForStream(sql, Map.of("msg", Thread.currentThread().getId()), (row, index) -> {
        List<OrderedWorkItem> stream = jdbcTemplate.query(sql, Map.of("msg", Thread.currentThread().getId()), (row, index) -> {
            OrderedWorkItem item = new OrderedWorkItem();
            item.setWid(row.getString("wid"));
            item.setOrderId(row.getString("order_id"));
            return item;
        });
        Optional<OrderedWorkItem> result = stream.stream().findFirst();
        return result.orElse(new OrderedWorkItem());
//        }
    }

    private void setStatus(OrderedWorkItem item, String status) {
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

    public static class OrderedWorkItem {
        private String wid;
        private String orderId;
        private String id;

        public OrderedWorkItem() {
        }

        public String getWid() {
            return wid;
        }

        public void setWid(String wid) {
            this.wid = wid;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
