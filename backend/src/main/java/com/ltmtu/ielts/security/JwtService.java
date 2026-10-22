package com.ltmtu.ielts.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.user.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Cấp và kiểm tra access token (JWT HS256).
 * Claim: {@code sub} = id người dùng, {@code usr} = tên đăng nhập, {@code role}, {@code sid} = id phiên.
 */
@Service
public class JwtService {

	private static final int MIN_SECRET_BYTES = 32;

	private final SecretKey key;
	private final JwtParser parser;
	private final AppProperties.Jwt cfg;
	private final Clock clock;

	public JwtService(AppProperties props, Clock clock) {
		this.cfg = props.jwt();
		this.clock = clock;
		byte[] secret = cfg.secret().getBytes(StandardCharsets.UTF_8);
		if (secret.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("app.jwt.secret phải dài ít nhất " + MIN_SECRET_BYTES + " byte");
		}
		this.key = Keys.hmacShaKeyFor(secret);
		this.parser = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build();
	}

	public IssuedToken issueAccessToken(AuthPrincipal principal) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(cfg.accessTtl());
		String token = Jwts.builder()
				.subject(String.valueOf(principal.id()))
				.claim("usr", principal.username())
				.claim("role", principal.role().name())
				.claim("sid", principal.sessionId())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(key)
				.compact();
		return new IssuedToken(token, expiresAt);
	}

	/** Đọc token; sai chữ ký, hết hạn hoặc sai định dạng → {@code Optional.empty()}. */
	public Optional<AuthPrincipal> parse(String token) {
		try {
			Claims c = parser.parseSignedClaims(token).getPayload();
			return Optional.of(new AuthPrincipal(
					Long.valueOf(c.getSubject()),
					c.get("usr", String.class),
					Role.valueOf(c.get("role", String.class)),
					c.get("sid", Long.class)));
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	public record IssuedToken(String token, Instant expiresAt) {
	}
}
