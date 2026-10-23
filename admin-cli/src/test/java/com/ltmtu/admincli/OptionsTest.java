package com.ltmtu.admincli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tham số dòng lệnh và các phần nhỏ của {@link AdminCli}. */
class OptionsTest {

	@Test
	@DisplayName("Đọc lệnh, tuỳ chọn có giá trị, cờ; giá trị mặc định")
	void parse() {
		Options o = Options.parse(new String[] { "import", "--class", "K15-A", "--file", "a.csv", "--email", "--port", "9091" });
		assertEquals("import", o.command());
		assertEquals("K15-A", o.require("class"));
		assertTrue(o.flag("email"));
		assertEquals(9091, o.getInt("port", 9090));
		assertEquals("localhost", o.get("host", "localhost"));
	}

	@Test
	@DisplayName("Tuỳ chọn lạ, thiếu giá trị, số sai → lỗi")
	void invalid() {
		assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] { "import", "--abc", "1" }));
		assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] { "import", "--class" }));
		assertThrows(IllegalArgumentException.class, () -> Options.parse(new String[] { "import", "class" }));
		assertThrows(IllegalArgumentException.class,
				() -> Options.parse(new String[] { "ping", "--port", "abc" }).getInt("port", 1));
	}

	@Test
	@DisplayName("Sai tham số → mã thoát 2 và in hướng dẫn; không tham số → in hướng dẫn, mã 0")
	void exitCodes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		AdminCli cli = new AdminCli(new PrintStream(out, true, StandardCharsets.UTF_8),
				new PrintStream(err, true, StandardCharsets.UTF_8));
		assertEquals(2, cli.run(new String[] { "import", "--file", "a.xlsx", "--token", "t", "--class", "K" }));
		assertTrue(err.toString(StandardCharsets.UTF_8).contains("chỉ hỗ trợ file .csv"));
		assertEquals(2, cli.run(new String[] { "xoa" }));
		assertEquals(0, cli.run(new String[0]));
		assertTrue(out.toString(StandardCharsets.UTF_8).contains("Cách dùng"));
	}

	@Test
	@DisplayName("File tài khoản: ô có dấu phẩy / ngoặc kép được bao ngoặc kép")
	void csvField() {
		assertEquals("Nguyễn Văn A", AdminCli.CredentialsCsv.field("Nguyễn Văn A"));
		assertEquals("\"A, B\"", AdminCli.CredentialsCsv.field("A, B"));
		assertEquals("\"x\"\"y\"", AdminCli.CredentialsCsv.field("x\"y"));
	}
}
