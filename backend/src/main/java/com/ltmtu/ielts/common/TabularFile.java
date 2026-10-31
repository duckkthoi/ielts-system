package com.ltmtu.ielts.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Đọc / ghi file bảng ({@code .xlsx}, {@code .csv}) cho các luồng import — dùng chung cho import sinh viên
 * ({@code classroom}) và import giáo viên ({@code user}).
 * <ul>
 * <li>Chỉ trả về các dòng có ít nhất một ô khác rỗng, kèm số dòng như Excel (dòng đầu file là 1).</li>
 * <li>CSV: UTF-8 (có hoặc không BOM), tự nhận dấu phân cách {@code , ; tab}.</li>
 * </ul>
 * Lớp thuần, không phụ thuộc Spring. Không ghi log nội dung file (CLAUDE.md ràng buộc 4).
 */
public final class TabularFile {

	/** Một dòng có dữ liệu: số dòng trong file + giá trị các ô (đã trim). */
	public record Line(int number, List<String> cells) {

		/** Ô thứ {@code i}; không có cột đó → chuỗi rỗng. */
		public String cell(Integer i) {
			return i == null || i >= cells.size() || cells.get(i) == null ? "" : cells.get(i).trim();
		}
	}

	private TabularFile() {
	}

	/** Đọc theo đuôi file. Đuôi khác {@code .xlsx} / {@code .csv} → {@code 400 BAD_FILE}. */
	public static List<Line> read(String filename, InputStream in) throws IOException {
		String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
		if (name.endsWith(".xlsx")) {
			return readXlsx(in);
		}
		if (name.endsWith(".csv")) {
			return readCsv(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
		throw badFile("Chỉ hỗ trợ file .xlsx hoặc .csv");
	}

	public static List<Line> readCsv(String content) throws IOException {
		if (!content.isEmpty() && content.charAt(0) == '﻿') {
			content = content.substring(1);
		}
		CSVFormat format = CSVFormat.DEFAULT.builder()
				.setDelimiter(detectDelimiter(content))
				.setTrim(true)
				.setIgnoreEmptyLines(true)
				.get();
		List<Line> lines = new ArrayList<>();
		try (CSVParser parser = CSVParser.parse(new StringReader(content), format)) {
			for (CSVRecord rec : parser) {
				// Số dòng vật lý trong file (bắt đầu từ 1); bỏ qua dòng trống nên phải lấy từ parser.
				lines.add(new Line((int) parser.getCurrentLineNumber(), rec.toList()));
			}
		}
		return lines;
	}

	public static List<Line> readXlsx(InputStream in) throws IOException {
		List<Line> lines = new ArrayList<>();
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
					lines.add(new Line(row.getRowNum() + 1, cells));
				}
			}
		} catch (org.apache.poi.UnsupportedFileFormatException | org.apache.poi.ooxml.POIXMLException e) {
			throw badFile("Không đọc được file Excel. Hãy lưu lại dưới dạng .xlsx");
		}
		return lines;
	}

	/** "Mã SV" → "masv", "Họ và tên" → "hovaten", "ho_ten" → "hoten". */
	public static String normalizeHeader(String raw) {
		String s = Normalizer.normalize(raw.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return s.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	/** File Excel một sheet: dòng tiêu đề in đậm, các cột dạng chữ (giữ nguyên số 0 đầu, mã dài). */
	public static byte[] xlsx(String sheetName, String[] header, List<String[]> rows) {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Sheet sheet = wb.createSheet(sheetName);
			Font bold = wb.createFont();
			bold.setBold(true);
			CellStyle headerStyle = wb.createCellStyle();
			headerStyle.setFont(bold);
			CellStyle text = wb.createCellStyle();
			text.setDataFormat(wb.createDataFormat().getFormat("@"));
			writeRow(sheet.createRow(0), header, headerStyle);
			for (int r = 0; r < rows.size(); r++) {
				writeRow(sheet.createRow(r + 1), rows.get(r), null);
			}
			for (int i = 0; i < header.length; i++) {
				sheet.setDefaultColumnStyle(i, text);
				// Độ rộng cố định (autoSizeColumn của POI cần font hệ thống).
				sheet.setColumnWidth(i, 24 * 256);
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			wb.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** CSV UTF-8 có BOM (Excel mở đúng tiếng Việt), xuống dòng CRLF. Ô có dấu phẩy / ngoặc kép được bọc. */
	public static byte[] csv(String[] header, List<String[]> rows) {
		StringBuilder sb = new StringBuilder("﻿");
		appendCsv(sb, header);
		rows.forEach(r -> appendCsv(sb, r));
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static void appendCsv(StringBuilder sb, String[] values) {
		for (int i = 0; i < values.length; i++) {
			String v = values[i] == null ? "" : values[i];
			if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
				v = "\"" + v.replace("\"", "\"\"") + "\"";
			}
			sb.append(i == 0 ? "" : ",").append(v);
		}
		sb.append("\r\n");
	}

	private static void writeRow(Row row, String[] values, CellStyle style) {
		for (int i = 0; i < values.length; i++) {
			Cell cell = row.createCell(i);
			cell.setCellValue(values[i] == null ? "" : values[i]);
			if (style != null) {
				cell.setCellStyle(style);
			}
		}
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

	private static ApiException badFile(String message) {
		return ApiException.badRequest("BAD_FILE", message);
	}
}
