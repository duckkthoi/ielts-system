package com.ltmtu.ielts.classroom;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.ltmtu.ielts.common.ApiException;

/**
 * Đọc file danh sách sinh viên ({@code .xlsx} hoặc {@code .csv}) thành các dòng thô — docs/05-api.md mục A.4.
 * <ul>
 * <li>Dòng đầu tiên có dữ liệu là tiêu đề. Tên cột không phân biệt hoa thường / dấu / khoảng trắng: {@code ma_sv},
 * {@code MSSV}, {@code Mã SV}, {@code Mã sinh viên}… đều được.</li>
 * <li>CSV: UTF-8 (có hoặc không BOM), tự nhận dấu phân cách {@code , ; tab}.</li>
 * <li>Số dòng ({@link RawRow#line()}) tính như Excel: tiêu đề là dòng 1.</li>
 * </ul>
 * Lớp thuần, không phụ thuộc Spring — dùng chung cho import HTTP và TCP.
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
		String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
		if (name.endsWith(".xlsx")) {
			return parseXlsx(in);
		}
		if (name.endsWith(".csv")) {
			return parseCsv(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
		throw badFile("Chỉ hỗ trợ file .xlsx hoặc .csv");
	}

	static List<RawRow> parseCsv(String content) throws IOException {
		if (!content.isEmpty() && content.charAt(0) == '﻿') {
			content = content.substring(1);
		}
		CSVFormat format = CSVFormat.DEFAULT.builder()
				.setDelimiter(detectDelimiter(content))
				.setTrim(true)
				.setIgnoreEmptyLines(true)
				.get();
		List<List<String>> lines = new ArrayList<>();
		List<Integer> lineNumbers = new ArrayList<>();
		try (CSVParser parser = CSVParser.parse(new StringReader(content), format)) {
			for (CSVRecord rec : parser) {
				lines.add(rec.toList());
				// Số dòng vật lý trong file (bắt đầu từ 1); bỏ qua dòng trống nên phải lấy từ parser.
				lineNumbers.add((int) parser.getCurrentLineNumber());
			}
		}
		return toRows(lines, lineNumbers);
	}

	static List<RawRow> parseXlsx(InputStream in) throws IOException {
		List<List<String>> lines = new ArrayList<>();
		List<Integer> lineNumbers = new ArrayList<>();
		DataFormatter formatter = new DataFormatter(Locale.ROOT);
		try (Workbook wb = new XSSFWorkbook(in)) {
			if (wb.getNumberOfSheets() == 0) {
				throw badFile("File Excel không có sheet nào");
			}
			Sheet sheet = wb.getSheetAt(0);
			for (Row row : sheet) {
				List<String> cells = new ArrayList<>();
				for (int i = 0; i < Math.max(row.getLastCellNum(), 0); i++) {
					Cell cell = row.getCell(i);
					// DataFormatter đọc số như hiển thị: 2151000001 chứ không phải 2.151000001E9.
					cells.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
				}
				if (cells.stream().anyMatch(s -> !s.isEmpty())) {
					lines.add(cells);
					lineNumbers.add(row.getRowNum() + 1);
				}
			}
		} catch (org.apache.poi.UnsupportedFileFormatException | org.apache.poi.ooxml.POIXMLException e) {
			throw badFile("Không đọc được file Excel. Hãy lưu lại dưới dạng .xlsx");
		}
		return toRows(lines, lineNumbers);
	}

	private static List<RawRow> toRows(List<List<String>> lines, List<Integer> lineNumbers) {
		if (lines.isEmpty()) {
			throw badFile("File không có dữ liệu");
		}
		Map<Column, Integer> index = mapHeader(lines.getFirst());
		if (!index.containsKey(Column.CODE) || !index.containsKey(Column.NAME)) {
			throw badFile("File thiếu cột bắt buộc: ma_sv và ho_ten (dòng đầu tiên phải là tiêu đề)");
		}
		if (lines.size() - 1 > MAX_ROWS) {
			throw badFile("Tối đa " + MAX_ROWS + " sinh viên mỗi lần import");
		}
		List<RawRow> rows = new ArrayList<>();
		for (int i = 1; i < lines.size(); i++) {
			List<String> cells = lines.get(i);
			String code = cell(cells, index.get(Column.CODE));
			String name = cell(cells, index.get(Column.NAME));
			String email = cell(cells, index.get(Column.EMAIL));
			if (!code.isEmpty() || !name.isEmpty() || !email.isEmpty()) {
				rows.add(new RawRow(lineNumbers.get(i), code, name, email));
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
		String s = Normalizer.normalize(raw.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return s.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private static char detectDelimiter(String content) {
		String firstLine = content.lines().filter(l -> !l.isBlank()).findFirst().orElse("");
		char best = ',';
		long bestCount = -1;
		for (char c : new char[] { ',', ';', '\t' }) {
			long count = firstLine.chars().filter(ch -> ch == c).count();
			if (count > bestCount) {
				best = c;
				bestCount = count;
			}
		}
		return best;
	}

	private static String cell(List<String> cells, Integer i) {
		return i == null || i >= cells.size() || cells.get(i) == null ? "" : cells.get(i).trim();
	}

	private static ApiException badFile(String message) {
		return ApiException.badRequest("BAD_FILE", message);
	}
}
