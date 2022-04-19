package com.example.springdemo;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringDemoApplication implements CommandLineRunner {
	private static Logger logger = LoggerFactory.getLogger(SpringDemoApplication.class);

	@Value("${sample.commandline.delay:5}")
	private int delay;

	public static void main(String[] args) {
		SpringApplication.run(SpringDemoApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		logger.info("CommandLine: starting");
		TimeUnit.SECONDS.sleep(delay);
		logger.info("CommandLine: finished");
	}
}
