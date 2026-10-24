package com.ltmtu.ielts.stats;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

/**
 * Xuất bảng điểm (FR-66). Bài: Task 1 (TA, CC), Task 2 (TR); giao trực tiếp A, B và lớp gồm C:
 * <ul>
 * <li>A: Task 1 = 6 / 7 → 6.5 đã trả; Task 2 = 7 đã trả, rồi bị yêu cầu sửa và đã nộp bản sửa (đang chờ chấm) → vẫn
 * hiện 7 của lần trả gần nhất; band bài (6.5 + 7 × 2) / 3 = 6.83 → 7.0;</li>
 * <li>B: Task 1 "Chấm 0" đã trả; Task 2 nộp muộn, chưa chấm;</li>
 * <li>C: chưa làm gì.</li>
 * </ul>
 */
class GradebookExportIntegrationTest extends StatsTestSupport {

	private static final List<String> HEADER = List.of("STT", "Mã SV", "Họ tên", "Task 1 – TA", "Task 1 – CC",
			"Task 1 – Band", "Task 1 – Trạng thái", "Task 2 – TR", "Task 2 – Band", "Task 2 – Trạng thái", "Band cả bài",
			"Số phần nộp muộn");

	private User a;
	private User b;
	private User c;
	private long classId;
	private long assignmentId;

	@BeforeEach
	void setUp() throws Exception {
		a = newUser(Role.STUDENT);
		b = newUser(Role.STUDENT);
		c = newUser(Role.STUDENT);
		classId = createClass(teacher, c);
		Created x = publish(Duration.ofDays(3), List.of(classId), List.of(a, b),
				List.of(part("Task 1", "WRITING_TASK1", "TA", "CC"), part("Task 2", "WRITING_TASK2", "TR")));
		assignmentId = x.id();

		long a1 = submit(a, x.part(0));
		Map<Long, Integer> scores = new LinkedHashMap<>();
		scores.put(x.criterion(0, "TA"), 6);
		scores.put(x.criterion(0, "CC"), 7);
		grade(a1, scores);
		returnGrade(a1);
		long a2 = submit(a, x.part(1));
		grade(a2, Map.of(x.criterion(1, "TR"), 7));
		call(postJson("/api/teacher/submissions/" + a2 + "/request-revision",
				Map.of("dueAt", clock.instant().plus(Duration.ofDays(10)).toString())), teacher)
				.andExpect(apiStatus(200));
		submit(a, x.part(1), 1);

		long b1 = submit(b, x.part(0));
		grade(b1, 1, Map.of(), "NOT_ENGLISH", List.of());
		returnGrade(b1);

		clock.advance(Duration.ofDays(4));
		reloginTeacher();
		submit(b, x.part(1));
	}

	@Test
	@DisplayName("FR-66: file .xlsx mở được, đúng cột; điểm lấy từ lần trả gần nhất; ô chưa có điểm để trống, \"Chấm 0\" ghi 0")
	void workbook() throws Exception {
		MvcResult res = call(get(url()), teacher)
				.andExpect(apiStatus(200))
				.andExpect(header().string("Content-Type",
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.andReturn();
		assertThat(res.getResponse().getHeader("Content-Disposition"))
				.contains("attachment").contains("bang-diem-bai-" + assignmentId + ".xlsx");

		try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(res.getResponse().getContentAsByteArray()))) {
			Sheet sheet = wb.getSheet("Bảng điểm");
			assertThat(values(sheet.getRow(0))).containsExactlyElementsOf(HEADER);
			assertThat(sheet.getLastRowNum()).isEqualTo(3);

			assertThat(values(row(sheet, a))).containsExactly("Đầu", a.getStudentCode(), a.getFullName(), "6.0", "7.0",
					"6.5", "Đã trả", "7.0", "7.0", "Đã nộp", "7.0", "0.0");
			assertThat(values(row(sheet, b))).containsExactly("Đầu", b.getStudentCode(), b.getFullName(), "0.0", "0.0",
					"0.0", "Đã trả", "", "", "Đã nộp (muộn)", "", "1.0");
			assertThat(values(row(sheet, c))).containsExactly("Đầu", c.getStudentCode(), c.getFullName(), "", "", "",
					"Chưa làm", "", "", "Chưa làm", "", "0.0");

			Sheet about = wb.getSheet("Thông tin");
			assertThat(about.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Bài thống kê");
			assertThat(about.getRow(3).getCell(0).getStringCellValue()).contains("3 sinh viên");
		}
	}

	@Test
	@DisplayName("Lọc theo lớp: chỉ thành viên lớp, tên file có mã lớp")
	void byClass() throws Exception {
		MvcResult res = call(get(url()).param("classId", String.valueOf(classId)), teacher)
				.andExpect(apiStatus(200)).andReturn();
		assertThat(res.getResponse().getHeader("Content-Disposition")).contains("bang-diem-bai-" + assignmentId + "-ST-");
		try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(res.getResponse().getContentAsByteArray()))) {
			Sheet sheet = wb.getSheet("Bảng điểm");
			assertThat(sheet.getLastRowNum()).isEqualTo(1);
			assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo(c.getStudentCode());
		}
	}

	@Test
	@DisplayName("Phân quyền: bài của giáo viên khác → 404, sinh viên → 403")
	void access() throws Exception {
		call(get(url()), accessToken(newUser(Role.TEACHER))).andExpect(apiStatus(404));
		call(get(url()), accessToken(a)).andExpect(apiStatus(403));
	}

	private String url() {
		return "/api/teacher/assignments/" + assignmentId + "/export";
	}

	private static Row row(Sheet sheet, User student) {
		for (int i = 1; i <= sheet.getLastRowNum(); i++) {
			if (student.getStudentCode().equals(sheet.getRow(i).getCell(1).getStringCellValue())) {
				return sheet.getRow(i);
			}
		}
		throw new AssertionError("Thiếu hàng của " + student.getStudentCode());
	}

	/** Giá trị các ô dạng chữ; ô số ghi 1 chữ số thập phân; cột STT thay bằng "Đầu" vì thứ tự hàng theo mã SV ngẫu nhiên. */
	private static List<String> values(Row row) {
		List<String> out = new ArrayList<>();
		for (int i = 0; i < row.getLastCellNum(); i++) {
			Cell cell = row.getCell(i);
			if (row.getRowNum() > 0 && i == 0) {
				out.add("Đầu");
			} else if (cell == null || cell.getCellType() == CellType.BLANK) {
				out.add("");
			} else if (cell.getCellType() == CellType.NUMERIC) {
				out.add(String.format(Locale.ROOT, "%.1f", cell.getNumericCellValue()));
			} else {
				out.add(cell.getStringCellValue());
			}
		}
		return out;
	}
}
