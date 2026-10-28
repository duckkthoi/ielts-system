package com.ltmtu.ielts.classroom;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.classroom.dto.ClassroomDtos.AddMembersResult;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.ClassroomRequest;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.ClassroomSummary;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.MemberDto;
import com.ltmtu.ielts.classroom.dto.ClassroomDtos.StudentOption;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.Pageables;
import com.ltmtu.ielts.user.User;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Lớp học và thành viên (UC-10). Giáo viên chỉ thấy và thao tác được lớp của mình (BR-15). */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomService {

	private static final Map<String, String> CLASS_SORT = Map.of("name", "name", "code", "code", "createdAt",
			"createdAt");
	private static final Map<String, String> MEMBER_SORT = Map.of("studentCode", "student.studentCode", "fullName",
			"student.fullName", "joinedAt", "joinedAt");
	private static final int STUDENT_SEARCH_LIMIT = 20;

	private final ClassroomRepository classroomRepository;
	private final ClassMemberRepository memberRepository;
	private final UserQueryService userQueryService;
	private final Clock clock;

	@Transactional(readOnly = true)
	public Page<ClassroomSummary> search(Long teacherId, String keyword, Pageable pageable) {
		return classroomRepository.searchOfTeacher(teacherId, Pageables.likePattern(keyword),
				Pageables.sanitize(pageable, CLASS_SORT, Sort.by("name")));
	}

	@Transactional(readOnly = true)
	public ClassroomSummary detail(Long teacherId, Long classroomId) {
		return toSummary(requireOwned(teacherId, classroomId));
	}

	@Transactional
	public ClassroomSummary create(Long teacherId, ClassroomRequest req) {
		String code = normalizeCode(req.code());
		if (classroomRepository.existsByCode(code)) {
			throw ApiException.conflict("DUPLICATE", "Mã lớp '" + code + "' đã tồn tại");
		}
		Classroom c = new Classroom();
		c.setCode(code);
		c.setTeacher(userQueryService.requireUser(teacherId));
		apply(c, req);
		classroomRepository.save(c);
		log.info("Giáo viên {} tạo lớp {} ({})", teacherId, c.getId(), code);
		return toSummary(c);
	}

	@Transactional
	public ClassroomSummary update(Long teacherId, Long classroomId, ClassroomRequest req) {
		Classroom c = requireOwned(teacherId, classroomId);
		String code = normalizeCode(req.code());
		if (!code.equals(c.getCode()) && classroomRepository.existsByCode(code)) {
			throw ApiException.conflict("DUPLICATE", "Mã lớp '" + code + "' đã tồn tại");
		}
		c.setCode(code);
		apply(c, req);
		return toSummary(c);
	}

	/** Xoá lớp: thành viên và các lần giao bài cho lớp bị xoá theo (FK cascade); tài khoản sinh viên giữ nguyên. */
	@Transactional
	public void delete(Long teacherId, Long classroomId) {
		classroomRepository.delete(requireOwned(teacherId, classroomId));
		log.info("Giáo viên {} xoá lớp {}", teacherId, classroomId);
	}

	@Transactional(readOnly = true)
	public Page<MemberDto> members(Long teacherId, Long classroomId, String keyword, Pageable pageable) {
		requireOwned(teacherId, classroomId);
		return memberRepository.searchMembers(classroomId, Pageables.likePattern(keyword),
				Pageables.sanitize(pageable, MEMBER_SORT, Sort.by("student.studentCode")));
	}

	/** Thêm sinh viên <b>đã có tài khoản</b> theo mã SV. Mã không tồn tại được trả về trong {@code notFound}. */
	@Transactional
	public AddMembersResult addMembers(Long teacherId, Long classroomId, Collection<String> studentCodes) {
		Classroom c = requireOwned(teacherId, classroomId);
		Set<String> codes = new LinkedHashSet<>();
		for (String code : studentCodes) {
			codes.add(code.trim());
		}
		List<User> students = userQueryService.findStudentsByCodes(codes);
		Set<Long> existing = students.isEmpty() ? Set.of()
				: memberRepository.findMemberIds(classroomId, students.stream().map(User::getId).toList());

		Instant now = clock.instant();
		int added = 0;
		for (User s : students) {
			if (!existing.contains(s.getId())) {
				memberRepository.save(new ClassMember(c, s, now));
				added++;
			}
		}
		Set<String> found = new LinkedHashSet<>();
		students.forEach(s -> found.add(s.getStudentCode().toUpperCase()));
		List<String> notFound = codes.stream().filter(code -> !found.contains(code.toUpperCase())).toList();
		return new AddMembersResult(added, existing.size(), notFound);
	}

	@Transactional
	public void removeMember(Long teacherId, Long classroomId, Long studentId) {
		requireOwned(teacherId, classroomId);
		ClassMember.Id id = new ClassMember.Id(classroomId, studentId);
		if (!memberRepository.existsById(id)) {
			throw ApiException.notFound("Sinh viên không thuộc lớp này");
		}
		memberRepository.deleteById(id);
	}

	@Transactional(readOnly = true)
	public List<StudentOption> searchStudents(String keyword) {
		return userQueryService.searchStudents(keyword, STUDENT_SEARCH_LIMIT).stream()
				.map(u -> new StudentOption(u.getId(), u.getStudentCode(), u.getFullName()))
				.toList();
	}

	/**
	 * Các lớp của giáo viên theo id (dùng khi giao bài). Có id không tồn tại hoặc thuộc giáo viên khác → 400,
	 * không chỉ rõ id nào thuộc người khác.
	 */
	@Transactional(readOnly = true)
	public List<Classroom> requireOwnedClassrooms(Long teacherId, Collection<Long> classroomIds) {
		List<Classroom> found = classroomRepository.findAllById(classroomIds).stream()
				.filter(c -> c.getTeacher().getId().equals(teacherId)).toList();
		if (found.size() != classroomIds.size()) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Có lớp không tồn tại hoặc không thuộc quyền quản lý của bạn");
		}
		return found;
	}

	@Transactional(readOnly = true)
	public long memberCount(Long classroomId) {
		return memberRepository.countByClassroomId(classroomId);
	}

	/** Lớp của giáo viên khác → 404, không lộ việc lớp đó tồn tại (docs/05-api.md A.1). */
	Classroom requireOwned(Long teacherId, Long classroomId) {
		return classroomRepository.findByIdAndTeacherId(classroomId, teacherId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy lớp"));
	}

	private ClassroomSummary toSummary(Classroom c) {
		return new ClassroomSummary(c.getId(), c.getCode(), c.getName(), c.getDescription(),
				memberRepository.countByClassroomId(c.getId()), classroomRepository.countOpenAssignments(c.getId()),
				c.getCreatedAt());
	}

	private static void apply(Classroom c, ClassroomRequest req) {
		c.setName(req.name().trim());
		c.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
	}

	/** Mã lớp lưu dạng viết hoa để so khớp không phân biệt hoa thường (vd lệnh TCP). */
	static String normalizeCode(String code) {
		return code.trim().toUpperCase();
	}
}
