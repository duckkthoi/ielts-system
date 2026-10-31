package com.ltmtu.ielts.stats;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.ltmtu.ielts.submission.SubmissionStatus;

/**
 * Dựng file bảng điểm {@code .xlsx} (FR-66) từ dữ liệu đã tính sẵn. Class thuần (chỉ Apache POI), để unit test.
 * Sheet 1 "Bảng điểm": mỗi sinh viên một hàng; mỗi phần gồm điểm từng tiêu chí, band phần, trạng thái; cuối hàng là
 * band cả bài và số phần nộp muộn. Sheet 2 "Thông tin": tên bài, thời điểm xuất, ghi chú cách tính.
 */
final class GradebookWorkbook {

	private GradebookWorkbook() {
	}

	record Column(String title, List<String> criteria) {
	}

	/** Một hàng: theo từng phần — điểm tiêu chí (cùng thứ tự {@link Column#criteria}), band, trạng thái. */
	record Line(String studentCode, String fullName, List<PartValues> parts, BigDecimal band, int lateParts) {
	}

	record PartValues(List<BigDecimal> scores, BigDecimal band, SubmissionStatus status, boolean late) {
	}

	static byte[] build(String assignmentTitle, List<String> info, List<Column> parts, List<Line> lines, boolean paper) {
		String scoreLabel = paper ? "Điểm" : "Band";
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			CellStyle header = headerStyle(wb);
			CellStyle number = wb.createCellStyle();
			number.setDataFormat(wb.createDataFormat().getFormat(paper ? "0.00" : "0.0"));
			CellStyle text = wb.createCellStyle();
			text.setDataFormat(wb.createDataFormat().getFormat("@"));

			Sheet sheet = wb.createSheet("Bảng điểm");
			Row h = sheet.createRow(0);
			int c = 0;
			c = put(h, c, "STT", header);
			c = put(h, c, "Mã SV", header);
			c = put(h, c, "Họ tên", header);
			for (Column p : parts) {
				for (String criterion : p.criteria()) {
					c = put(h, c, p.title() + " – " + criterion, header);
				}
				c = put(h, c, p.title() + " – " + scoreLabel, header);
				c = put(h, c, p.title() + " – Trạng thái", header);
			}
			c = put(h, c, scoreLabel + " cả bài", header);
			int columns = put(h, c, "Số phần nộp muộn", header);

			int r = 1;
			for (Line line : lines) {
				Row row = sheet.createRow(r);
				c = 0;
				row.createCell(c++).setCellValue(r);
				Cell code = row.createCell(c++);
				code.setCellValue(line.studentCode());
				code.setCellStyle(text);
				row.createCell(c++).setCellValue(line.fullName());
				for (PartValues v : line.parts()) {
					for (BigDecimal score : v.scores()) {
						c = putNumber(row, c, score, number);
					}
					c = putNumber(row, c, v.band(), number);
					row.createCell(c++).setCellValue(statusLabel(v.status()) + (v.late() ? " (muộn)" : ""));
				}
				c = putNumber(row, c, line.band(), number);
				row.createCell(c).setCellValue(line.lateParts());
				r++;
			}
			sheet.createFreezePane(3, 1);
			sheet.setColumnWidth(0, 6 * 256);
			sheet.setColumnWidth(1, 14 * 256);
			sheet.setColumnWidth(2, 26 * 256);
			for (int i = 3; i < columns; i++) {
				sheet.setColumnWidth(i, 16 * 256);
			}

			Sheet about = wb.createSheet("Thông tin");
			put(about.createRow(0), 0, assignmentTitle, header);
			for (int i = 0; i < info.size(); i++) {
				about.createRow(i + 1).createCell(0).setCellValue(info.get(i));
			}
			about.setColumnWidth(0, 100 * 256);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			wb.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Nhãn tiếng Việt của trạng thái bài làm (docs/06 mục 2). */
	static String statusLabel(SubmissionStatus s) {
		return switch (s) {
			case NOT_STARTED -> "Chưa làm";
			case DRAFT -> "Đang làm";
			case SUBMITTED -> "Đã nộp";
			case GRADED -> "Đang chấm";
			case RETURNED -> "Đã trả";
			case REVISION_REQUESTED -> "Cần sửa";
		};
	}

	private static int put(Row row, int col, String value, CellStyle style) {
		Cell cell = row.createCell(col);
		cell.setCellValue(value);
		cell.setCellStyle(style);
		return col + 1;
	}

	/** Ô số; không có giá trị thì để trống (không ghi 0, để không lẫn với "Chấm 0"). */
	private static int putNumber(Row row, int col, BigDecimal value, CellStyle style) {
		Cell cell = row.createCell(col);
		if (value != null) {
			cell.setCellValue(value.doubleValue());
			cell.setCellStyle(style);
		}
		return col + 1;
	}

	private static CellStyle headerStyle(XSSFWorkbook wb) {
		Font bold = wb.createFont();
		bold.setBold(true);
		CellStyle style = wb.createCellStyle();
		style.setFont(bold);
		style.setWrapText(true);
		return style;
	}
}
