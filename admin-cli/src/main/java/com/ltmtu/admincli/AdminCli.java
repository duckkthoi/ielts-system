package com.ltmtu.admincli;

import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import com.ltmtu.admincli.ImportClient.Credential;
import com.ltmtu.admincli.ImportClient.Result;
import com.ltmtu.admincli.ImportClient.RowError;
import com.ltmtu.admincli.ImportClient.ServerError;

/**
 * Công cụ dòng lệnh import danh sách sinh viên qua server TCP của hệ thống (docs/05 phần C).
 *
 * <pre>
 * java -jar admin-cli.jar import --class K15-A --file danh-sach.csv --user gv.hanh [--email]
 * </pre>
 *
 * Mã thoát: 0 = thành công, 1 = lỗi (kết nối / server trả ERR), 2 = sai tham số.
 */
public final class AdminCli {

	static final String USAGE = """
			Cách dùng:
			  java -jar admin-cli.jar import --class <mã lớp> --file <danh-sách.csv> [--email] [--out <file tài khoản.csv>]
			  java -jar admin-cli.jar ping
			Xác thực (chọn một):
			  --user <tên đăng nhập> [--password <mật khẩu>]   đăng nhập qua HTTP để lấy token (bỏ --password thì nhập ẩn)
			  --token <access token>                          dùng token có sẵn
			Kết nối:
			  --host <máy chủ> (localhost)  --port <cổng TCP> (9090)  --api <địa chỉ HTTP> (http://localhost:8080)
			  --timeout <giây chờ phản hồi> (60)
			  --insecure: chấp nhận chứng chỉ HTTPS tự ký khi đăng nhập (chạy sau nginx trong docker compose)
			File CSV: dòng đầu là tiêu đề, cần cột ma_sv, ho_ten (email tuỳ chọn), UTF-8.
			--email: gửi email tài khoản cho sinh viên mới có email.
			Tài khoản mới (kèm mật khẩu tạm) được lưu ra --out, mặc định tai-khoan-<mã lớp>.csv.""";

	private final PrintStream out;
	private final PrintStream err;

	AdminCli(PrintStream out, PrintStream err) {
		this.out = out;
		this.err = err;
	}

	public static void main(String[] args) {
		PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
		PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
		System.exit(new AdminCli(out, err).run(args));
	}

	int run(String[] args) {
		Options opts;
		try {
			opts = Options.parse(args);
		} catch (IllegalArgumentException e) {
			err.println(e.getMessage());
			err.println(USAGE);
			return 2;
		}
		try {
			return switch (opts.command()) {
				case "import" -> importCommand(opts);
				case "ping" -> pingCommand(opts);
				case "help", "--help" -> {
					out.println(USAGE);
					yield 0;
				}
				default -> throw new IllegalArgumentException("Không có lệnh \"" + opts.command() + "\"");
			};
		} catch (IllegalArgumentException e) {
			err.println(e.getMessage());
			err.println(USAGE);
			return 2;
		} catch (ServerError e) {
			err.println("Server từ chối (" + e.code() + "): " + e.getMessage());
			return 1;
		} catch (IOException e) {
			err.println("Lỗi: " + e.getMessage());
			return 1;
		}
	}

	private int pingCommand(Options opts) throws IOException {
		String token = token(opts);
		try (ImportClient client = connect(opts)) {
			client.auth(token);
			long start = System.nanoTime();
			client.ping();
			long micros = (System.nanoTime() - start) / 1000;
			client.quit();
			out.printf(Locale.ROOT, "PONG từ %s:%d sau %.1f ms%n", opts.get("host", "localhost"), port(opts),
					micros / 1000.0);
		}
		return 0;
	}

	private int importCommand(Options opts) throws IOException {
		String classCode = opts.require("class");
		Path file = Path.of(opts.require("file"));
		if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
			throw new IllegalArgumentException("Qua TCP chỉ hỗ trợ file .csv (UTF-8). File Excel hãy dùng giao diện web");
		}
		if (!Files.isRegularFile(file)) {
			throw new IllegalArgumentException("Không thấy file " + file);
		}
		byte[] csv = Files.readAllBytes(file);
		String token = token(opts);

