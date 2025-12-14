package com.example.springdemo.service;

import com.example.springdemo.controller.QueueController;
import com.example.springdemo.data.QueueRepository;
import com.example.springdemo.util.PrefetchBlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.function.ThrowingSupplier;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Service
@Slf4j
public class QueueService {
    private final QueueRepository queueRepository;
    private final SecureRandom random = new SecureRandom();
    private final AtomicBoolean running = new AtomicBoolean(true);

    public QueueService(QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    public QueueController.OrderedWorkItem orderNext() {
        log.info("Fetching next ordered...");
        return queueRepository.orderedFetchNext(QueueRepository.FETCH_TYPE.OUTPUT_NOT_EXISTS);
    }

    public Map<String, Integer> orderStatus() {
        log.info("Fetching order status counts...");
        return queueRepository.getOrderedStatusCounts();
    }

    public Map<String, Object> orderManyNext(int threadCount, int errorRate, QueueRepository.FETCH_TYPE fetchType) throws InterruptedException {
        log.info("Processing order/manyNext with {} threads...", threadCount);

        return processItems("orderedFetchNext", threadCount, errorRate,
                () -> queueRepository.orderedFetchNext(fetchType), queueRepository::orderedSetStatus, Map.of("fetchType", fetchType.name()));
    }

    public Map<String, Object> orderManyPrefetch(final int threadCount, final int fetchSize, int errorRate, QueueRepository.FETCH_TYPE fetchType) throws InterruptedException {
        log.info("Processing order/manyPrefetch [{}] with {} threads and prefetch size {}...", fetchType, threadCount, fetchSize);

        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") final PrefetchBlockingQueue<QueueController.OrderedWorkItem> blockingQueue = new PrefetchBlockingQueue<>(threadCount, fetchSize,
                (limit) -> queueRepository.orderFetchMany(limit, fetchType));
        Map<String, Object> context = Map.of("fetchType", fetchType.name());
        Map<String, Object> result = processItems("orderFetchMany", threadCount, errorRate,
                blockingQueue::fetch, queueRepository::orderedSetStatus, context);
        if (!blockingQueue.isEmpty()) {
            log.info("Returning {} queue prefetch items; {}", blockingQueue.size(), context);
            QueueController.OrderedWorkItem[] items = blockingQueue.toArray(QueueController.OrderedWorkItem[]::new);
            queueRepository.orderedResetItemsToReady(List.of(items));
        }
        return result;
    }

    public int orderMergeSingle(final UUID wid, final int orderId) {
        return queueRepository.orderedMergeSingle(wid, orderId);
    }

    public int orderAddSingle(final UUID wid, final int orderId) {
        return queueRepository.orderedAddSingle(wid, orderId);
    }

    public int orderAddUniqueSingle(final UUID wid, final int orderId) {
        return queueRepository.orderedAddUniqueSingle(wid, orderId);
    }

    public int orderAddMany(final int itemCount, final int uniqueOrders, boolean dropAll) {
        return queueRepository.orderedAddMany(itemCount, uniqueOrders, dropAll);
    }

    public int orderResetErrors() {
        log.info("Resetting ordered errors...");
        return queueRepository.orderedResetErrors();
    }

    public List<Map<String, Object>> orderVerify() {
        log.info("Verifying ordered items...");
        return queueRepository.orderVerify();
    }

    public Map<String, Integer> status() {
        log.info("Fetching status counts...");
        return queueRepository.getStatusCounts();
    }

    public QueueController.OrderedWorkItem next() {
        log.info("Fetching next...");
        return queueRepository.fetchNext();
    }

    public QueueController.OrderedWorkItem selectNext() {
        log.info("Selecting next...");
        return queueRepository.selectNext();
    }

    public Map<String, Object> manyNext(final int threadCount, int errorRate, boolean useFetch) throws InterruptedException {
        log.info("Processing manyNext with {} threads with {}...", threadCount,
                (useFetch ? "fetchNext" : "selectNext"));

        return processItems("manyNext", threadCount, errorRate,
                useFetch ? queueRepository::fetchNext : queueRepository::selectNext,
                queueRepository::setStatus, Map.of("useFetch", useFetch));
    }

    public Map<String, Object> manyPrefetch(final int threadCount, final int fetchSize, int errorRate, boolean useFetch) throws InterruptedException {
        log.info("Processing manyPrefetch with {} threads and prefetch size {}...", threadCount, fetchSize);

        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") final PrefetchBlockingQueue<QueueController.OrderedWorkItem> blockingQueue = new PrefetchBlockingQueue<>(threadCount, fetchSize,
                (useFetch ? queueRepository::fetchMany : queueRepository::selectMany));
        Map<String, Object> context = Map.of("useFetch", useFetch);

