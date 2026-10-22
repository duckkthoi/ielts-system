package com.ltmtu.ielts.config;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ltmtu.ielts.common.FileStorageService;

import lombok.RequiredArgsConstructor;

/**
 * Phục vụ ảnh đã upload tại {@code /uploads/**}. Công khai (thẻ &lt;img&gt; không gửi được header token), nhưng tên
 * file là UUID ngẫu nhiên nên không đoán được. Tên file không bao giờ đổi nên cache lâu.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final FileStorageService storage;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler(FileStorageService.URL_PREFIX + "**")
				.addResourceLocations(storage.root().toUri().toString())
				.setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
	}
}
