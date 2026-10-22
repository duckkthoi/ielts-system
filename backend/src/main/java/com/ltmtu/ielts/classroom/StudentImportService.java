package com.ltmtu.ielts.classroom;

import java.io.IOException;
import java.io.InputStream;
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
 * Mỗi dòng chạy trong <b>một transaction riêng</b>: dòng lỗi không làm hỏng các dòng khác, và client mất kết nối
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

	public StudentImportService(ClassroomService classroomService, ClassroomRepository classroomRepository,
			ClassMemberRepository memberRepository, StudentAccountService studentAccountService,
			ImportCredentialStore credentialStore, MailService mailService, Clock clock,
			PlatformTransactionManager txManager) {
		this.classroomService = classroomService;
		this.classroomRepository = classroomRepository;
		this.memberRepository = memberRepository;
		this.studentAccountService = studentAccountService;
		this.credentialStore = credentialStore;
		this.mailService = mailService;
		this.clock = clock;
		this.rowTx = new TransactionTemplate(txManager);
		this.rowTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	/** Import từ file upload (HTTP). */
	public ImportReport importFile(Long teacherId, Long classroomId, String filename, InputStream content,
			boolean sendEmail) {
		Classroom classroom = classroomService.requireOwned(teacherId, classroomId);
		List<RawRow> rows;
		try {
			rows = StudentFileParser.parse(filename, content);
		} catch (IOException e) {
			throw ApiException.badRequest("BAD_FILE", "Không đọc được file");
		}
		return importRows(teacherId, classroom, rows, sendEmail, (done, total) -> {
		});
	}

	/**
	 * Xử lý các dòng đã đọc. {@code progress} được gọi sau mỗi dòng với (đã xử lý, tổng) — luồng TCP dùng để gửi
	 * {@code PROGRESS n/total}.
	 */
	public ImportReport importRows(Long teacherId, Classroom classroom, List<RawRow> rows, boolean sendEmail,
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

		ImportCredentialStore.Issued issued = credentials.isEmpty() ? null
				: credentialStore.put(teacherId, classroom.getCode(), credentials);
		List<NewAccountMail> mails = credentials.stream().filter(c -> c.email() != null)
				.map(c -> new NewAccountMail(c.email(), c.fullName(), c.studentCode(), c.temporaryPassword()))
				.toList();
		boolean emailQueued = sendEmail && !mails.isEmpty();
		if (emailQueued) {
			mailService.sendNewStudentAccounts(classroom.getName(), mails);
		}

		log.info("Import lớp {}: {} dòng, tạo {}, đã có {}, bỏ qua {}, lỗi {}", classroom.getId(), rows.size(),
				counters.created, counters.existing, counters.skipped, errors.size());
		return new ImportReport(rows.size(), counters.created, counters.existing, counters.skipped, errors,
				emailQueued, issued == null ? null : issued.token(), issued == null ? null : issued.expiresAt());
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
