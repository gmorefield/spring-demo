package com.example.springdemo.controller;

import com.example.springdemo.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.function.ThrowingSupplier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

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

    @GetMapping("/order/next")
    public OrderedWorkItem orderNext() {
        log.info("Fetching next...");
        return queueService.orderedFetchNext();
    }

    @GetMapping("/order/manyNext")
    public Map orderManyNext(@RequestParam(value = "threads", required = false) Optional<Integer> threads) throws InterruptedException {
        int threadCount = threads.orElse(10);
        log.info("Processing order/manyNext with {} threads...", threadCount);

        return processItems("order/manyNext", threadCount, queueService::orderedFetchNext, this::orderedSetStatus);
    }

    @GetMapping("/order/manyPrefetch")
    public Map orderManyPrefetch(@RequestParam(value = "threads", required = false) Optional<Integer> threads,
                                 @RequestParam(value = "prefetch", required = false) Optional<Integer> fetch) throws InterruptedException {
        int threadCount = threads.orElse(10);
        int fetchSize = fetch.orElse(20);
        log.info("Processing order/manyPrefetch with {} threads and prefetch size {}...", threadCount, fetchSize);

        final PrefetchBlockingQueue<OrderedWorkItem> blockingQueue = new PrefetchBlockingQueue<>(threadCount, fetchSize,
                queueService::orderedFetchMany);
        return processItems("order/manyPrefetch", threadCount, blockingQueue::fetch, this::orderedSetStatus);
    }

    @GetMapping("/order/addMany")
    public Map orderAddMany(@RequestParam(value = "items", required = false) Optional<Integer> items,
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

        Map data = Map.of("rowsAdded", count);
        log.info("order/addMany complete: {}", data);
        return data;
    }

    @GetMapping("/order/resetErrors")
    public Map orderResetErrors() {
        String sql = """
                update orderedqueue
                   set status = 'R'
                 where status = 'E'
                   and retry_cnt < 4
                   and dateadd(SECOND, 10*retry_cnt, update_dt) < getdate()
                """;
        log.info("Resetting errors...");
        int count = jdbcTemplate.update(sql, Collections.emptyMap());

        log.info("reset complete: {}", Map.of("count", count));
        return Map.of("rowsReset", count);
    }

    @GetMapping("/next")
    public OrderedWorkItem next() {
        log.info("Fetching next...");
        return queueService.orderedFetchNext();
    }

    @GetMapping("/manyNext")
    public Map manyNext(@RequestParam(value = "threads", required = false) Optional<Integer> threads) throws InterruptedException {
        int threadCount = threads.orElse(10);
        log.info("Processing manyNext with {} threads...", threadCount);

        return processItems("manyNext", threadCount, queueService::fetchNext, this::setStatus);
    }

    @GetMapping("/manyPrefetch")
    public Map manyPrefetch(@RequestParam(value = "threads", required = false) Optional<Integer> threads,
                            @RequestParam(value = "prefetch", required = false) Optional<Integer> fetch) throws InterruptedException {
        int threadCount = threads.orElse(10);
        int fetchSize = fetch.orElse(20);
        log.info("Processing manyPrefetch with {} threads and prefetch size {}...", threadCount, fetchSize);

        final PrefetchBlockingQueue<OrderedWorkItem> blockingQueue = new PrefetchBlockingQueue<>(threadCount, fetchSize,
                queueService::fetchMany);

        return processItems("manyPrefetch", threadCount, blockingQueue::fetch, this::setStatus);
    }

    @GetMapping("/addMany")
    public Map addMany(@RequestParam(value = "items", required = false) Optional<Integer> items) {
        String itemCount = String.valueOf(items.orElse(1000));
        log.info("Adding {} items...", itemCount);
        String sql = """
                declare @numcreated int = 0, @order int = 1
                while (@numcreated < #count#)
                begin
                	insert into workqueue (wid,status) values (newid(),'R');
                	select @numcreated = @numcreated + 1;
                end;
                select @numcreated;
                """;
        int count = jdbcTemplate.queryForObject(sql.replace("#count#", itemCount),
                Collections.emptyMap(),
                Integer.class);

        Map data = Map.of("rowsAdded", count);
        log.info("addMany complete: {}", data);
        return data;
    }

    @GetMapping("/resetErrors")
    public Map resetErrors() {
        String sql = """
                update workqueue
                   set status = 'R'
                 where status = 'E'
                   and retry_cnt < 4
                   and dateadd(SECOND, 10*retry_cnt, update_dt) < getdate()
                """;
        log.info("Resetting errors...");
        int count = jdbcTemplate.update(sql, Collections.emptyMap());

        log.info("reset complete: {}", Map.of("count", count));
        return Map.of("rowsReset", count);
    }


    private Map processItems(final String methodName, final int threadCount, final ThrowingSupplier<OrderedWorkItem> itemSupplier,
                             final BiConsumer<OrderedWorkItem, String> statusConsumer) throws InterruptedException {
        AtomicInteger itemsProcessed = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        Executor executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);

        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    OrderedWorkItem item = itemSupplier.get();
                    ;
                    while (item.getWid() != null) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(random.nextInt(0, 50));
                        } catch (InterruptedException ignored) {
                        }
                        String status = (random.nextInt(1, 101) == 3) ? "E" : "C";
                        statusConsumer.accept(item, status);
                        itemsProcessed.incrementAndGet();
                        if ("E".equals(status)) {
                            errorCount.incrementAndGet();
                        }
                        item = itemSupplier.get();
                    }
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

        long duration = System.currentTimeMillis() - start;
        Map stats = Map.of("count", itemsProcessed.intValue(),
                "errors", errorCount.intValue(),
                "threads", threadCount,
                "total-s", duration > 0 ? duration / 1000 : 0,
                "avg-ms", itemsProcessed.intValue() > 0 ? Math.round(duration / itemsProcessed.intValue()) : 0);
        log.info("{} complete: {}", methodName, stats);

        return stats;
    }

    private void orderedSetStatus(OrderedWorkItem item, String status) {
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

    private void setStatus(OrderedWorkItem item, String status) {
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

    public static class PrefetchBlockingQueue<T> extends ArrayBlockingQueue {
        private final ReentrantLock takeLock = new ReentrantLock();
        private final int minSize;
        private final int fetchSize;
        private final ItemProvider<Integer, List<T>> supplier;
        private transient boolean draining = false;
        private final AtomicInteger finalChecks;

        @FunctionalInterface
        public interface ItemProvider<T, R> {
            R apply(T count);
        }

        public PrefetchBlockingQueue(int minSize, int fetchSize, ItemProvider<Integer, List<T>> supplier) {
            super(minSize + fetchSize, true);
            this.finalChecks = new AtomicInteger(1);
            this.minSize = minSize;
            this.fetchSize = fetchSize;
            this.supplier = supplier;
        }

        public <T> T fetch() throws InterruptedException {
            final ReentrantLock lock = this.takeLock;
            lock.lockInterruptibly();
            try {
                if (size() < minSize && !draining) {
                    List<T> items = (List<T>) fetchMany();
                    addAll(items);
                }
                if (size() <= 0) {
                    return (T) new OrderedWorkItem();
                }
            } finally {
                lock.unlock();
            }
            return (T) super.take();
        }

        private List<T> fetchMany() {
            List<T> items = Collections.emptyList();
            try {
                items = supplier.apply(fetchSize);
                if (items.isEmpty()) {
                    if (finalChecks.decrementAndGet() <= 0) {
                        log.info("Prefetch was empty. Started draining");
                        draining = true;
                    }
                } else if (finalChecks.intValue() < minSize) {
                    finalChecks.set(minSize);
                }
                return items;
            } catch (Exception e) {
                //TODO: enhance this to handle multiple attempts
                draining = true;
                log.error("Failed to prefetch orderedqueue items. Will drain queue: {}", e.getMessage());
            }
            return items;
        }
    }
}
