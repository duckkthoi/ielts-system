package com.ltmtu.ielts.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Bean thay thế cho mọi integration test: đồng hồ điều khiển được và bộ gửi mail giữ email trong bộ nhớ. */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfig {

	@Bean
	@Primary
	MutableClock testClock() {
		return new MutableClock();
	}

	@Bean
	@Primary
	CapturingMailSender capturingMailSender() {
		return new CapturingMailSender();
	}
}
