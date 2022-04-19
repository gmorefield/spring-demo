package com.example.springdemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {"spring.application.admin.enabled=false"})
@ActiveProfiles("test")
class SpringDemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
