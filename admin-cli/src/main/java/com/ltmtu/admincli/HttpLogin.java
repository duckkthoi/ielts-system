package com.ltmtu.admincli;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** Lấy access token qua {@code POST /api/auth/login} (docs/05 mục A.2) — token dùng cho lệnh {@code AUTH}. */
final class HttpLogin {

	private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

	private HttpLogin() {
	}

	/**
	 * @param insecure tin mọi chứng chỉ (chỉ để thử với chứng chỉ tự ký của nginx khi dev — docs bước 13). Tên máy
	 *                 vẫn được kiểm tra theo chứng chỉ.
	 */
	static String accessToken(String apiBase, String username, char[] password, boolean insecure) throws IOException {
		String body = "{\"username\":\"" + escape(username) + "\",\"password\":\"" + escape(new String(password)) + "\"}";
		HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase.replaceAll("/+$", "") + "/api/auth/login"))
				.timeout(Duration.ofSeconds(15))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> response;
		HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));
		if (insecure) {
			builder.sslContext(trustAll());
		}
		try (HttpClient client = builder.build()) {
			response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Bị huỷ khi đang đăng nhập", e);
		}
		Matcher token = ACCESS_TOKEN.matcher(response.body());
		if (response.statusCode() == 200 && token.find()) {
			return token.group(1);
		}
		Matcher message = MESSAGE.matcher(response.body());
		throw new IOException("Đăng nhập thất bại (HTTP " + response.statusCode() + ")"
				+ (message.find() ? ": " + message.group(1) : ""));
	}

	static String escape(String s) {
		StringBuilder sb = new StringBuilder();
		for (char c : s.toCharArray()) {
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				default -> {
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
				}
			}
		}
		return sb.toString();
	}

	private static SSLContext trustAll() throws IOException {
		TrustManager[] trustAll = { new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		} };
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, trustAll, new SecureRandom());
			return ctx;
		} catch (GeneralSecurityException e) {
			throw new IOException("Không tạo được kết nối TLS", e);
		}
	}
}
