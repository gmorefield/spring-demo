package com.example.springdemo.service;

import com.example.springdemo.controller.QueueController;
import com.example.springdemo.data.QueueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.function.ThrowingSupplier;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Service
@Slf4j
public class QueueService {
    private final QueueRepository queueRepository;
    private final SecureRandom random = new SecureRandom();

    public QueueService(QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    public QueueController.OrderedWorkItem orderNext() {
        log.info("Fetching next...");
        return queueRepository.orderedFetchNext();
    }

    public Map orderManyNext(int threadCount) throws InterruptedException {
        log.info("Processing order/manyNext with {} threads...", threadCount);

        return processItems("order/manyNext", threadCount, queueRepository::orderedFetchNext, queueRepository::orderedSetStatus);
    }

    public Map orderManyPrefetch(final int threadCount, final int fetchSize) throws InterruptedException {
        log.info("Processing order/manyPrefetch with {} threads and prefetch size {}...", threadCount, fetchSize);

        final QueueController.PrefetchBlockingQueue<QueueController.OrderedWorkItem> blockingQueue = new QueueController.PrefetchBlockingQueue<>(threadCount, fetchSize,
                queueRepository::orderedFetchMany);
        return processItems("order/manyPrefetch", threadCount, blockingQueue::fetch, queueRepository::orderedSetStatus);
    }

    public int orderAddMany(final int itemCount, final int uniqueOrders) {
        log.info("Adding {} items...", itemCount);
        return queueRepository.orderedAddMany(itemCount, uniqueOrders);
    }

    public int orderResetErrors() {
        log.info("Resetting errors...");
        return queueRepository.orderedResetErrors();
    }

    public QueueController.OrderedWorkItem next() {
        log.info("Fetching next...");
        return queueRepository.orderedFetchNext();
    }

    public Map manyNext(final int threadCount) throws InterruptedException {
        log.info("Processing manyNext with {} threads...", threadCount);

        return processItems("manyNext", threadCount, queueRepository::fetchNext, queueRepository::setStatus);
    }

    public Map manyPrefetch(final int threadCount, final int fetchSize) throws InterruptedException {
        log.info("Processing manyPrefetch with {} threads and prefetch size {}...", threadCount, fetchSize);

        final QueueController.PrefetchBlockingQueue<QueueController.OrderedWorkItem> blockingQueue = new QueueController.PrefetchBlockingQueue<>(threadCount, fetchSize,
                queueRepository::fetchMany);

        return processItems("manyPrefetch", threadCount, blockingQueue::fetch, queueRepository::setStatus);
    }

    public int addMany(final int itemCount) {
        log.info("Adding {} items...", itemCount);
        return queueRepository.addMany(itemCount);
    }

    public int resetErrors() {
        log.info("Resetting errors...");
        return queueRepository.resetErrors();
    }

    private Map processItems(final String methodName, final int threadCount, final ThrowingSupplier<QueueController.OrderedWorkItem> itemSupplier,
                             final BiConsumer<QueueController.OrderedWorkItem, String> statusConsumer) throws InterruptedException {
        AtomicInteger itemsProcessed = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        Executor executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);

        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    QueueController.OrderedWorkItem item = itemSupplier.get();
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
}
