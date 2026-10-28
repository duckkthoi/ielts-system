package com.ltmtu.ielts.user;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.audit.AuditAction;
import com.ltmtu.ielts.audit.AuditService;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.Pageables;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.security.PasswordGenerator;
import com.ltmtu.ielts.user.dto.AdminUserDtos.CreateTeacherRequest;
import com.ltmtu.ielts.user.dto.AdminUserDtos.CreatedTeacher;
import com.ltmtu.ielts.user.dto.AdminUserDtos.TemporaryPassword;
import com.ltmtu.ielts.user.dto.AdminUserDtos.UpdateTeacherRequest;
import com.ltmtu.ielts.user.dto.AdminUserDtos.UserDetail;
import com.ltmtu.ielts.user.dto.AdminUserDtos.UserSummary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Quản trị viên quản lý tài khoản giáo viên và người dùng (UC-05, UC-06). */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

	private static final Set<String> SORTABLE = Set.of("username", "fullName", "createdAt", "lastLoginAt");
	private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

	private final UserRepository userRepository;
	private final UserSessionRepository sessionRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final AuditService audit;
	private final SessionRevoker sessionRevoker;

	@Transactional(readOnly = true)
	public Page<UserSummary> search(String keyword, Role role, Boolean active, Pageable pageable) {
		Page<User> page = userRepository.search(Pageables.likePattern(keyword), role, active,
				Pageables.sanitize(pageable, SORTABLE, DEFAULT_SORT));
		List<Long> teacherIds = page.getContent().stream().filter(u -> u.getRole() == Role.TEACHER).map(User::getId)
				.toList();
		Map<Long, Long> classCounts = countClasses(teacherIds);
		return page.map(u -> u.getRole() == Role.TEACHER
				? UserSummary.from(u).withClassCount(classCounts.getOrDefault(u.getId(), 0L))
				: UserSummary.from(u));
	}

	private Map<Long, Long> countClasses(List<Long> teacherIds) {
		Map<Long, Long> result = new HashMap<>();
		if (teacherIds.isEmpty()) {
			return result;
		}
		for (Object[] row : userRepository.countClassesByTeacherIds(teacherIds)) {
			result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
		}
		return result;
	}

	@Transactional(readOnly = true)
	public UserDetail detail(Long id) {
		User user = load(id);
		return new UserDetail(UserSummary.from(user), sessionRepository.countActive(id, clock.instant()));
	}

	/** UC-05. Tài khoản mới có mật khẩu tạm và bị bắt đổi mật khẩu ở lần đăng nhập đầu (BR-02). */
	@Transactional
	public CreatedTeacher createTeacher(AuthPrincipal admin, CreateTeacherRequest req) {
		String username = req.username().trim();
		if (userRepository.existsByUsernameIgnoreCase(username)) {
			throw ApiException.conflict("DUPLICATE", "Tên đăng nhập '" + username + "' đã tồn tại");
		}
		String password = PasswordGenerator.temporaryPassword();
		User teacher = new User();
		teacher.setUsername(username);
		teacher.setFullName(req.fullName().trim());
		teacher.setEmail(blankToNull(req.email()));
		teacher.setRole(Role.TEACHER);
		teacher.setPasswordHash(passwordEncoder.encode(password));
		teacher.setMustChangePassword(true);
		userRepository.save(teacher);
		audit.record(admin.id(), AuditAction.CREATE_TEACHER, "USER", teacher.getId(), Map.of("username", username));
		log.info("Admin tạo giáo viên {} ({})", teacher.getId(), username);
		return new CreatedTeacher(UserSummary.from(teacher), password);
	}

	@Transactional
	public UserSummary updateTeacher(Long id, UpdateTeacherRequest req) {
		User teacher = userRepository.findByIdAndRole(id, Role.TEACHER)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy giáo viên"));
		teacher.setFullName(req.fullName().trim());
		teacher.setEmail(blankToNull(req.email()));
		return UserSummary.from(teacher);
	}

	/** Khoá tài khoản: thu hồi mọi phiên ngay (token đang dùng bị từ chối ở request kế tiếp). */
	@Transactional
	public UserSummary lock(AuthPrincipal admin, Long id) {
		if (admin.id().equals(id)) {
			throw ApiException.conflict("INVALID_STATE", "Không thể tự khoá tài khoản của chính mình");
		}
		User user = load(id);
		user.setActive(false);
		int revoked = sessionRevoker.revokeAll(user, null, RevokeReason.ACCOUNT_LOCKED, clock.instant());
		audit.record(admin.id(), AuditAction.LOCK_USER, "USER", id, Map.of("revokedSessions", revoked));
		log.info("Admin {} khoá tài khoản {}, thu hồi {} phiên", admin.id(), id, revoked);
		return UserSummary.from(user);
	}

	@Transactional
	public UserSummary unlock(AuthPrincipal admin, Long id) {
		User user = load(id);
		user.setActive(true);
		audit.record(admin.id(), AuditAction.UNLOCK_USER, "USER", id, null);
		return UserSummary.from(user);
	}

	/** Đặt lại mật khẩu tạm, bắt đổi ở lần đăng nhập sau, thu hồi mọi phiên đang mở. */
	@Transactional
	public TemporaryPassword resetPassword(AuthPrincipal admin, Long id) {
		User user = load(id);
		String password = PasswordGenerator.temporaryPassword();
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setMustChangePassword(true);
		Instant now = clock.instant();
		sessionRevoker.revokeAll(user, null, RevokeReason.ADMIN_FORCED, now);
		audit.record(admin.id(), AuditAction.RESET_PASSWORD, "USER", id, null);
		log.info("Admin {} đặt lại mật khẩu cho tài khoản {}", admin.id(), id);
		return new TemporaryPassword(password);
	}

	private User load(Long id) {
		return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
