package com.ltmtu.ielts.classroom;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.ltmtu.ielts.audit.AuditAction;
import com.ltmtu.ielts.audit.AuditService;
import com.ltmtu.ielts.classroom.ImportCredentialStore.Credential;
import com.ltmtu.ielts.classroom.StudentFileParser.RawRow;
import com.ltmtu.ielts.classroom.dto.ImportDtos.ImportReport;
import com.ltmtu.ielts.classroom.dto.ImportDtos.RowError;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.notification.MailService;
import com.ltmtu.ielts.notification.MailService.NewAccountMail;
import com.ltmtu.ielts.security.PasswordGenerator;
import com.ltmtu.ielts.user.StudentAccountService;
import com.ltmtu.ielts.user.StudentAccountService.Lookup;
import com.ltmtu.ielts.user.User;

import lombok.extern.slf4j.Slf4j;

/**
 * Import danh sách sinh viên vào lớp và tự tạo tài khoản theo mã SV — yêu cầu cốt lõi (UC-11, SD-03, BR-01..03).
 * <p>
 * Hai cửa vào dùng chung một lõi: file upload (HTTP) và CSV qua TCP ({@code tcpgateway}). Mỗi dòng chạy trong
 * <b>một transaction riêng</b>: dòng lỗi không làm hỏng các dòng khác, và client mất kết nối
 * giữa chừng (import qua TCP) thì các dòng đã xử lý vẫn được giữ.
 */
@Slf4j
@Service
public class StudentImportService {

