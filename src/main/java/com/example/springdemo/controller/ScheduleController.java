package com.example.springdemo.controller;

import com.example.springdemo.config.SchedulerConfig;
import com.example.springdemo.tasks.SampleTask;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.OneTimeTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tasks")
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class ScheduleController implements ApplicationContextAware, SchedulingConfigurer {
    private ApplicationContext appContext;
    private final Set<ScheduledTask> scheduledTasks = new LinkedHashSet<>(16);
    private final Set<ScheduledTask> cancelledTasks = new LinkedHashSet<>(16);
    private ScheduledTaskRegistrar taskRegistrar;

    public ScheduleController(SchedulerConfig config, TaskScheduler taskScheduler, Optional<SampleTask> sampleTask, ObjectProvider<ScheduledTaskHolder> holders) {
        holders.stream()
                .flatMap(h -> h.getScheduledTasks().stream())
                .forEach(scheduledTasks::add);
    }

    @GetMapping()
    public List<Map> listTriggers(@RequestParam final Optional<Boolean> includeCanceled) {
        return List.of(taskRegistrar.getScheduledTasks(), this.scheduledTasks)
                .stream()
                .flatMap(Set::stream)
                .filter(st -> includeCanceled.orElse(false) || !cancelledTasks.contains(st))
                .map(st -> {
                    Task t = st.getTask();
                    if (t instanceof FixedDelayTask) {
                        FixedDelayTask fdt = (FixedDelayTask) t;
                        return Map.of("interval", fdt.getIntervalDuration(),
                                "initialDelay", fdt.getInitialDelayDuration(),
                                "type", "fixedDuration",
                                "task", fdt.toString(),
                                "isCancelled", this.cancelledTasks.contains(st));
                    } else if (t instanceof FixedRateTask) {
                        FixedRateTask frt = (FixedRateTask) t;
                        return Map.of("interval", frt.getIntervalDuration(),
                                "initialDelay", frt.getInitialDelayDuration(),
                                "type", "fixedRate",
                                "task", frt.toString(),
                                "isCancelled", this.cancelledTasks.contains(st));
                    } else if (t instanceof CronTask) {
                        CronTask ct = (CronTask) t;
                        return Map.of("expression", ct.getExpression(),
                                "type", "fixedRate",
                                "task", ct.toString(),
                                "isCancelled", this.cancelledTasks.contains(st));
                    } else if (t instanceof OneTimeTask) {
                        OneTimeTask ott = (OneTimeTask) t;
                        return Map.of("initialDelay", ott.getInitialDelayDuration(),
                                "type", "oneTimeTask",
                                "task", ott.toString(),
                                "isCancelled", this.cancelledTasks.contains(st));
                    }
                    return Collections.emptyMap();
                })
                .collect(Collectors.toList());
    }

    @PostMapping()
    public ResponseEntity<?> addTrigger(@RequestBody TriggerSpec spec) throws NoSuchMethodException {
        Object targetObject = appContext.getBean(spec.targetBean);
        ScheduledMethodRunnable runnable = new ScheduledMethodRunnable(targetObject, spec.targetMethod);
        if (spec.triggerType == TriggerType.CRON) {
            this.scheduledTasks.add(taskRegistrar.scheduleCronTask(
                    new CronTask(runnable, spec.cronExpression)));
        } else if (spec.triggerType == TriggerType.FIXED_RATE) {
            this.scheduledTasks.add(taskRegistrar.scheduleFixedRateTask(
                    new FixedRateTask(runnable, spec.period, spec.initialDelay)));
        } else if (spec.triggerType == TriggerType.FIXED_DELAY) {
            this.scheduledTasks.add(taskRegistrar.scheduleFixedDelayTask(
                    new FixedDelayTask(runnable, spec.period, spec.initialDelay)));
        } else if (spec.triggerType == TriggerType.ONE_TIME) {
            this.scheduledTasks.add(taskRegistrar.scheduleOneTimeTask(
                    new OneTimeTask(runnable, spec.initialDelay)));
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> cancelTrigger(@PathVariable String name) {
        List<ScheduledTask> tasks = scheduledTasks.stream()
                .filter(t -> t.toString().equals(name) && !cancelledTasks.contains(t))
                .collect(Collectors.toList());
        if (tasks.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        tasks.stream()
                .forEach(t -> {
                    t.cancel(false);
                    cancelledTasks.add(t);
                });

        return ResponseEntity.ok().build();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        this.taskRegistrar = taskRegistrar;
    }

    public enum TriggerType {
        CRON, FIXED_RATE, FIXED_DELAY, ONE_TIME
    }

    public static class TriggerSpec {
        public TriggerType triggerType;
        public String cronExpression;
        public Duration period;
        public Duration initialDelay;
        public String targetBean;
        public String targetMethod;
    }
}
