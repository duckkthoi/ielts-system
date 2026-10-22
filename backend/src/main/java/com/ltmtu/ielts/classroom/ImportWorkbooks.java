package com.ltmtu.ielts.classroom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.ltmtu.ielts.classroom.ImportCredentialStore.Credential;

/** Sinh file Excel / CSV cho luồng import: file mẫu và file tài khoản vừa tạo. */
final class ImportWorkbooks {

	private static final String[] TEMPLATE_HEADER = { "ma_sv", "ho_ten", "email" };
	private static final String[][] TEMPLATE_SAMPLE = {
			{ "2151000001", "Nguyễn Văn An", "an.nv@sv.edu.vn" },
			{ "2151000002", "Trần Thị Bình", "" } };

	private ImportWorkbooks() {
	}

	static byte[] templateXlsx() {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Sheet sheet = wb.createSheet("Danh sách sinh viên");
			writeRow(sheet.createRow(0), TEMPLATE_HEADER, headerStyle(wb));
			for (int i = 0; i < TEMPLATE_SAMPLE.length; i++) {
				writeRow(sheet.createRow(i + 1), TEMPLATE_SAMPLE[i], null);
			}
			// Cột mã SV dạng chữ để Excel không đổi 2151000001 thành 2.15E+09.
			CellStyle text = wb.createCellStyle();
			text.setDataFormat(wb.createDataFormat().getFormat("@"));
			sheet.setDefaultColumnStyle(0, text);
			setColumnWidths(sheet, TEMPLATE_HEADER.length);
			return toBytes(wb);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static byte[] templateCsv() {
		StringBuilder sb = new StringBuilder("﻿"); // BOM để Excel mở đúng tiếng Việt
		sb.append(String.join(",", TEMPLATE_HEADER)).append("\r\n");
		for (String[] row : TEMPLATE_SAMPLE) {
			sb.append(String.join(",", row)).append("\r\n");
		}
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	static byte[] credentialsXlsx(String classroomCode, List<Credential> credentials) {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Sheet sheet = wb.createSheet("Tài khoản " + classroomCode);
			String[] header = { "STT", "Mã SV", "Họ tên", "Email", "Tên đăng nhập", "Mật khẩu tạm" };
			writeRow(sheet.createRow(0), header, headerStyle(wb));
			int r = 1;
			for (Credential c : credentials) {
				writeRow(sheet.createRow(r), new String[] { String.valueOf(r), c.studentCode(), c.fullName(),
						c.email() == null ? "" : c.email(), c.studentCode(), c.temporaryPassword() }, null);
				r++;
			}
			Row note = sheet.createRow(r + 1);
			note.createCell(0).setCellValue(
					"Mật khẩu tạm chỉ dùng cho lần đăng nhập đầu tiên; hệ thống sẽ yêu cầu sinh viên đổi mật khẩu.");
			setColumnWidths(sheet, header.length);
			return toBytes(wb);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void writeRow(Row row, String[] values, CellStyle style) {
		for (int i = 0; i < values.length; i++) {
			var cell = row.createCell(i);
			cell.setCellValue(values[i]);
			if (style != null) {
				cell.setCellStyle(style);
			}
		}
	}

	private static CellStyle headerStyle(XSSFWorkbook wb) {
		Font bold = wb.createFont();
		bold.setBold(true);
		CellStyle style = wb.createCellStyle();
		style.setFont(bold);
		return style;
	}

	/** Độ rộng cố định (autoSizeColumn của POI cần font hệ thống, dễ lỗi khi chạy server không có giao diện). */
	private static void setColumnWidths(Sheet sheet, int columns) {
		for (int i = 0; i < columns; i++) {
			sheet.setColumnWidth(i, 22 * 256);
		}
	}

	private static byte[] toBytes(XSSFWorkbook wb) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		wb.write(out);
		return out.toByteArray();
	}
}
