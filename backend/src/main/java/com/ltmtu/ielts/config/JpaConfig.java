package com.ltmtu.ielts.config;

import java.time.Clock;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * {@code created_at} / {@code updated_at} của {@code BaseEntity} lấy giờ qua bean {@link Clock} (giống mọi chỗ
 * khác), để test với {@code MutableClock} thấy đúng giờ và giờ chỉ có độ chính xác micro giây như PostgreSQL.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

	@Bean
	DateTimeProvider auditingDateTimeProvider(Clock clock) {
		return () -> Optional.of(clock.instant());
	}
}
