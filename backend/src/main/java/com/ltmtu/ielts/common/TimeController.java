package com.ltmtu.ielts.common;

import java.time.Clock;
import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** Giờ server, dùng để client đồng bộ đồng hồ đếm ngược (SD-11). Không cần đăng nhập. */
@RestController
@RequiredArgsConstructor
public class TimeController {

	private final Clock clock;

	@GetMapping("/api/time")
	public ApiResponse<ServerTime> time() {
		return ApiResponse.ok(new ServerTime(clock.instant()));
	}

	public record ServerTime(Instant serverTime) {
	}
}
