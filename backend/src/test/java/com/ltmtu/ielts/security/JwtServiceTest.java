package com.ltmtu.ielts.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.support.MutableClock;
import com.ltmtu.ielts.user.Role;

class JwtServiceTest {

	private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

	private final MutableClock clock = new MutableClock();
	private final JwtService jwt = new JwtService(props(SECRET), clock);

	private static AppProperties props(String secret) {
		return new AppProperties(
				new AppProperties.Jwt(secret, Duration.ofMinutes(30), Duration.ofDays(7), Duration.ofSeconds(30)),
				null, new AppProperties.Cors(List.of()), null, null, null, ZoneId.of("Asia/Ho_Chi_Minh"), null, null, null, null, null, null);
	}

	@Test
	@DisplayName("Cấp rồi đọc lại access token giữ nguyên id, tên, vai trò, mã phiên")
	void issueThenParse_roundTrip() {
		var principal = new AuthPrincipal(7L, "gv01", Role.TEACHER, 42L);
		var issued = jwt.issueAccessToken(principal);

		assertThat(issued.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(30)));
		assertThat(jwt.parse(issued.token())).contains(principal);
	}

	@Test
	@DisplayName("NFR-07: access token hết hạn sau 30 phút")
	void expiredToken_isRejected() {
		var issued = jwt.issueAccessToken(new AuthPrincipal(1L, "a", Role.ADMIN, 1L));
		clock.advance(Duration.ofMinutes(31));

		assertThat(jwt.parse(issued.token())).isEmpty();
	}

	@Test
	@DisplayName("Token bị sửa hoặc ký bằng khoá khác bị từ chối")
	void tamperedOrForeignToken_isRejected() {
		String token = jwt.issueAccessToken(new AuthPrincipal(1L, "a", Role.STUDENT, 1L)).token();
		String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
		String foreign = new JwtService(props(SECRET + "-khac"), clock)
				.issueAccessToken(new AuthPrincipal(1L, "a", Role.ADMIN, 1L)).token();

		assertThat(jwt.parse(tampered)).isEmpty();
		assertThat(jwt.parse(foreign)).isEmpty();
		assertThat(jwt.parse("khong-phai-jwt")).isEmpty();
	}

	@Test
	@DisplayName("Khoá bí mật ngắn hơn 32 byte → không cho khởi động")
	void shortSecret_failsFast() {
		assertThatThrownBy(() -> new JwtService(props("ngan-qua"), clock))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("Refresh token ngẫu nhiên, hash SHA-256 dạng hex 64 ký tự")
	void refreshTokenAndHash() {
		String a = TokenHasher.newRefreshToken();
		String b = TokenHasher.newRefreshToken();

		assertThat(a).isNotEqualTo(b).hasSize(43);
		assertThat(TokenHasher.sha256(a)).hasSize(64).isEqualTo(TokenHasher.sha256(a)).isNotEqualTo(TokenHasher.sha256(b));
	}
}
