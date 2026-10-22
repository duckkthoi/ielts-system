package com.ltmtu.ielts.config;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nguồn thời gian duy nhất của ứng dụng. Code nghiệp vụ luôn dùng {@code Instant.now(clock)}
 * để test được hạn nộp / nộp muộn / nhắc hạn bằng {@code Clock.fixed(...)}.
 * <p>
 * Đồng hồ "nhảy" theo <b>micro giây</b> vì PostgreSQL {@code TIMESTAMPTZ} chỉ lưu tới micro giây: nếu giữ nano giây,
 * giá trị trả về ngay sau khi lưu sẽ khác giá trị đọc lại từ DB (vd …724673700Z so với …724674Z).
 */
@Configuration
public class TimeConfig {

	@Bean
	Clock clock() {
		return Clock.tick(Clock.systemUTC(), Duration.of(1, ChronoUnit.MICROS));
	}
}
