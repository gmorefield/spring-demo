package com.example.springdemo.jobs;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j

public class DumpContextJob extends QuartzJobBean {
    @Override
    public void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("{}: instance={} context={}", context.getMergedJobDataMap().getString("name"),
                this.toString(), context);
    }
}
