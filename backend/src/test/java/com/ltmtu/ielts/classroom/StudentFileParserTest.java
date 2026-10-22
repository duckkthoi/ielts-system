package com.ltmtu.ielts.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ltmtu.ielts.classroom.StudentFileParser.RawRow;
import com.ltmtu.ielts.common.ApiException;

class StudentFileParserTest {

	private static List<RawRow> csv(String content) throws Exception {
		return StudentFileParser.parse("ds.csv", new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	@DisplayName("Tên cột không phân biệt hoa thường / dấu / khoảng trắng")
	void headerAliases() {
		for (String h : List.of("ma_sv", "MSSV", "Mã SV", "Mã sinh viên", " mã  sv ")) {
			assertThat(StudentFileParser.normalizeHeader(h)).as(h).isIn("masv", "mssv", "masinhvien");
		}
		assertThat(StudentFileParser.normalizeHeader("Họ và tên")).isEqualTo("hovaten");
		assertThat(StudentFileParser.normalizeHeader("Họ_tên")).isEqualTo("hoten");
	}

	@Test
	@DisplayName("CSV: có BOM, phân cách bằng dấu chấm phẩy, thứ tự cột tuỳ ý, bỏ dòng trống, số dòng tính như Excel")
	void csv_bomSemicolonAndLineNumbers() throws Exception {
		List<RawRow> rows = csv("﻿Email;Họ và tên;MSSV\n"
				+ "an@sv.edu.vn;Nguyễn Văn An;2151000001\n"
				+ "\n"
				+ ";Trần Thị Bình;2151000002\n");

		assertThat(rows).containsExactly(
				new RawRow(2, "2151000001", "Nguyễn Văn An", "an@sv.edu.vn"),
				new RawRow(4, "2151000002", "Trần Thị Bình", ""));
	}

	@Test
	@DisplayName("CSV: giá trị có dấu phẩy trong ngoặc kép")
	void csv_quoted() throws Exception {
		List<RawRow> rows = csv("ma_sv,ho_ten\n2151000003,\"Lê, Hoàng Cường\"\n");
		assertThat(rows.getFirst().fullName()).isEqualTo("Lê, Hoàng Cường");
	}

	@Test
	@DisplayName("XLSX: mã SV dạng số được đọc đúng (không thành 2.15E9)")
	void xlsx_numericCodes() throws Exception {
		byte[] file = xlsx(List.of(
				Map.of(0, "ma_sv", 1, "ho_ten", 2, "email"),
				Map.of(0, 2151000001d, 1, "Phạm Thu Dung", 2, "dung@sv.edu.vn")));
		List<RawRow> rows = StudentFileParser.parse("DS.XLSX", new ByteArrayInputStream(file));

		assertThat(rows).containsExactly(new RawRow(2, "2151000001", "Phạm Thu Dung", "dung@sv.edu.vn"));
	}

	@Test
	@DisplayName("Lỗi file: thiếu cột bắt buộc, sai đuôi, file Excel hỏng, chỉ có tiêu đề")
	void badFiles() {
		assertBadFile(() -> csv("ma_sv,email\n2151000001,a@b.c\n"), "thiếu cột");
		assertBadFile(() -> StudentFileParser.parse("ds.pdf", new ByteArrayInputStream(new byte[] { 1 })), ".xlsx");
		assertBadFile(() -> StudentFileParser.parse("ds.xlsx",
				new ByteArrayInputStream("không phải excel".getBytes(StandardCharsets.UTF_8))), "Excel");
		assertBadFile(() -> csv("ma_sv,ho_ten\n"), "không có dòng");
	}

	@Test
	@DisplayName("Quá " + StudentFileParser.MAX_ROWS + " dòng → từ chối cả file")
	void tooManyRows() {
		StringBuilder sb = new StringBuilder("ma_sv,ho_ten\n");
		for (int i = 0; i <= StudentFileParser.MAX_ROWS; i++) {
			sb.append("SV").append(i).append(",Ten ").append(i).append('\n');
		}
		assertBadFile(() -> csv(sb.toString()), "Tối đa");
	}

	@Test
	@DisplayName("Kiểm tra dòng: mã sai định dạng, thiếu tên, email sai, trùng mã trong file (không phân biệt hoa thường)")
	void validateRow() {
		var seen = new java.util.HashMap<String, Integer>();
		assertThat(StudentImportService.validate(new RawRow(2, "sv001a", "Võ Minh Đức", ""), seen)).isNull();
		assertThat(StudentImportService.validate(new RawRow(3, "SV001A", "Khác", ""), seen)).contains("dòng 2");
		assertThat(StudentImportService.validate(new RawRow(4, "21A!", "X Y", ""), seen)).contains("không hợp lệ");
		assertThat(StudentImportService.validate(new RawRow(5, "2151000009", "", ""), seen)).isEqualTo("Thiếu họ tên");
		assertThat(StudentImportService.validate(new RawRow(6, "2151000010", "Đặng Linh", "linh@"), seen))
				.isEqualTo("Email không hợp lệ");
		assertThat(StudentImportService.validate(new RawRow(7, "", "Ai đó", ""), seen)).isEqualTo("Thiếu mã sinh viên");
	}

	private static void assertBadFile(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String text) {
		assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> {
			assertThat(e.getCode()).isEqualTo("BAD_FILE");
			assertThat(e.getMessage()).containsIgnoringCase(text);
		});
	}

	static byte[] xlsx(List<Map<Integer, Object>> rows) throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			var sheet = wb.createSheet();
			for (int r = 0; r < rows.size(); r++) {
				var row = sheet.createRow(r);
				rows.get(r).forEach((c, v) -> {
					if (v instanceof Double d) {
						row.createCell(c).setCellValue(d);
					} else {
						row.createCell(c).setCellValue(v.toString());
					}
				});
			}
			wb.write(out);
			return out.toByteArray();
		}
	}
}
