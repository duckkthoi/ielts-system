package com.ltmtu.ielts.user;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.ltmtu.ielts.audit.AuditAction;
import com.ltmtu.ielts.audit.AuditService;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.TabularFile;
import com.ltmtu.ielts.common.TabularFile.Line;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.security.PasswordGenerator;
import com.ltmtu.ielts.user.dto.AdminUserDtos.ImportedTeacher;
import com.ltmtu.ielts.user.dto.AdminUserDtos.TeacherImportError;
import com.ltmtu.ielts.user.dto.AdminUserDtos.TeacherImportReport;

import lombok.extern.slf4j.Slf4j;

/**
 * Admin import danh sách giáo viên từ file {@code .xlsx} / {@code .csv} (UC-05b) — cùng quy tắc với tạo từng giáo
 * viên (UC-05): mật khẩu tạm, bắt đổi ở lần đăng nhập đầu (BR-02).
 * <ul>
 * <li>Cột bắt buộc {@code ten_dang_nhap}, {@code ho_ten}; tuỳ chọn {@code email}. Tên cột không phân biệt hoa
 * thường / dấu.</li>
 * <li>Mỗi dòng một transaction: dòng lỗi không làm hỏng các dòng khác.</li>
 * <li>Tên đăng nhập đã là giáo viên → bỏ qua (không đổi gì); trùng tài khoản vai trò khác → lỗi dòng.</li>
 * <li>Mật khẩu tạm chỉ trả trong response, không ghi DB / log; nhật ký chỉ ghi số lượng.</li>
 * </ul>
 */
@Slf4j
@Service
public class TeacherImportService {

	public static final int MAX_ROWS = 500;

	private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
	private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private static final String[] TEMPLATE_HEADER = { "ten_dang_nhap", "ho_ten", "email" };
	private static final List<String[]> TEMPLATE_SAMPLE = List.of(
			new String[] { "gv.nguyenan", "Nguyễn Văn An", "an.nv@truong.edu.vn" },
			new String[] { "gv.tranbinh", "Trần Thị Bình", "" });

	enum Column {
		USERNAME, NAME, EMAIL
	}

	private static final Map<String, Column> HEADER_ALIASES = new HashMap<>();

	static {
		for (String a : Set.of("tendangnhap", "username", "taikhoan", "magv", "magiaovien")) {
			HEADER_ALIASES.put(a, Column.USERNAME);
		}
		for (String a : Set.of("hoten", "hovaten", "fullname", "name", "ten")) {
			HEADER_ALIASES.put(a, Column.NAME);
		}
		for (String a : Set.of("email", "mail", "thudientu")) {
			HEADER_ALIASES.put(a, Column.EMAIL);
		}
	}

	/** Một dòng dữ liệu chưa kiểm tra. */
	record RawRow(int line, String username, String fullName, String email) {
	}

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditService audit;
	private final TransactionTemplate rowTx;

