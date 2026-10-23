package com.ltmtu.ielts.tcpgateway;

import java.util.Locale;

/**
 * Lệnh từ client (docs/05 mục C.2) và bộ phân tích một dòng lệnh. Class thuần, không phụ thuộc Spring hay socket.
 * Từ khoá lệnh không phân biệt hoa thường; các phần cách nhau bằng một hoặc nhiều dấu cách.
 */
public sealed interface TcpCommand {

	record Auth(String token) implements TcpCommand {
	}

	record Import(String classCode, int byteLength, boolean sendEmail) implements TcpCommand {
	}

	record Ping() implements TcpCommand {
	}

	record Quit() implements TcpCommand {
	}

	/** Phân tích một dòng (đã bỏ {@code \r\n}). Sai cú pháp → {@link TcpException} mã 400. */
	static TcpCommand parse(String line) {
		String[] parts = line.trim().split(" +");
		String keyword = parts[0].toUpperCase(Locale.ROOT);
		return switch (keyword) {
			case "AUTH" -> {
				expect(parts.length == 2, "Cú pháp: AUTH <accessToken>");
				yield new Auth(parts[1]);
			}
			case "IMPORT" -> parseImport(parts);
			case "PING" -> {
				expect(parts.length == 1, "Cú pháp: PING");
				yield new Ping();
			}
			case "QUIT" -> new Quit();
			case "" -> throw new TcpException(400, "Dòng lệnh trống");
			default -> throw new TcpException(400, "Lệnh không hợp lệ: " + keyword);
		};
	}

	private static Import parseImport(String[] parts) {
		String usage = "Cú pháp: IMPORT <classCode> <byteLength> [EMAIL]";
		expect(parts.length == 3 || parts.length == 4, usage);
		boolean email = false;
		if (parts.length == 4) {
			expect("EMAIL".equalsIgnoreCase(parts[3]), usage);
			email = true;
		}
		int length;
		try {
			length = Integer.parseInt(parts[2]);
		} catch (NumberFormatException e) {
			throw new TcpException(400, "byteLength phải là số nguyên dương");
		}
		expect(length > 0, "byteLength phải là số nguyên dương");
		return new Import(parts[1], length, email);
	}

	private static void expect(boolean ok, String message) {
		if (!ok) {
			throw new TcpException(400, message);
		}
	}
}
