package com.ltmtu.ielts.stats;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.stats.GradebookExportService.ExportFile;
import com.ltmtu.ielts.stats.dto.AnalyticsDtos.StudentRow;

import lombok.RequiredArgsConstructor;

/**
 * Xuất bảng sinh viên của thống kê tổng hợp (H5) ra CSV (UTF-8 có BOM để Excel đọc đúng tiếng Việt) hoặc XLSX — đúng
 * bộ lọc đang xem, tên cột tiếng Việt, giờ ghi rõ múi giờ. Không có nội dung bài làm.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsExportService {

	public enum Format {
		CSV, XLSX
	}

	static final List<String> HEADERS = List.of("Mã SV", "Họ tên", "Lớp", "Số phần được giao", "Đã nộp", "Quá hạn",
			"Lần sửa", "Band gần nhất", "Tiến bộ (band)", "Hoạt động lần cuối", "Cảnh báo");

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final AppProperties props;

	public ExportFile export(AnalyticsService.Filter f, List<StudentRow> rows, Format format) {
		ZoneId zone = props.zone();
		String title = "Thống kê sinh viên từ " + DATE.format(f.from()) + " đến " + DATE.format(f.to())
				+ " — giờ theo " + zone.getId();
		List<List<String>> lines = new ArrayList<>();
		for (StudentRow r : rows) {
			lines.add(List.of(nz(r.studentCode()), r.fullName(), nz(r.classes()), String.valueOf(r.assigned()),
					String.valueOf(r.submitted()), String.valueOf(r.overdue()), String.valueOf(r.revisions()),
					num(r.latestBand()), num(r.progress()),
					r.lastActivityAt() == null ? "" : DATE_TIME.format(r.lastActivityAt().atZone(zone)),
					String.join("; ", r.alerts())));
		}
		String base = "thong-ke-sinh-vien-" + f.from() + "_" + f.to();
		return format == Format.CSV ? new ExportFile(base + ".csv", csv(title, lines))
				: new ExportFile(base + ".xlsx", xlsx(title, lines));
	}

	private static byte[] csv(String title, List<List<String>> lines) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8),
				CSVFormat.EXCEL)) {
			out.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
			printer.printRecord(title);
			printer.printRecord(HEADERS);
			for (List<String> line : lines) {
				printer.printRecord(line);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return out.toByteArray();
	}

	private static byte[] xlsx(String title, List<List<String>> lines) {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = wb.createSheet("Sinh viên");
			Font bold = wb.createFont();
			bold.setBold(true);
			CellStyle head = wb.createCellStyle();
			head.setFont(bold);
			sheet.createRow(0).createCell(0).setCellValue(title);
			Row header = sheet.createRow(2);
			for (int c = 0; c < HEADERS.size(); c++) {
				Cell cell = header.createCell(c);
				cell.setCellValue(HEADERS.get(c));
				cell.setCellStyle(head);
			}
			for (int r = 0; r < lines.size(); r++) {
				Row row = sheet.createRow(r + 3);
				List<String> line = lines.get(r);
				for (int c = 0; c < line.size(); c++) {
					String v = line.get(c);
					// Cột số (3 → 8) ghi dạng số để lọc / tính trong Excel.
					if (c >= 3 && c <= 8 && !v.isEmpty()) {
						row.createCell(c).setCellValue(Double.parseDouble(v));
					} else {
						row.createCell(c).setCellValue(v);
					}
				}
			}
			for (int c = 0; c < HEADERS.size(); c++) {
				sheet.autoSizeColumn(c);
			}
			wb.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	private static String num(BigDecimal v) {
		return v == null ? "" : v.stripTrailingZeros().toPlainString();
	}
}
