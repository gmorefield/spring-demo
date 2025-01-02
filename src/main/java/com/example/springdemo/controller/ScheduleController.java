package com.example.springdemo.controller;

import com.example.springdemo.data.TasksRepository;
import com.example.springdemo.model.TriggerSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.OneTimeTask;
import org.springframework.scheduling.config.ScheduledTask;
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/tasks")
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class ScheduleController implements ApplicationContextAware, SchedulingConfigurer {
    private ApplicationContext appContext;
    private final Set<ScheduledTask> scheduledTasks = new LinkedHashSet<>(16);
    // TODO: consider removing cancelledTasks and just removing tasks from scheduledTasks
    private final Set<ScheduledTask> cancelledTasks = new LinkedHashSet<>(16);
    private ScheduledTaskRegistrar taskRegistrar;
    private final ScheduledAnnotationBeanPostProcessor scheduledAnnotationBeanPostProcessor;
    private final TasksRepository tasksRepository;

    public ScheduleController(ScheduledAnnotationBeanPostProcessor scheduledAnnotationBeanPostProcessor, TasksRepository tasksRepository) {
        this.scheduledAnnotationBeanPostProcessor = scheduledAnnotationBeanPostProcessor;
        this.tasksRepository = tasksRepository;
    }

    @GetMapping()
    public List<Map> listTriggers(@RequestParam final Optional<Boolean> includeCanceled) {
//        return List.of(taskRegistrar.getScheduledTasks(), this.scheduledTasks)
//                .stream()
//                .flatMap(Set::stream)
        return scheduledTasks.stream()
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

    @GetMapping("/persisted")
    public List<TriggerSpec> listPersistedTriggers(@RequestParam final Optional<Boolean> includeInactive) {
        return tasksRepository.findAll().stream()
                .filter(spec -> includeInactive.orElse(false) || spec.isEnabled())
                .collect(Collectors.toList());
    }

    @PostMapping()
    public ResponseEntity<?> addTrigger(@RequestBody TriggerSpec spec) throws NoSuchMethodException {
        Object targetObject = appContext.getBean(spec.getTargetBean());
        ScheduledMethodRunnable runnable = new ScheduledMethodRunnable(targetObject, spec.getTargetMethod());

        // TODO: consider switching to use of TaskScheduler outside of initial configuration
        if (spec.getTriggerType() == TriggerSpec.TriggerType.CRON) {
            this.scheduledTasks.add(taskRegistrar.scheduleCronTask(
                    new CronTask(runnable, spec.getCronExpression())));
        } else if (spec.getTriggerType() == TriggerSpec.TriggerType.FIXED_RATE) {
            this.scheduledTasks.add(taskRegistrar.scheduleFixedRateTask(
                    new FixedRateTask(runnable, spec.getInterval(), spec.getInitialDelay())));
        } else if (spec.getTriggerType() == TriggerSpec.TriggerType.FIXED_DELAY) {
            this.scheduledTasks.add(taskRegistrar.scheduleFixedDelayTask(
                    new FixedDelayTask(runnable, spec.getInterval(), spec.getInitialDelay())));
        } else if (spec.getTriggerType() == TriggerSpec.TriggerType.ONE_TIME) {
            this.scheduledTasks.add(taskRegistrar.scheduleOneTimeTask(
                    new OneTimeTask(runnable, spec.getInitialDelay())));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "invalidTriggerType", "request", spec));
        }
        log.info("Added {}", spec);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> cancelTrigger(@PathVariable String name) {
//        List<ScheduledTask> tasks = List.of(taskRegistrar.getScheduledTasks(), this.scheduledTasks)
//                .stream()
//                .flatMap(Set::stream)
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
                    log.info("Cancelled trigger {}", t);
                });

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("")
    public ResponseEntity<?> cancelAllTrigger() {
//        List.of(taskRegistrar.getScheduledTasks(), this.scheduledTasks)
//                .stream()
//                .flatMap(Set::stream)
        scheduledTasks.stream()
                .filter(t -> !cancelledTasks.contains(t))
                .forEach(t -> {
                    t.cancel(false);
                    cancelledTasks.add(t);
                    log.info("Cancelled trigger {}", t);
                });

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshTriggers() throws NoSuchMethodException {
        // remove existing
        this.scheduledTasks.stream()
                .filter(st -> !cancelledTasks.contains(st))
                .forEach(st -> {
                    this.cancelTrigger(st.getTask().getRunnable().toString());
                });

        // add back entries from db
        tasksRepository.findAll()
                .stream()
                .forEach(spec -> {
                    try {
                        this.addTrigger(spec);
                    } catch (NoSuchMethodException e) {
                        log.error("Failed to add trigger {}", spec, e);
                    }
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

        List<TriggerSpec> persistedTriggers = tasksRepository.findAll();

        // check for annotated jobs that match database
        scheduledAnnotationBeanPostProcessor.getScheduledTasks()
                .forEach(st -> {
                    Runnable r = st.getTask().getRunnable();
                    if (r instanceof ScheduledMethodRunnable) {
                        ScheduledMethodRunnable smr = (ScheduledMethodRunnable) r;

                        String beanName = StringUtils.uncapitalize(smr.getTarget().getClass().getSimpleName());
                        String methodName = smr.getMethod().getName();

                        // if found in database, remove annotated trigger(s)
                        // NOTE: cleanest approach is to put all jobs in the database
                        if (persistedTriggers.stream().anyMatch(pt -> pt.getTargetBean().equals(beanName) && pt.getTargetMethod().equals(methodName))) {
                            log.info("Removed annotated trigger for {}.{} in favor of db trigger(s)", beanName, methodName);
                            scheduledAnnotationBeanPostProcessor.postProcessBeforeDestruction(smr.getTarget(), beanName);
                            if (taskRegistrar.getCronTaskList().stream().anyMatch(cronTask -> cronTask.getRunnable() == smr)) {
                                taskRegistrar.setCronTasksList(
                                        taskRegistrar.getCronTaskList().stream().filter(cronTask -> cronTask.getRunnable() != smr).collect(Collectors.toList())
                                );
                            }
                            if (taskRegistrar.getFixedDelayTaskList().stream().anyMatch(cronTask -> cronTask.getRunnable() == smr)) {
                                taskRegistrar.setFixedDelayTasksList(
                                        taskRegistrar.getFixedDelayTaskList().stream().filter(cronTask -> cronTask.getRunnable() != smr).collect(Collectors.toList())
                                );
                            }
                            if (taskRegistrar.getFixedRateTaskList().stream().anyMatch(cronTask -> cronTask.getRunnable() == smr)) {
                                taskRegistrar.setFixedRateTasksList(
                                        taskRegistrar.getFixedRateTaskList().stream().filter(cronTask -> cronTask.getRunnable() != smr).collect(Collectors.toList())
                                );
                            }
                        } else {
                            // not found in the database
                            // TODO: persist annotated trigger to database
                            log.info("Keeping annotated trigger for {}.{} (not in db)", beanName, methodName);
                            this.scheduledTasks.add(st);
                        }
                    }
                });

        // load triggers from database
        // NOTE: they will get scheduled before any annotated ones. the annotated will get scheduled
        // by ScheduledAnnotationBeanPostProcessor::finishRegistration (after this method is called)
        tasksRepository.findAll().stream()
                .filter(TriggerSpec::isEnabled)
                .forEach(spec -> {
                    try {
                        this.addTrigger(spec);
                    } catch (NoSuchMethodException e) {
                        log.error("Failed to add trigger {}", spec, e);
                    }
                });
    }
}
