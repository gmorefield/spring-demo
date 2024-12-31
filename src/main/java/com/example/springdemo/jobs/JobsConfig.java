package com.example.springdemo.jobs;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

@Configuration
@Slf4j
public class JobsConfig {

    @Bean
    public JobDetail pingJobDetail() {
        return JobBuilder.newJob().ofType(DumpContextJob.class)
                .usingJobData("name", "ping")
                .storeDurably()
                .withIdentity(JobKey.jobKey("pingJob", "sample"))
                .withDescription("Ping job service")
                .build();
    }

    @Bean
    public JobDetail pongJobDetail() {
        return JobBuilder.newJob().ofType(DumpContextJob.class)
                .usingJobData("name", "pong")
                .storeDurably()
                .withIdentity(JobKey.jobKey("pongJob", "sample"))
                .withDescription("Pong job service")
                .build();
    }

    @Bean
    public Trigger pingJobRepeatingTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(JobKey.jobKey("pingJob", "sample"))
                .withIdentity("pingJob.Repeating", "sample").withDescription("Sample simple trigger")
                .withSchedule(simpleSchedule().repeatForever().withIntervalInMinutes(5))
                .build();
    }

    @Bean
    public Trigger pongJobRepeatingTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(JobKey.jobKey("pongJob", "sample"))
                .withIdentity("pongJob.Repeating", "sample").withDescription("Sample cron trigger")
                .withSchedule(cronSchedule("0 0/5 * * * ?"))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.sql.init.platform", havingValue = "mssql")
    public JobDetail queueResetJob() {
        return JobBuilder.newJob()
                .ofType(SpringBeanMethodInvokingJob.class)
                .storeDurably()
                .withIdentity("queueResetJob", "queue")
                .usingJobData("targetBean", "queueService")
                .usingJobData("targetMethod", "resetErrors")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.sql.init.platform", havingValue = "mssql")
    public Trigger queueResetTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob("queueResetJob", "queue")
                .withSchedule(simpleSchedule()
                        .withIntervalInMinutes(5)
                        .repeatForever()
                        .withMisfireHandlingInstructionIgnoreMisfires()
                )
                .withIdentity("queueResetJob.periodic", "queue")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.sql.init.platform", havingValue = "mssql")
    public JobDetail orderedQueueResetJob() {
        return JobBuilder.newJob()
                .ofType(SpringBeanMethodInvokingJob.class)
                .storeDurably()
                .withIdentity("orderedQueueResetJob", "queue")
                .usingJobData("targetBean", "queueService")
                .usingJobData("targetMethod", "orderResetErrors")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.sql.init.platform", havingValue = "mssql")
    public Trigger orderedQueueResetTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob("orderedQueueResetJob", "queue")
                .withSchedule(simpleSchedule()
                        .withIntervalInMinutes(5)
                        .repeatForever()
                        .withMisfireHandlingInstructionIgnoreMisfires()
                )
                .withIdentity("orderedQueueResetJob.periodic", "queue")
                .build();
    }
}
