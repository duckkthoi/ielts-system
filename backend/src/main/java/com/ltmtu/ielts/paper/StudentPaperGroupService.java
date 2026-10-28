package com.ltmtu.ielts.paper;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.StudentAssignmentAccess;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.paper.dto.PaperGroupDtos.StudentGroups;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sinh viên tự lập / vào / rời nhóm trước hạn chốt nhóm (UC-34, BR-27). Bài không được giao → 404 (BR-04).
 * Mỗi người một nhóm: DB chặn bằng UNIQUE (assignment, student); thêm người thì khoá dòng nhóm trước khi đếm, để hai
 * người vào chỗ cuối cùng cùng lúc chỉ một người được.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentPaperGroupService {

	private final StudentAssignmentAccess assignmentAccess;
	private final UserQueryService userQueryService;
	private final PaperGroupRepository groupRepository;
	private final PaperGroupMemberRepository memberRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public StudentGroups groups(Long studentId, Long assignmentId) {
		return view(requirePaper(studentId, assignmentId), studentId);
	}

	@Transactional
	public StudentGroups create(Long studentId, Long assignmentId, String rawName) {
		Assignment a = requireChangeable(studentId, assignmentId);
		requireNoGroup(assignmentId, studentId);
		String name = rawName.trim();
		if (groupRepository.existsByAssignmentIdAndName(assignmentId, name)) {
			throw ApiException.conflict("DUPLICATE", "Đã có nhóm tên \"" + name + "\"");
		}
		PaperGroup g = new PaperGroup();
		g.setAssignment(a);
		g.setName(name);
		g.setCreatedBy(userQueryService.requireUser(studentId));
		g.setCreatedAt(clock.instant());
		try {
			groupRepository.saveAndFlush(g);
		} catch (DataIntegrityViolationException e) {
			throw ApiException.conflict("DUPLICATE", "Đã có nhóm tên \"" + name + "\"");
		}
		addMember(g, studentId);
		log.info("Sinh viên {} lập nhóm {} cho bài {}", studentId, g.getId(), assignmentId);
		return view(a, studentId);
	}

	@Transactional
	public StudentGroups join(Long studentId, Long assignmentId, Long groupId) {
		Assignment a = requireChangeable(studentId, assignmentId);
		groupRepository.findInAssignment(groupId, assignmentId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy nhóm"));
		PaperGroup g = groupRepository.findForUpdate(groupId).orElseThrow();
		requireNoGroup(assignmentId, studentId);
		if (memberRepository.countByIdGroupId(groupId) >= a.getMaxGroupSize()) {
			throw ApiException.conflict("GROUP_FULL", "Nhóm \"" + g.getName() + "\" đã đủ " + a.getMaxGroupSize() + " người");
		}
		addMember(g, studentId);
		return view(a, studentId);
	}

	/** Rời nhóm; nhóm sinh viên tự lập không còn ai thì xoá. */
	@Transactional
	public StudentGroups leave(Long studentId, Long assignmentId) {
		Assignment a = requireChangeable(studentId, assignmentId);
		PaperGroupMember m = memberRepository.findByAssignmentIdAndIdStudentId(assignmentId, studentId)
				.orElseThrow(() -> ApiException.notFound("Bạn chưa có nhóm"));
		PaperGroup g = m.getGroup();
		if (groupRepository.hasWork(g.getId())) {
			throw ApiException.conflict("INVALID_STATE",
					"Nhóm đã có bài làm, không tự rời được. Liên hệ giáo viên nếu cần đổi nhóm");
		}
		memberRepository.delete(m);
		memberRepository.flush();
		if (g.isStudentMade() && memberRepository.countByIdGroupId(g.getId()) == 0) {
			groupRepository.delete(g);
			groupRepository.flush();
		}
		return view(a, studentId);
	}

	// ─── Nội bộ ──────────────────────────────────────────────────────────────

	private Assignment requirePaper(Long studentId, Long assignmentId) {
		Assignment a = assignmentAccess.requireVisibleAssignment(assignmentId, studentId, clock.instant());
		if (!a.isPaper()) {
			throw ApiException.conflict("INVALID_STATE", "Bài này không làm theo nhóm");
		}
		return a;
	}

	/** Còn tự đổi nhóm được: bài chưa đóng và chưa tới hạn chốt nhóm (BR-27). */
	private Assignment requireChangeable(Long studentId, Long assignmentId) {
		Assignment a = requirePaper(studentId, assignmentId);
		if (a.getStatus() == AssignmentStatus.CLOSED) {
			throw ApiException.conflict("INVALID_STATE", "Bài tập đã đóng");
		}
		if (locked(a)) {
			throw ApiException.conflict("GROUP_LOCKED", "Đã qua hạn chốt nhóm. Liên hệ giáo viên nếu cần đổi nhóm");
		}
		return a;
	}

	private boolean locked(Assignment a) {
		Instant lockAt = a.effectiveGroupLockAt();
		return lockAt != null && !clock.instant().isBefore(lockAt);
	}

	private void requireNoGroup(Long assignmentId, Long studentId) {
		if (memberRepository.findByAssignmentIdAndIdStudentId(assignmentId, studentId).isPresent()) {
			throw ApiException.conflict("ALREADY_IN_GROUP", "Bạn đã ở trong một nhóm. Hãy rời nhóm đó trước");
		}
	}

	private void addMember(PaperGroup g, Long studentId) {
		try {
			memberRepository.saveAndFlush(
					new PaperGroupMember(g, userQueryService.requireUser(studentId), clock.instant()));
		} catch (DataIntegrityViolationException e) {
			// Cùng lúc vào hai nhóm (2 tab): UNIQUE (assignment, student) chặn lần thứ hai.
			throw ApiException.conflict("ALREADY_IN_GROUP", "Bạn đã ở trong một nhóm. Hãy rời nhóm đó trước");
		}
	}

	private StudentGroups view(Assignment a, Long studentId) {
		Long mine = memberRepository.findByAssignmentIdAndIdStudentId(a.getId(), studentId)
				.map(m -> m.getId().getGroupId()).orElse(null);
		boolean canChange = a.getStatus() != AssignmentStatus.CLOSED && !locked(a);
		return new StudentGroups(a.getId(), mine, canChange, a.getMaxGroupSize(), a.effectiveGroupLockAt(),
				PaperGroupViews.groups(groupRepository.findByAssignment(a.getId()),
						memberRepository.findByAssignment(a.getId())));
	}
}
