package com.example.springdemo.jobs;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@RequestMapping("/jobs")
@RestController
@Slf4j
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class JobsController {

    private final SchedulerFactoryBean schedulerFactory;
    private final List<Job> availableJobs;

    public JobsController(SchedulerFactoryBean schedulerFactory, ObjectProvider<Job> jobsProvider) {
        log.info("Configuring JobsController");
        this.schedulerFactory = schedulerFactory;
        this.availableJobs = jobsProvider.stream().toList();
    }

    @GetMapping("/classes")
    public ResponseEntity getClasses() {
        return ok(availableJobs.stream()
                .map(j -> j.getClass().getName())
                .collect(Collectors.toList()));
    }

    @Transactional
    @PostMapping()
    public ResponseEntity saveJob(@RequestBody final JobSpec jobSpec) throws SchedulerException {
        Scheduler scheduler = schedulerFactory.getScheduler();
        JobKey jobKey = JobKey.jobKey(jobSpec.name, jobSpec.group);
        log.info("Saving {}", jobKey);

        JobDetail detail = JobBuilder.newJob()
                .ofType(jobSpec.jobClass)
                .storeDurably()
                .withIdentity(jobKey)
                .withDescription(jobSpec.desc)
                .usingJobData(new JobDataMap(jobSpec.jobData))
                .build();

        scheduler.addJob(detail, scheduler.checkExists(jobKey));

        return ok(detail.getKey());
    }

    @Transactional
    @PostMapping("{group}/{name}/triggers")
    public ResponseEntity saveTrigger(@PathVariable("group") final String jobGroup,
                                      @PathVariable("name") final String jobName,
                                      @RequestBody final TriggerSpec triggerSpec) throws SchedulerException {
        Scheduler scheduler = schedulerFactory.getScheduler();
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerSpec.name, triggerSpec.group);
        log.info("Saving trigger {} for job {}", triggerKey, jobKey);

        Trigger newTrigger = TriggerBuilder.newTrigger()
                .forJob(jobKey)
                .withIdentity(triggerKey)
                .withDescription(triggerSpec.desc)
                .usingJobData(new JobDataMap(triggerSpec.jobData))
                .withSchedule(cronSchedule(triggerSpec.cronSchedule)
                        .withMisfireHandlingInstructionDoNothing())
                .build();

        Trigger existingTrigger = scheduler.getTrigger(triggerKey);
        if (existingTrigger != null) {
            if (existingTrigger.getJobKey().equals(jobKey)) {
                scheduler.rescheduleJob(triggerKey, newTrigger);
            } else {
                log.warn("saveTrigger: Trigger {} not saved for job {} as it already exists for job {}", triggerKey, jobKey, existingTrigger.getJobKey());
                return status(HttpStatus.CONFLICT).body("Trigger already exists for Job " + existingTrigger.getJobKey());
            }
        } else {
            scheduler.scheduleJob(newTrigger);
        }

        return ok(newTrigger.getKey());
    }

    @Transactional
    @DeleteMapping("/triggers/{group}/{name}")
    public ResponseEntity saveTrigger(@PathVariable("group") final String triggerGroup,
                                      @PathVariable("name") final String triggerName) throws SchedulerException {
        Scheduler scheduler = schedulerFactory.getScheduler();
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);
        log.info("Removing trigger {}", triggerKey);

        Trigger existingTrigger = scheduler.getTrigger(triggerKey);
        if (existingTrigger != null) {
            scheduler.unscheduleJob(triggerKey);
        } else {
            return status(HttpStatus.NOT_FOUND).body("Trigger " + triggerKey + " not found");
        }
        return ok(triggerKey);
    }

    @Transactional
    @PostMapping("/pauseAll")
    public ResponseEntity pauseAllJobs() throws SchedulerException {
        log.info("Pausing all jobs");
        schedulerFactory.getScheduler().pauseAll();
        return noContent().build();
    }

    @Transactional
    @PostMapping("/resumeAll")
    public ResponseEntity resumeAllJobs() throws SchedulerException {
        log.info("Resuming all jobs");
        schedulerFactory.getScheduler().resumeAll();
        return noContent().build();
    }

    @Transactional
    @PostMapping("/{group}/{name}/pause")
    public ResponseEntity pauseJob(@PathVariable final String group, @PathVariable final String name) throws SchedulerException {
        log.info("Pausing {}/{}", group, name);
        schedulerFactory.getScheduler().pauseJob(JobKey.jobKey(name, group));
        return noContent().build();
    }

    @Transactional
    @PostMapping("/{group}/{name}/resume")
    public ResponseEntity resumeJob(@PathVariable final String group, @PathVariable final String name) throws SchedulerException {
        log.info("Resuming {}/{}", group, name);
        schedulerFactory.getScheduler().resumeJob(JobKey.jobKey(name, group));
        return noContent().build();
    }

    public static class JobSpec {
        public String name;
        public String group;
        public String desc;
        public Class<Job> jobClass;
        public Map jobData;
    }

    public static class TriggerSpec {
        public String name;
        public String group;
        public String desc;
        public String cronSchedule;
        public Map jobData;
    }
}
