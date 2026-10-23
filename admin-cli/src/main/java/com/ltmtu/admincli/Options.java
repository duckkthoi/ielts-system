package com.ltmtu.admincli;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Tham số dòng lệnh: {@code <lệnh> --tên giá-trị … --cờ}. */
public final class Options {

	/** Tuỳ chọn dạng cờ (không có giá trị). */
	private static final Set<String> FLAGS = Set.of("email", "help", "insecure");

	private static final Set<String> KNOWN = Set.of("host", "port", "api", "user", "password", "token", "class",
			"file", "out", "email", "help", "timeout", "insecure");

	private final String command;
	private final Map<String, String> values;

	private Options(String command, Map<String, String> values) {
		this.command = command;
		this.values = values;
	}

	/** Sai cú pháp → {@link IllegalArgumentException} với lời nhắn tiếng Việt. */
	public static Options parse(String[] args) {
		if (args.length == 0) {
			return new Options("help", Map.of());
		}
		Map<String, String> values = new HashMap<>();
		for (int i = 1; i < args.length; i++) {
			String a = args[i];
			if (!a.startsWith("--") || a.length() == 2) {
				throw new IllegalArgumentException("Tham số không hợp lệ: " + a);
			}
			String name = a.substring(2);
			if (!KNOWN.contains(name)) {
				throw new IllegalArgumentException("Không có tuỳ chọn --" + name);
			}
			if (FLAGS.contains(name)) {
				values.put(name, "true");
			} else {
				if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
					throw new IllegalArgumentException("Thiếu giá trị cho --" + name);
				}
				values.put(name, args[++i]);
			}
		}
		return new Options(args[0], values);
	}

	public String command() {
		return command;
	}

	public String get(String name) {
		return values.get(name);
	}

	public String get(String name, String fallback) {
		return values.getOrDefault(name, fallback);
	}

	public String require(String name) {
		String v = values.get(name);
		if (v == null || v.isBlank()) {
			throw new IllegalArgumentException("Thiếu --" + name);
		}
		return v;
	}

	public int getInt(String name, int fallback) {
		String v = values.get(name);
		if (v == null) {
			return fallback;
		}
		try {
			return Integer.parseInt(v);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("--" + name + " phải là số");
		}
	}

	public boolean flag(String name) {
		return values.containsKey(name);
	}
}
