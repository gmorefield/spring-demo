package com.example.springdemo.controller;

import com.example.springdemo.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@RequestMapping("/queue")
@RestController
@Slf4j
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class QueueController {
    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/order/next")
    public OrderedWorkItem orderNext() {
        return queueService.orderNext();
    }

    @GetMapping("/order/manyNext")
    public Map orderManyNext(@RequestParam(value = "threads", required = false) Optional<Integer> threads,
                             @RequestParam(value = "errorRate", required = false) Optional<Integer> errorRate) throws InterruptedException {
        return queueService.orderManyNext(threads.orElse(10), errorRate.orElse(1));
    }

    @GetMapping("/order/manyPrefetch")
    public Map orderManyPrefetch(@RequestParam(value = "threads", required = false) Optional<Integer> threads,
                                 @RequestParam(value = "prefetch", required = false) Optional<Integer> fetch,
                                 @RequestParam(value = "errorRate", required = false) Optional<Integer> errorRate) throws InterruptedException {
        return queueService.orderManyPrefetch(threads.orElse(10), fetch.orElse(20), errorRate.orElse(1));
    }

    @GetMapping("/order/addMany")
    public Map orderAddMany(@RequestParam(value = "items", required = false) Optional<Integer> items,
                            @RequestParam(value = "order", required = false) Optional<Integer> order) {
        int countAdded = queueService.orderAddMany(items.orElse(1000), order.orElse(25));

        Map data = Map.of("rowsAdded", countAdded);
        log.info("order/addMany complete: {}", data);
        return data;
    }

    @GetMapping("/order/resetErrors")
    public Map orderResetErrors() {
        int count = queueService.orderResetErrors();
        log.info("reset complete: {}", Map.of("count", count));
        return Map.of("rowsReset", count);
    }

    @GetMapping("/next")
    public OrderedWorkItem next() {
        return queueService.next();
    }

    @GetMapping("/manyNext")
    public Map manyNext(@RequestParam(value = "threads", required = false) Optional<Integer> threads,
                        @RequestParam(value = "errorRate", required = false) Optional<Integer> errorRate) throws InterruptedException {
        return queueService.manyNext(threads.orElse(10), errorRate.orElse(1));
    }

    @GetMapping("/manyPrefetch")
    public Map manyPrefetch(@RequestParam(value = "threads", required = false) Optional<Integer> threads,
                            @RequestParam(value = "prefetch", required = false) Optional<Integer> fetch,
                            @RequestParam(value = "errorRate", required = false) Optional<Integer> errorRate) throws InterruptedException {
        return queueService.manyPrefetch(threads.orElse(10), fetch.orElse(20), errorRate.orElse(1));
    }

    @GetMapping("/addMany")
    public Map addMany(@RequestParam(value = "items", required = false) Optional<Integer> items) {
        int count = queueService.addMany(items.orElse(1000));

        Map data = Map.of("rowsAdded", count);
        log.info("addMany complete: {}", data);
        return data;
    }

    @GetMapping("/resetErrors")
    public Map resetErrors() {
        int count = queueService.resetErrors();

        log.info("reset complete: {}", Map.of("count", count));
        return Map.of("rowsReset", count);
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