        Map<String, Object> result = processItems("manyPrefetch", threadCount, errorRate, blockingQueue::fetch, queueRepository::setStatus, context);
        if (!blockingQueue.isEmpty()) {
            log.info("Returning {} queue prefetch items; {}", blockingQueue.size(), context);
            QueueController.OrderedWorkItem[] items = blockingQueue.toArray(QueueController.OrderedWorkItem[]::new);
            queueRepository.resetItemsToReady(List.of(items));
        }
        return result;
    }

    public int addMany(final int itemCount) {
        log.info("Adding {} items...", itemCount);
        return queueRepository.addMany(itemCount);
    }

    public int resetErrors() {
        log.info("Resetting errors...");
        return queueRepository.resetErrors();
    }

    public Map<String, Measure> getMeasures() {
        // sort measures by totalDuration
        LinkedHashMap<String, Measure> sorted = new LinkedHashMap<>();
        measures.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getTotalDuration(), e1.getValue().getTotalDuration()))
                .forEachOrdered(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    @EventListener(classes = {ContextClosedEvent.class})
    public void onShutdown() {
        log.info("Application shutting down, stopping queues...");
        running.set(false);
    }

    public void clearMeasures() {
        measures.clear();
    }

    private static final ConcurrentHashMap<String, Measure> measures = new ConcurrentHashMap<>();

    public static void addMetric(String methodName, String fetchType, int itemsProcessed, long duration, int retries) {
        measures.computeIfAbsent(methodName + "-" + (fetchType == null ? "default" : fetchType), k -> new Measure())
                .add(itemsProcessed, duration, retries);
    }

    private Map<String, Object> processItems(final String methodName, final int threadCount, final int errorRate, final ThrowingSupplier<QueueController.OrderedWorkItem> itemSupplier,
                                             final BiConsumer<QueueController.OrderedWorkItem, String> statusConsumer, Map<String, Object> context) throws InterruptedException {
        AtomicInteger itemsProcessed = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(
                threadCount, // corePoolSize
                threadCount, // maximumPoolSize
                10, // keepAliveTime
                TimeUnit.SECONDS, // keepAliveTime unit
                new LinkedBlockingQueue<>(2) // workQueue with capacity 2
        );
        final CountDownLatch latch = new CountDownLatch(threadCount);

        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            taskExecutor.execute(() -> {
                try {
                    QueueController.OrderedWorkItem item = itemSupplier.get();

                    while (item.getWid() != null) {
                        // simulate processing
                        try {
                            TimeUnit.MILLISECONDS.sleep(random.nextInt(0, 50));
                        } catch (InterruptedException ignored) {
                        }

                        String status = (errorRate > 0 && random.nextInt(1, 101) <= errorRate) ? "E" : "C";
                        statusConsumer.accept(item, status);
                        itemsProcessed.incrementAndGet();
                        if ("E".equals(status)) {
                            errorCount.incrementAndGet();
                        }

                        item = running.get() && !Thread.interrupted() ? itemSupplier.get() : new QueueController.OrderedWorkItem();
                    }

                    if (!running.get() || Thread.interrupted()) {
                        log.info("Shutdown detected; thread {} exiting early", Thread.currentThread().getName());
                    }
                } catch (Exception e) {
                    log.error("Thread " + Thread.currentThread().getName() + " (" + Thread.currentThread().getId() + ") failed: {}", e.getClass().getSimpleName(), e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            while (running.get() && !latch.await(10, TimeUnit.SECONDS)) {
                log.info("-->{} {} {} threads active, {} items processed, {} errors, {} duration",
                        methodName,
                        context,
                        latch.getCount(),
                        itemsProcessed.intValue(),
                        errorCount.intValue(),
                        (System.currentTimeMillis() - start) / 1000);
            }

            if (!running.get()) {
                log.info("Shutdown detected; waiting for queue threads to finish...");
                taskExecutor.shutdown();
                if (!taskExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Forcing thread shutdown");
                    taskExecutor.shutdownNow();
                }
            }
        } catch (InterruptedException ie) {
            log.error("{} interrupted: {}", methodName, ie.getMessage());
            throw ie;
        }

        long duration = System.currentTimeMillis() - start;
        Map<String, Object> stats = new HashMap<>(Map.of("methodName", methodName,
                "count", itemsProcessed.intValue(),
                "errors", errorCount.intValue(),
                "threads", threadCount,
                "total-s", duration > 0 ? duration / 1000 : 0,
                "avg-ms", itemsProcessed.intValue() > 0 ? Math.round((float) duration / itemsProcessed.intValue()) : 0));
        stats.putAll(context);
        log.info("{} complete: {}", methodName, stats);

        addMetric(methodName, (String) context.get("fetchType"),
                itemsProcessed.intValue(), duration, 0);

        return stats;
    }

    public static final class Measure {
        private final AtomicInteger itemCount = new AtomicInteger(0);
        private final AtomicInteger totalDuration = new AtomicInteger(0);
        private final AtomicInteger totalRetries = new AtomicInteger(0);
        private final AtomicInteger avgDuration = new AtomicInteger(0);

        public void add(int itemsProcessed, long duration, int retries) {
            int c = itemCount.addAndGet(itemsProcessed);
            int d = totalDuration.addAndGet((int) duration);
            totalRetries.addAndGet(retries);

            avgDuration.set(c > 0 ? Math.round((float) d / c) : 0);
        }

        @SuppressWarnings("unused")
        public int getItemCount() {
            return itemCount.get();
        }

        public int getTotalDuration() {
            return totalDuration.get();
        }

        @SuppressWarnings("unused")
        public int getTotalRetries() {
            return totalRetries.get();
        }

        @SuppressWarnings("unused")
        public int getAvgDuration() {
            return avgDuration.get();
        }
    }
}
