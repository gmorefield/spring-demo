package com.example.springdemo.tasks;

import com.example.springdemo.service.QueueService;
import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="spring.main.web-application-type", havingValue = "NONE")
public class QueueTask implements ApplicationRunner, ApplicationContextAware {
    private final QueueService queueService;
    private ApplicationContext applicationContext;

    public QueueTask(QueueService queueService) {
        this.queueService = queueService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("action")) {
            new IllegalArgumentException("--action is required");
        }

        String action = args.getOptionValues("action").get(0);
        int threadCount = getArgument(args, "threads", 10);
        int fetchSize = getArgument(args, "preFetch", 20);

        if ("orderedManyPrefetch".equals(action)) {
            queueService.orderManyPrefetch(threadCount, fetchSize);
        }
    }

    private int getArgument(ApplicationArguments args, final String name, final int defaultValue) {
        if (args.containsOption(name)) {
            return Integer.parseInt(args.getOptionValues(name).get(0));
        } else {
            return defaultValue;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
