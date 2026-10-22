package com.ltmtu.ielts.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật @Async (gửi email nền) và @Scheduled (job nhắc hạn nộp). */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
