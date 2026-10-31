package com.ltmtu.ielts.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** UC-08: hiển thị phiên an toàn — IP che, user-agent rút gọn, trạng thái. */
class SessionRulesTest {

	private static final Instant NOW = Instant.parse("2026-10-31T03:00:00Z");

	@Test
	@DisplayName("UC-08: IPv4 che octet cuối, IPv6 giữ 3 nhóm đầu, rỗng → null")
	void maskIp() {
		assertThat(SessionRules.maskIp("192.168.1.25")).isEqualTo("192.168.1.*");
		assertThat(SessionRules.maskIp("2001:db8:85a3:0:0:8a2e:370:7334")).isEqualTo("2001:db8:85a3:*");
		assertThat(SessionRules.maskIp("::1")).isEqualTo("*");
		assertThat(SessionRules.maskIp(" ")).isNull();
	}

	@Test
	@DisplayName("UC-08: user-agent rút gọn thành trình duyệt · hệ điều hành")
	void shortUserAgent() {
		assertThat(SessionRules.shortUserAgent(
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"))
				.isEqualTo("Chrome · Windows");
		assertThat(SessionRules.shortUserAgent(
				"Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/130.0 Safari/537.36 Edg/130.0"))
				.isEqualTo("Edge · Windows");
		assertThat(SessionRules.shortUserAgent(
				"Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Safari/604.1"))
				.isEqualTo("Safari · iOS");
		assertThat(SessionRules.shortUserAgent("Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0"))
				.isEqualTo("Firefox · Linux");
		assertThat(SessionRules.shortUserAgent(null)).isEqualTo("Không rõ");
		assertThat(SessionRules.shortUserAgent("abc")).isEqualTo("Khác");
	}

	@Test
	@DisplayName("UC-08: trạng thái — thu hồi ưu tiên hơn hết hạn")
	void status() {
		assertThat(SessionRules.status(null, NOW.plusSeconds(1), NOW)).isEqualTo(SessionRules.Status.ACTIVE);
		assertThat(SessionRules.status(null, NOW, NOW)).isEqualTo(SessionRules.Status.EXPIRED);
		assertThat(SessionRules.status(NOW.minusSeconds(5), NOW.minusSeconds(1), NOW))
				.isEqualTo(SessionRules.Status.REVOKED);
	}
}
