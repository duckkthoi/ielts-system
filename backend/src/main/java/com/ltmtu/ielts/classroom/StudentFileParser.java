package com.ltmtu.ielts.classroom;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.TabularFile;
import com.ltmtu.ielts.common.TabularFile.Line;

/**
 * Đọc file danh sách sinh viên ({@code .xlsx} hoặc {@code .csv}) thành các dòng thô — docs/05-api.md mục A.4.
 * <ul>
 * <li>Dòng đầu tiên có dữ liệu là tiêu đề. Tên cột không phân biệt hoa thường / dấu / khoảng trắng: {@code ma_sv},
 * {@code MSSV}, {@code Mã SV}, {@code Mã sinh viên}… đều được.</li>
 * <li>Số dòng ({@link RawRow#line()}) tính như Excel: tiêu đề là dòng 1.</li>
 * </ul>
 * Đọc file thô qua {@link TabularFile} (CSV UTF-8 có / không BOM, tự nhận dấu phân cách). Lớp thuần, không phụ thuộc
 * Spring — dùng chung cho import HTTP và TCP.
 */
public final class StudentFileParser {

	public static final int MAX_ROWS = 2000;

	enum Column {
		CODE, NAME, EMAIL
	}

	private static final Map<String, Column> HEADER_ALIASES = new HashMap<>();

	static {
		for (String a : Set.of("masv", "mssv", "masinhvien", "studentcode", "studentid")) {
			HEADER_ALIASES.put(a, Column.CODE);
		}
		for (String a : Set.of("hoten", "hovaten", "fullname", "name", "ten")) {
			HEADER_ALIASES.put(a, Column.NAME);
		}
		for (String a : Set.of("email", "mail", "thudientu")) {
			HEADER_ALIASES.put(a, Column.EMAIL);
		}
	}

	/** Một dòng dữ liệu chưa kiểm tra. Trường không có giá trị là chuỗi rỗng. */
	public record RawRow(int line, String studentCode, String fullName, String email) {
	}

	private StudentFileParser() {
	}

	public static List<RawRow> parse(String filename, InputStream in) throws IOException {
		return toRows(TabularFile.read(filename, in));
	}

	static List<RawRow> parseCsv(String content) throws IOException {
		return toRows(TabularFile.readCsv(content));
	}

	static List<RawRow> parseXlsx(InputStream in) throws IOException {
		return toRows(TabularFile.readXlsx(in));
	}

	private static List<RawRow> toRows(List<Line> lines) {
		if (lines.isEmpty()) {
			throw badFile("File không có dữ liệu");
		}
		Map<Column, Integer> index = mapHeader(lines.getFirst().cells());
		if (!index.containsKey(Column.CODE) || !index.containsKey(Column.NAME)) {
			throw badFile("File thiếu cột bắt buộc: ma_sv và ho_ten (dòng đầu tiên phải là tiêu đề)");
		}
		if (lines.size() - 1 > MAX_ROWS) {
			throw badFile("Tối đa " + MAX_ROWS + " sinh viên mỗi lần import");
		}
		List<RawRow> rows = new ArrayList<>();
		for (Line line : lines.subList(1, lines.size())) {
			String code = line.cell(index.get(Column.CODE));
			String name = line.cell(index.get(Column.NAME));
			String email = line.cell(index.get(Column.EMAIL));
			if (!code.isEmpty() || !name.isEmpty() || !email.isEmpty()) {
				rows.add(new RawRow(line.number(), code, name, email));
			}
		}
		if (rows.isEmpty()) {
			throw badFile("File không có dòng sinh viên nào");
		}
		return rows;
	}

	private static Map<Column, Integer> mapHeader(List<String> header) {
		Map<Column, Integer> index = new HashMap<>();
		for (int i = 0; i < header.size(); i++) {
			Column col = HEADER_ALIASES.get(normalizeHeader(header.get(i)));
			if (col != null) {
				index.putIfAbsent(col, i);
			}
		}
		return index;
	}

	/** "Mã SV" → "masv", "Họ và tên" → "hovaten", "ho_ten" → "hoten". */
	static String normalizeHeader(String raw) {
		return TabularFile.normalizeHeader(raw);
	}

	private static ApiException badFile(String message) {
		return ApiException.badRequest("BAD_FILE", message);
	}
}