	private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9]{4,30}$");
	private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private final ClassroomService classroomService;
	private final ClassroomRepository classroomRepository;
	private final ClassMemberRepository memberRepository;
	private final StudentAccountService studentAccountService;
	private final ImportCredentialStore credentialStore;
	private final MailService mailService;
	private final Clock clock;
	private final TransactionTemplate rowTx;
	private final AuditService audit;

	public StudentImportService(ClassroomService classroomService, ClassroomRepository classroomRepository,
			ClassMemberRepository memberRepository, StudentAccountService studentAccountService,
			ImportCredentialStore credentialStore, MailService mailService, Clock clock,
			PlatformTransactionManager txManager, AuditService audit) {
		this.classroomService = classroomService;
		this.classroomRepository = classroomRepository;
		this.memberRepository = memberRepository;
		this.studentAccountService = studentAccountService;
		this.credentialStore = credentialStore;
		this.mailService = mailService;
		this.clock = clock;
		this.rowTx = new TransactionTemplate(txManager);
		this.rowTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.audit = audit;
	}

	/** Import từ file upload (HTTP). Tài khoản mới được giữ tạm để tải file một lần (BR-02). */
	public ImportReport importFile(Long teacherId, Long classroomId, String filename, InputStream content,
			boolean sendEmail) {
		Classroom classroom = classroomService.requireOwned(teacherId, classroomId);
		List<RawRow> rows;
		try {
			rows = StudentFileParser.parse(filename, content);
		} catch (IOException e) {
			throw ApiException.badRequest("BAD_FILE", "Không đọc được file");
		}
		Outcome o = run(classroom, rows, sendEmail, (done, total) -> {
		});
		audit.record(teacherId, AuditAction.IMPORT_STUDENTS, "CLASSROOM", classroom.getId(), o.summary());
		ImportCredentialStore.Issued issued = o.credentials().isEmpty() ? null
				: credentialStore.put(teacherId, classroom.getCode(), o.credentials());
		return o.report(issued);
	}

	/**
	 * Lớp nhận import qua TCP (docs/05 phần C), tra theo mã lớp (không phân biệt hoa thường). Không có lớp → 404;
	 * giáo viên chỉ import vào lớp của mình (khác → 403), admin import được mọi lớp. Gọi trước khi nhận dữ liệu, để
	 * từ chối sớm.
	 */
	public Classroom requireImportableClass(Long actorId, boolean admin, String classCode) {
		Classroom classroom = classroomRepository.findByCode(ClassroomService.normalizeCode(classCode))
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy lớp " + classCode));
		if (!admin && classroomRepository.findByIdAndTeacherId(classroom.getId(), actorId).isEmpty()) {
			throw ApiException.forbidden("Không có quyền import vào lớp " + classroom.getCode());
		}
		return classroom;
	}

	/**
	 * Import nội dung CSV nhận qua TCP. {@code progress} được gọi sau mỗi dòng với (đã xử lý, tổng) để gửi
	 * {@code PROGRESS n/total}. Tài khoản mới (kèm mật khẩu tạm) trả thẳng cho người gọi, chỉ nằm trong bộ nhớ.
	 */
	public CsvImportResult importCsv(Long actorId, boolean admin, String classCode, byte[] csv, boolean sendEmail,
			String clientIp, BiConsumer<Integer, Integer> progress) {
		Classroom classroom = requireImportableClass(actorId, admin, classCode);
		List<RawRow> rows;
		try {
			rows = StudentFileParser.parseCsv(new String(csv, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw ApiException.badRequest("BAD_FILE", "Nội dung CSV không hợp lệ");
		}
		Outcome o = run(classroom, rows, sendEmail, progress);
		audit.recordAlone(actorId, AuditAction.TCP_IMPORT, "CLASSROOM", classroom.getId(), clientIp, o.summary());
		List<NewAccount> accounts = o.credentials().stream()
				.map(c -> new NewAccount(c.studentCode(), c.fullName(), c.temporaryPassword())).toList();
		return new CsvImportResult(o.report(null), accounts);
	}

	/** Kết quả import qua TCP. */
	public record CsvImportResult(ImportReport report, List<NewAccount> accounts) {
	}

	/** Tài khoản vừa tạo. Mật khẩu tạm không ghi DB, không ghi log (BR-02). */
	public record NewAccount(String studentCode, String fullName, String temporaryPassword) {
	}

	/** Xử lý các dòng đã đọc, gửi email tài khoản nếu được yêu cầu. */
	private Outcome run(Classroom classroom, List<RawRow> rows, boolean sendEmail,
			BiConsumer<Integer, Integer> progress) {
		Counters counters = new Counters();
		List<RowError> errors = new ArrayList<>();
		List<Credential> credentials = new ArrayList<>();
		Map<String, Integer> seen = new HashMap<>();

		for (int i = 0; i < rows.size(); i++) {
			RawRow raw = rows.get(i);
			String error = validate(raw, seen);
			if (error == null) {
				error = processRow(classroom.getId(), raw, counters, credentials);
			}
			if (error != null) {
				errors.add(new RowError(raw.line(), raw.studentCode(), error));
			}
			progress.accept(i + 1, rows.size());
		}

		List<NewAccountMail> mails = credentials.stream().filter(c -> c.email() != null)
				.map(c -> new NewAccountMail(c.email(), c.fullName(), c.studentCode(), c.temporaryPassword()))
				.toList();
		boolean emailQueued = sendEmail && !mails.isEmpty();
		if (emailQueued) {
			mailService.sendNewStudentAccounts(classroom.getName(), mails);
		}

		log.info("Import lớp {}: {} dòng, tạo {}, đã có {}, bỏ qua {}, lỗi {}", classroom.getId(), rows.size(),
				counters.created, counters.existing, counters.skipped, errors.size());
		return new Outcome(rows.size(), counters, errors, credentials, emailQueued);
	}

	private record Outcome(int totalRows, Counters counters, List<RowError> errors, List<Credential> credentials,
			boolean emailQueued) {

		/** Số liệu cho nhật ký (không có mã SV, tên hay mật khẩu). */
		Map<String, Integer> summary() {
			return Map.of("rows", totalRows, "created", counters.created, "existing", counters.existing, "skipped",
					counters.skipped, "errors", errors.size());
		}

		ImportReport report(ImportCredentialStore.Issued issued) {
			return new ImportReport(totalRows, counters.created, counters.existing, counters.skipped, errors,
					emailQueued, issued == null ? null : issued.token(), issued == null ? null : issued.expiresAt());
		}
	}

	/** Kiểm tra một dòng (không đụng DB). Trả về lý do lỗi, hoặc {@code null} nếu hợp lệ. */
	static String validate(RawRow raw, Map<String, Integer> seen) {
		String code = raw.studentCode().toUpperCase();
		if (code.isEmpty()) {
			return "Thiếu mã sinh viên";
		}
		if (!CODE.matcher(code).matches()) {
			return "Mã sinh viên không hợp lệ (4–30 ký tự, chỉ chữ và số)";
		}
		if (raw.fullName().length() < 2 || raw.fullName().length() > 150) {
			return raw.fullName().isEmpty() ? "Thiếu họ tên" : "Họ tên phải từ 2 đến 150 ký tự";
		}
		if (!raw.email().isEmpty() && (raw.email().length() > 150 || !EMAIL.matcher(raw.email()).matches())) {
			return "Email không hợp lệ";
		}
		Integer firstLine = seen.putIfAbsent(code, raw.line());
		if (firstLine != null) {
			return "Trùng mã sinh viên với dòng " + firstLine;
		}
		return null;
	}

	/** Kết quả ghi một dòng. Chỉ được cộng vào báo cáo SAU KHI transaction của dòng đã commit. */
	private sealed interface RowOutcome {
		record Created(Credential credential) implements RowOutcome {
		}

		record AddedExisting() implements RowOutcome {
		}

		record AlreadyMember() implements RowOutcome {
		}

		record Failed(String reason) implements RowOutcome {
		}
	}

	/** Ghi một dòng trong transaction riêng (BR-03). Trả về lý do lỗi, hoặc {@code null} nếu thành công. */
	private String processRow(Long classroomId, RawRow raw, Counters counters, List<Credential> credentials) {
		RowOutcome outcome;
		try {
			outcome = rowTx.execute(status -> writeRow(classroomId, raw));
		} catch (DataIntegrityViolationException e) {
			// Hai lần import chạy cùng lúc cùng tạo một mã SV mới: lần sau vi phạm UNIQUE.
			outcome = new RowOutcome.Failed("Mã sinh viên vừa được tạo bởi thao tác khác, hãy import lại dòng này");
		}
		switch (outcome) {
			case RowOutcome.Created c -> {
				counters.created++;
				credentials.add(c.credential());
			}
			case RowOutcome.AddedExisting a -> counters.existing++;
			case RowOutcome.AlreadyMember m -> counters.skipped++;
			case RowOutcome.Failed f -> {
				return f.reason();
			}
			case null -> throw new IllegalStateException("Transaction của dòng import không trả kết quả");
		}
		return null;
	}

	private RowOutcome writeRow(Long classroomId, RawRow raw) {
		String code = raw.studentCode().toUpperCase();
		String email = raw.email().isEmpty() ? null : raw.email();
		Classroom classroom = classroomRepository.getReferenceById(classroomId);
		return switch (studentAccountService.lookup(code)) {
			case Lookup.UsernameTaken t -> new RowOutcome.Failed("Mã sinh viên trùng tên đăng nhập của một tài khoản khác");
			case Lookup.Existing e -> {
				User student = e.student();
				if (memberRepository.existsById(new ClassMember.Id(classroomId, student.getId()))) {
					yield new RowOutcome.AlreadyMember();
				}
				memberRepository.save(new ClassMember(classroom, student, clock.instant()));
				yield new RowOutcome.AddedExisting();
			}
			case Lookup.Available a -> {
				String password = PasswordGenerator.temporaryPassword();
				User student = studentAccountService.createStudent(code, raw.fullName(), email, password);
				memberRepository.save(new ClassMember(classroom, student, clock.instant()));
				yield new RowOutcome.Created(new Credential(code, raw.fullName(), email, password));
			}
		};
	}

	/** Lấy file tài khoản (một lần). Sai giáo viên, đã tải hoặc hết hạn → 404. */
	public CredentialsFile takeCredentials(Long teacherId, String token) {
		ImportCredentialStore.Entry entry = credentialStore.take(token, teacherId)
				.orElseThrow(() -> ApiException.notFound(
						"File tài khoản không còn: đã được tải trước đó hoặc đã hết hạn 15 phút"));
		return new CredentialsFile("tai-khoan-" + entry.classroomCode().toLowerCase() + ".xlsx",
				ImportWorkbooks.credentialsXlsx(entry.classroomCode(), entry.credentials()));
	}

	public record CredentialsFile(String filename, byte[] content) {
	}

	private static final class Counters {
		int created;
		int existing;
		int skipped;
	}
}
