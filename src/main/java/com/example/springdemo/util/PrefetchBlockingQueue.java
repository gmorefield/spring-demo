package com.example.springdemo.util;

import com.example.springdemo.controller.QueueController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class PrefetchBlockingQueue<T> extends ArrayBlockingQueue {
    private static final Logger logger = LoggerFactory.getLogger(PrefetchBlockingQueue.class);
    private final ReentrantLock takeLock = new ReentrantLock(true);
    private final int minSize;
    private final int fetchSize;
    private final ItemProvider<Integer, List<T>> supplier;
    private transient boolean draining = false;
    private final AtomicInteger finalChecks;
//    private final List<T> cache = new ArrayList<>();

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

    public T fetch() throws InterruptedException {
        final ReentrantLock lock = this.takeLock;
        lock.lockInterruptibly();
        try {
            if (size() < minSize && !draining) {
                List<T> items = (List<T>) fetchMany();
                addAll(items);

//                cache.addAll(items);
//                if (cache.size() != this.size()) {
//                    logger.info("Prefetch: items={},size={},cache={},counter={}", items.size(), this.size(), cache.size(), QueueRepository.fetchCounter.get());
////                    logger.error("***Cache size {} does not match queue size {}***", cache.size(), this.size());
////                    throw new IllegalStateException("Cache size does not match queue size.");
//                }
//                if (!cache.isEmpty() && cache.get(0) instanceof QueueController.OrderedWorkItem) {
//                    if (cache.stream().map(i -> ((QueueController.OrderedWorkItem) i).getOrderId()).distinct().count() != cache.size()) {
//                        String details = cache.stream()
//                                .map(i -> ((QueueController.OrderedWorkItem) i).getShortKey())
//                                .collect(Collectors.joining(", "));
//                        logger.error("***Duplicate groups detected in prefetched items: {}", details);
//                        throw new IllegalStateException("Duplicate groups detected in prefetched items.");
//                    }
//                } else {
//                    logger.info("Prefetched {} items.", items.size());
//                }
            }
            if (size() <= 0) {
                return (T) new QueueController.OrderedWorkItem();
            }
        } finally {
            lock.unlock();
        }
        T next = (T) super.take();

//        lock.lockInterruptibly();
//        try {
//            if (!cache.remove(next)) {
//                logger.warn("***Item {} taken from queue was not found in cache: {}", ((QueueController.OrderedWorkItem) next).getLongKey(),
//                        cache.stream()
//                                .map(i -> ((QueueController.OrderedWorkItem) i).getLongKey())
//                                .collect(Collectors.joining(", ")));
//            }
//        } finally {
//            lock.unlock();
//        }
        return next;
    }

    private List<T> fetchMany() {
        List<T> items = Collections.emptyList();
        try {
            items = supplier.apply(fetchSize);
            if (items.isEmpty()) {
                if (finalChecks.decrementAndGet() <= 0) {
                    logger.info("Prefetch was empty. Started draining");
                    draining = true;
                }
            } else if (finalChecks.intValue() < minSize) {
                finalChecks.set(minSize);
            }
            return items;
        } catch (Exception e) {
            //TODO: enhance this to handle multiple attempts
            draining = true;
            logger.error("Failed to prefetch items. Will drain queue: {}", e.getMessage());
        }
        return items;
    }
}