	public TeacherImportService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService audit,
			PlatformTransactionManager txManager) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.audit = audit;
		this.rowTx = new TransactionTemplate(txManager);
		this.rowTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public TeacherImportReport importFile(AuthPrincipal admin, String filename, InputStream content) {
		List<RawRow> rows;
		try {
			rows = parse(TabularFile.read(filename, content));
		} catch (IOException e) {
			throw ApiException.badRequest("BAD_FILE", "Không đọc được file");
		}
		int skipped = 0;
		List<TeacherImportError> errors = new ArrayList<>();
		List<ImportedTeacher> accounts = new ArrayList<>();
		Map<String, Integer> seen = new HashMap<>();
		for (RawRow raw : rows) {
			String error = validate(raw, seen);
			if (error == null) {
				RowOutcome outcome = processRow(raw);
				switch (outcome) {
					case RowOutcome.Created c -> accounts.add(c.account());
					case RowOutcome.Skipped s -> skipped++;
					case RowOutcome.Failed f -> error = f.reason();
				}
			}
			if (error != null) {
				errors.add(new TeacherImportError(raw.line(), raw.username(), error));
			}
		}
		audit.record(admin.id(), AuditAction.IMPORT_TEACHERS, "USER", null, Map.of("rows", rows.size(), "created",
				accounts.size(), "skipped", skipped, "errors", errors.size()));
		log.info("Admin {} import giáo viên: {} dòng, tạo {}, bỏ qua {}, lỗi {}", admin.id(), rows.size(),
				accounts.size(), skipped, errors.size());
		return new TeacherImportReport(rows.size(), accounts.size(), skipped, errors, accounts);
	}

	// ─── Đọc file ─────────────────────────────────────────────────────────────

	static List<RawRow> parse(List<Line> lines) {
		if (lines.isEmpty()) {
			throw badFile("File không có dữ liệu");
		}
		Map<Column, Integer> index = new HashMap<>();
		List<String> header = lines.getFirst().cells();
		for (int i = 0; i < header.size(); i++) {
			Column col = HEADER_ALIASES.get(TabularFile.normalizeHeader(header.get(i)));
			if (col != null) {
				index.putIfAbsent(col, i);
			}
		}
		if (!index.containsKey(Column.USERNAME) || !index.containsKey(Column.NAME)) {
			throw badFile("File thiếu cột bắt buộc: ten_dang_nhap và ho_ten (dòng đầu tiên phải là tiêu đề)");
		}
		if (lines.size() - 1 > MAX_ROWS) {
			throw badFile("Tối đa " + MAX_ROWS + " giáo viên mỗi lần import");
		}
		List<RawRow> rows = new ArrayList<>();
		for (Line line : lines.subList(1, lines.size())) {
			RawRow row = new RawRow(line.number(), line.cell(index.get(Column.USERNAME)),
					line.cell(index.get(Column.NAME)), line.cell(index.get(Column.EMAIL)));
			if (!row.username().isEmpty() || !row.fullName().isEmpty() || !row.email().isEmpty()) {
				rows.add(row);
			}
		}
		if (rows.isEmpty()) {
			throw badFile("File không có dòng giáo viên nào");
		}
		return rows;
	}

	/** Kiểm tra một dòng (không đụng DB), cùng ràng buộc với form tạo giáo viên. {@code null} = hợp lệ. */
	static String validate(RawRow raw, Map<String, Integer> seen) {
		if (raw.username().isEmpty()) {
			return "Thiếu tên đăng nhập";
		}
		if (!USERNAME.matcher(raw.username()).matches()) {
			return "Tên đăng nhập không hợp lệ (3–50 ký tự, chỉ chữ không dấu, số và . _ -)";
		}
		if (raw.fullName().isEmpty()) {
			return "Thiếu họ tên";
		}
		if (raw.fullName().length() > 150) {
			return "Họ tên tối đa 150 ký tự";
		}
		if (!raw.email().isEmpty() && (raw.email().length() > 150 || !EMAIL.matcher(raw.email()).matches())) {
			return "Email không hợp lệ";
		}
		Integer firstLine = seen.putIfAbsent(raw.username().toLowerCase(Locale.ROOT), raw.line());
		if (firstLine != null) {
			return "Trùng tên đăng nhập với dòng " + firstLine;
		}
		return null;
	}

	// ─── Ghi từng dòng ────────────────────────────────────────────────────────

	private sealed interface RowOutcome {
		record Created(ImportedTeacher account) implements RowOutcome {
		}

		record Skipped() implements RowOutcome {
		}

		record Failed(String reason) implements RowOutcome {
		}
	}

	private RowOutcome processRow(RawRow raw) {
		try {
			RowOutcome outcome = rowTx.execute(status -> writeRow(raw));
			if (outcome == null) {
				throw new IllegalStateException("Transaction của dòng import không trả kết quả");
			}
			return outcome;
		} catch (DataIntegrityViolationException e) {
			// Hai lần import / tạo cùng lúc cùng một tên đăng nhập: lần sau vi phạm UNIQUE.
			return new RowOutcome.Failed("Tên đăng nhập vừa được tạo bởi thao tác khác, hãy import lại dòng này");
		}
	}

	private RowOutcome writeRow(RawRow raw) {
		User existing = userRepository.findByUsernameIgnoreCase(raw.username()).orElse(null);
		if (existing != null) {
			return existing.getRole() == Role.TEACHER ? new RowOutcome.Skipped()
					: new RowOutcome.Failed("Tên đăng nhập đã thuộc một tài khoản không phải giáo viên");
		}
		String password = PasswordGenerator.temporaryPassword();
		User teacher = new User();
		teacher.setUsername(raw.username());
		teacher.setFullName(raw.fullName());
		teacher.setEmail(raw.email().isEmpty() ? null : raw.email());
		teacher.setRole(Role.TEACHER);
		teacher.setPasswordHash(passwordEncoder.encode(password));
		teacher.setMustChangePassword(true);
		userRepository.saveAndFlush(teacher);
		return new RowOutcome.Created(new ImportedTeacher(teacher.getId(), teacher.getUsername(),
				teacher.getFullName(), teacher.getEmail(), password));
	}

	// ─── File mẫu ─────────────────────────────────────────────────────────────

	public static byte[] templateXlsx() {
		return TabularFile.xlsx("Danh sách giáo viên", TEMPLATE_HEADER, TEMPLATE_SAMPLE);
	}

	public static byte[] templateCsv() {
		return TabularFile.csv(TEMPLATE_HEADER, TEMPLATE_SAMPLE);
	}

	private static ApiException badFile(String message) {
		return ApiException.badRequest("BAD_FILE", message);
	}
}
