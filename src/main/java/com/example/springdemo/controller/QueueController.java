package com.example.springdemo.controller;

import com.example.springdemo.data.QueueRepository;
import com.example.springdemo.service.QueueService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequestMapping("/queue")
@RestController
@Slf4j
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class QueueController {
    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/order/status")
    public Map<String, Integer> orderStatus() {
        return queueService.orderStatus();
    }

    @GetMapping("/order/next")
    public OrderedWorkItem orderNext() {
        return queueService.orderNext();
    }

    @GetMapping("/order/manyNext")
    public Map<String, Object> orderManyNext(@RequestParam(value = "threads", required = false, defaultValue = "10") Integer threads,
                                             @RequestParam(value = "errorRate", required = false, defaultValue = "1") Integer errorRate,
                                             @RequestParam(value = "fetchType", required = false, defaultValue = "OUTPUT_PARTITION") QueueRepository.FETCH_TYPE fetchType) throws InterruptedException {
        return queueService.orderManyNext(threads, errorRate, fetchType);
    }

    @GetMapping("/order/manyPrefetch")
    public Map<String, Object> orderManyPrefetch(@RequestParam(value = "threads", required = false, defaultValue = "10") Integer threads,
                                                 @RequestParam(value = "prefetch", required = false, defaultValue = "20") Integer fetch,
                                                 @RequestParam(value = "errorRate", required = false, defaultValue = "1") Integer errorRate,
                                                 @RequestParam(value = "fetchType", required = false, defaultValue = "OUTPUT_PARTITION") String fetchType) throws InterruptedException {
        return queueService.orderManyPrefetch(threads,
                fetch,
                errorRate,
                QueueRepository.FETCH_TYPE.valueOf(fetchType));
    }

    @GetMapping("/order/addSingle")
    public Map<String, Object> orderAddSingle(@RequestParam(value = "wid") @NotNull String wid,
                                              @RequestParam(value = "order") @NotNull Integer order) {
        int countAdded = queueService.orderAddSingle(java.util.UUID.fromString(wid), order);

        Map<String, Object> data = Map.of("rowsAdded", countAdded);
        log.info("order/addSingle complete: {}", data);
        return data;
    }

    @GetMapping("/order/mergeSingle")
    public Map<String, Object> orderMergeSingle(@RequestParam(value = "wid") @NotNull String wid,
                                                @RequestParam(value = "order") @NotNull Integer order) {
        int countMerged = queueService.orderMergeSingle(java.util.UUID.fromString(wid), order);

        Map<String, Object> data = Map.of("rowsMerged", countMerged);
        log.info("order/mergeSingle complete: {}", data);
        return data;
    }

    @GetMapping("/order/addUniqueSingle")
    public Map<String, Object> orderAddUniqueSingle(@RequestParam(value = "wid") @NotNull String wid,
                                                    @RequestParam(value = "order") @NotNull Integer order) {
        int countMerged = queueService.orderAddUniqueSingle(java.util.UUID.fromString(wid), order);

        Map<String, Object> data = Map.of("rowsMerged", countMerged);
        log.info("order/addUniqueSingle complete: {}", data);
        return data;
    }

    @GetMapping("/order/addMany")
    public Map<String, Object> orderAddMany(@RequestParam(value = "items", required = false, defaultValue = "1000") Integer items,
                                            @RequestParam(value = "order", required = false, defaultValue = "25") Integer order,
                                            @RequestParam(value = "dropAll", required = false, defaultValue = "false") Boolean dropAll) {
        int countAdded = queueService.orderAddMany(items, order, dropAll);

        Map<String, Object> data = Map.of("rowsAdded", countAdded, "dropAll", dropAll);
        log.info("order/addMany complete: {}", data);
        return data;
    }

    @GetMapping("/order/resetErrors")
    public Map<String, Object> orderResetErrors() {
        int count = queueService.orderResetErrors();
        log.info("reset complete: {}", Map.of("count", count));
        return Map.of("rowsReset", count);
    }

    @GetMapping("/order/verify")
    public List<Map<String, Object>> orderVerify() {
        return queueService.orderVerify();
    }


    @GetMapping("/status")
    public Map<String, Integer> status() {
        return queueService.status();
    }

    @GetMapping("/next")
    public OrderedWorkItem next() {
        return queueService.next();
    }

    @GetMapping("/manyNext")
    public Map<String, Object> manyNext(@RequestParam(value = "threads", required = false, defaultValue = "10") Integer threads,
                                        @RequestParam(value = "errorRate", required = false, defaultValue = "1") Integer errorRate,
                                        @RequestParam(value = "useFetch", required = false, defaultValue = "true") Boolean useFetch) throws InterruptedException {
        return queueService.manyNext(threads, errorRate, useFetch);
    }

    @GetMapping("/manyPrefetch")
    public Map<String, Object> manyPrefetch(@RequestParam(value = "threads", required = false, defaultValue = "10") Integer threads,
                                            @RequestParam(value = "prefetch", required = false, defaultValue = "20") Integer fetch,
                                            @RequestParam(value = "errorRate", required = false, defaultValue = "1") Integer errorRate,
                                            @RequestParam(value = "useFetch", required = false, defaultValue = "true") Boolean useFetch) throws InterruptedException {
        return queueService.manyPrefetch(threads, fetch, errorRate, useFetch);
    }

    @GetMapping("/addMany")
    public Map<String, Object> addMany(@RequestParam(value = "items", required = false, defaultValue = "1000") Integer items) {
        int count = queueService.addMany(items);

        Map<String, Object> data = Map.of("rowsAdded", count);
        log.info("addMany complete: {}", data);
        return data;
    }

    @GetMapping("/resetErrors")
    public Map<String, Object> resetErrors() {
        int count = queueService.resetErrors();

        log.info("reset complete: {}", Map.of("count", count));
        return Map.of("rowsReset", count);
    }

    @GetMapping("/measures")
    public Map<String, QueueService.Measure> getMeasures() {
        return queueService.getMeasures();
    }

    @DeleteMapping("/measures")
    public void clearMeasures() {
        queueService.clearMeasures();
    }

    @Setter
    @Getter
    public static class OrderedWorkItem {
        private String wid;
        private String orderId;
        private String id;

        public OrderedWorkItem() {
        }

        @Override
        public String toString() {
            // return a JSON-like string representation
            return "{" +
                    "\"wid\":\"" + wid + "\"," +
                    "\"orderId\":\"" + orderId + "\"," +
                    "\"id\":\"" + id + "\"" +
                    "}";
        }

        public String getShortKey() {
            return orderId + "-" + id;
        }

        public String getLongKey() {
            return orderId + "-" + id + "-" + wid;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            OrderedWorkItem that = (OrderedWorkItem) obj;
            if (!Objects.equals(wid, that.wid)) return false;
            if (!Objects.equals(orderId, that.orderId)) return false;
            return Objects.equals(id, that.id);
        }
    }

}
