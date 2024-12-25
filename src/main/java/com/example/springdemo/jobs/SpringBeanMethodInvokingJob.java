package com.example.springdemo.jobs;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.util.MethodInvoker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class SpringBeanMethodInvokingJob extends QuartzJobBean {
    private static final ConcurrentMap<String, MethodInvoker> invokerMap = new ConcurrentHashMap<>();

    private final ApplicationContext applicationContext;

    public SpringBeanMethodInvokingJob(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        try {
            getInvoker(applicationContext, data.getString("targetBean"), data.getString("targetMethod"))
                    .invoke();
        } catch (Exception e) {
            throw new JobExecutionException("Failed to execute job with targetBean=%,targetMethod=%"
                    .formatted(data.getString("targetBean"), data.getString("targetMethod")), e);
        }
    }

    private static MethodInvoker getInvoker(final ApplicationContext appContext, final String targetBean, final String targetMethod) throws ClassNotFoundException, NoSuchMethodException {
        String key = targetBean + "." + targetMethod;
        MethodInvoker invoker = invokerMap.get(key);
        if (invoker != null) {
            return invoker;
        }

        final MethodInvoker newInvoker = new MethodInvoker();
        newInvoker.setTargetObject(appContext.getBean(targetBean));
        newInvoker.setTargetMethod(targetMethod);
        newInvoker.prepare();

        return invokerMap.computeIfAbsent(key, k -> newInvoker);
    }
}