		Result result;
		try (ImportClient client = connect(opts)) {
			client.auth(token);
			out.println("Đã xác thực. Gửi " + csv.length + " byte tới lớp " + classCode + "…");
			ProgressBar bar = new ProgressBar(out);
			result = client.importCsv(classCode, csv, opts.flag("email"), bar::update);
			bar.finish();
		}

		out.printf("Xong: tạo mới %d, đã có tài khoản %d, đã trong lớp %d, lỗi %d%n", result.created(),
				result.existing(), result.skipped(), result.errors().size());
		for (RowError e : result.errors()) {
			out.println("  Dòng " + e.row() + ": " + e.reason());
		}
		if (!result.credentials().isEmpty()) {
			Path target = Path.of(opts.get("out", "tai-khoan-" + classCode.toLowerCase(Locale.ROOT) + ".csv"));
			CredentialsCsv.write(target, result.credentials());
			out.println("Đã lưu " + result.credentials().size() + " tài khoản mới vào " + target.toAbsolutePath());
			out.println("Lưu ý: file chứa mật khẩu tạm — giữ kín và xoá sau khi đã phát cho sinh viên.");
		}
		return 0;
	}

	private ImportClient connect(Options opts) throws IOException {
		return ImportClient.connect(opts.get("host", "localhost"), port(opts), opts.getInt("timeout", 60) * 1000);
	}

	private static int port(Options opts) {
		return opts.getInt("port", 9090);
	}

	private String token(Options opts) throws IOException {
		String token = opts.get("token");
		if (token != null) {
			return token;
		}
		String user = opts.get("user");
		if (user == null) {
			throw new IllegalArgumentException("Cần --user hoặc --token");
		}
		char[] password;
		if (opts.get("password") != null) {
			password = opts.get("password").toCharArray();
		} else {
			Console console = System.console();
			if (console == null) {
				throw new IllegalArgumentException("Không nhập mật khẩu ẩn được ở đây, hãy dùng --password");
			}
			password = console.readPassword("Mật khẩu của %s: ", user);
		}
		if (opts.flag("insecure")) {
			err.println("Cảnh báo: --insecure bỏ qua kiểm tra chứng chỉ HTTPS — chỉ dùng với chứng chỉ tự ký khi thử nghiệm.");
		}
		return HttpLogin.accessToken(opts.get("api", "http://localhost:8080"), user, password, opts.flag("insecure"));
	}

	/** Thanh tiến trình một dòng, vẽ lại theo mỗi {@code PROGRESS n/total}. */
	static final class ProgressBar {

		private static final int WIDTH = 30;
		private final PrintStream out;
		private boolean drawn;

		ProgressBar(PrintStream out) {
			this.out = out;
		}

		void update(int done, int total) {
			int filled = total == 0 ? WIDTH : done * WIDTH / total;
			out.print("\r[" + "#".repeat(filled) + "-".repeat(WIDTH - filled) + "] " + done + "/" + total);
			out.flush();
			drawn = true;
		}

		void finish() {
			if (drawn) {
				out.println();
			}
		}
	}

	/** Ghi file tài khoản: UTF-8 có BOM để Excel mở đúng tiếng Việt. */
	static final class CredentialsCsv {

		private CredentialsCsv() {
		}

		static void write(Path target, List<Credential> credentials) throws IOException {
			StringBuilder sb = new StringBuilder("﻿ma_sv,ho_ten,mat_khau_tam\r\n");
			for (Credential c : credentials) {
				sb.append(field(c.studentCode())).append(',').append(field(c.fullName())).append(',')
						.append(field(c.temporaryPassword())).append("\r\n");
			}
			Files.writeString(target, sb.toString(), StandardCharsets.UTF_8);
		}

		static String field(String value) {
			if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
				return "\"" + value.replace("\"", "\"\"") + "\"";
			}
			return value;
		}
	}
}
