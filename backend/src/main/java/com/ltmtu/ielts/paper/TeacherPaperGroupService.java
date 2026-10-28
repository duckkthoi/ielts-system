package com.ltmtu.ielts.paper;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.TeacherAssignmentAccess;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.paper.dto.PaperGroupDtos.GroupRequest;
import com.ltmtu.ielts.paper.dto.PaperGroupDtos.StudentRef;
import com.ltmtu.ielts.paper.dto.PaperGroupDtos.TeacherGroups;
import com.ltmtu.ielts.user.User;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Giáo viên chia nhóm cho bài PAPER (UC-34, BR-27). Giáo viên không bị hạn chốt nhóm và có thể chuyển sinh viên
 * giữa các nhóm: xếp một sinh viên vào nhóm này thì tự rời nhóm cũ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherPaperGroupService {

	private final TeacherAssignmentAccess assignmentAccess;
	private final ClassroomService classroomService;
	private final UserQueryService userQueryService;
	private final PaperGroupRepository groupRepository;
	private final PaperGroupMemberRepository memberRepository;
	private final Clock clock;
	private final Random random = new SecureRandom();

	@Transactional(readOnly = true)
	public TeacherGroups groups(Long teacherId, Long assignmentId) {
		return view(requirePaper(teacherId, assignmentId));
	}

	/** Tạo nhóm với các thành viên cho trước (có thể rỗng). */
	@Transactional
	public TeacherGroups create(Long teacherId, Long assignmentId, GroupRequest req) {
		Assignment a = requirePaper(teacherId, assignmentId);
		PaperGroup g = newGroup(a, req.name().trim(), userQueryService.requireUser(teacherId));
		setMembers(a, g, req.studentIds());
		log.info("Giáo viên {} tạo nhóm {} cho bài {}", teacherId, g.getId(), assignmentId);
		return view(a);
	}

	/** PUT: đổi tên và thay toàn bộ thành viên. */
	@Transactional
	public TeacherGroups update(Long teacherId, Long assignmentId, Long groupId, GroupRequest req) {
		Assignment a = requirePaper(teacherId, assignmentId);
		PaperGroup g = requireGroup(groupId, assignmentId);
		String name = req.name().trim();
		if (!name.equals(g.getName())) {
			requireFreeName(assignmentId, name);
			g.setName(name);
		}
		setMembers(a, g, req.studentIds());
		return view(a);
	}

	@Transactional
	public TeacherGroups delete(Long teacherId, Long assignmentId, Long groupId) {
		Assignment a = requirePaper(teacherId, assignmentId);
		PaperGroup g = requireGroup(groupId, assignmentId);
		if (groupRepository.hasWork(groupId)) {
			throw ApiException.conflict("INVALID_STATE", "Nhóm \"" + g.getName() + "\" đã có bài làm, không xoá được");
		}
		groupRepository.delete(g);
		groupRepository.flush();
		return view(a);
	}

	/**
	 * Chia ngẫu nhiên người nhận bài <b>chưa có nhóm</b> (trong một lớp nếu có {@code classId}) thành các nhóm mới, đều
	 * nhau và không quá cỡ nhóm. Nhóm đang có giữ nguyên.
	 */
	@Transactional
	public TeacherGroups autoSplit(Long teacherId, Long assignmentId, Long classId) {
		Assignment a = requirePaper(teacherId, assignmentId);
		Set<Long> pool = ungroupedIds(a);
		if (classId != null) {
			classroomService.requireOwnedClassrooms(teacherId, List.of(classId));
			pool.retainAll(memberRepository.classMemberIds(classId));
		}
		Set<String> names = groupRepository.findByAssignment(assignmentId).stream().map(PaperGroup::getName)
				.collect(Collectors.toCollection(HashSet::new));
		User teacher = userQueryService.requireUser(teacherId);
		List<List<Long>> split = PaperGroupRules.split(pool, a.getMaxGroupSize(), random);
		for (List<Long> ids : split) {
			String name = PaperGroupRules.nextName(names);
			names.add(name);
			PaperGroup g = newGroup(a, name, teacher);
			addMembers(g, userQueryService.requireStudents(ids));
		}
		log.info("Giáo viên {} chia ngẫu nhiên {} người thành {} nhóm cho bài {}", teacherId, pool.size(), split.size(),
				assignmentId);
		return view(a);
	}

	// ─── Nội bộ ──────────────────────────────────────────────────────────────

	private Assignment requirePaper(Long teacherId, Long assignmentId) {
		Assignment a = assignmentAccess.requireOwned(teacherId, assignmentId);
		if (!a.isPaper()) {
			throw ApiException.conflict("INVALID_STATE", "Bài này không làm theo nhóm");
		}
		if (a.isDraft()) {
			throw ApiException.conflict("INVALID_STATE", "Hãy phát hành bài trước khi chia nhóm");
		}
		return a;
	}

	private PaperGroup requireGroup(Long groupId, Long assignmentId) {
		return groupRepository.findInAssignment(groupId, assignmentId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy nhóm"));
	}

	private void requireFreeName(Long assignmentId, String name) {
		if (groupRepository.existsByAssignmentIdAndName(assignmentId, name)) {
			throw ApiException.conflict("DUPLICATE", "Đã có nhóm tên \"" + name + "\"");
		}
	}

	private PaperGroup newGroup(Assignment a, String name, User createdBy) {
		requireFreeName(a.getId(), name);
		PaperGroup g = new PaperGroup();
		g.setAssignment(a);
		g.setName(name);
		g.setCreatedBy(createdBy);
		g.setCreatedAt(clock.instant());
		try {
			return groupRepository.saveAndFlush(g);
		} catch (DataIntegrityViolationException e) {
			throw ApiException.conflict("DUPLICATE", "Đã có nhóm tên \"" + name + "\"");
		}
	}

	/**
	 * Thay toàn bộ thành viên của nhóm. Mọi người phải là người nhận bài, không quá cỡ nhóm; ai đang ở nhóm khác thì
	 * được chuyển sang (nhóm sinh viên tự lập bị bỏ trống thì xoá).
	 */
	private void setMembers(Assignment a, PaperGroup g, Set<Long> requested) {
		Set<Long> ids = requested == null ? Set.of() : requested;
		if (ids.size() > a.getMaxGroupSize()) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Nhóm tối đa " + a.getMaxGroupSize() + " người");
		}
		Set<Long> recipients = assignmentAccess.recipientIds(a.getId());
		if (!recipients.containsAll(ids)) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Có sinh viên không được giao bài này");
		}
		Set<Long> touched = memberRepository.findByAssignment(a.getId()).stream()
				.filter(m -> ids.contains(m.getStudent().getId()))
				.map(m -> m.getId().getGroupId()).collect(Collectors.toSet());
		memberRepository.deleteByGroup(g.getId());
		if (!ids.isEmpty()) {
			memberRepository.deleteByAssignmentAndStudents(a.getId(), ids);
		}
		memberRepository.flush();
		addMembers(g, userQueryService.requireStudents(ids));
		touched.remove(g.getId());
		for (Long otherId : touched) {
			groupRepository.findById(otherId)
					.filter(o -> o.isStudentMade() && memberRepository.countByIdGroupId(otherId) == 0)
					.ifPresent(groupRepository::delete);
		}
		groupRepository.flush();
	}

	private void addMembers(PaperGroup g, List<User> students) {
		Instant now = clock.instant();
		for (User s : students) {
			memberRepository.save(new PaperGroupMember(g, s, now));
		}
		memberRepository.flush();
	}

	private Set<Long> ungroupedIds(Assignment a) {
		Set<Long> ids = new HashSet<>(assignmentAccess.recipientIds(a.getId()));
		memberRepository.findByAssignment(a.getId()).forEach(m -> ids.remove(m.getStudent().getId()));
		return ids;
	}

	private TeacherGroups view(Assignment a) {
		List<PaperGroup> groups = groupRepository.findByAssignment(a.getId());
		List<PaperGroupMember> members = memberRepository.findByAssignment(a.getId());
		List<StudentRef> ungrouped = userQueryService.findAllByIds(ungroupedIds(a)).stream()
				.sorted((x, y) -> String.valueOf(x.getStudentCode()).compareTo(String.valueOf(y.getStudentCode())))
				.map(u -> new StudentRef(u.getId(), u.getStudentCode(), u.getFullName())).toList();
		Instant lockAt = a.effectiveGroupLockAt();
		return new TeacherGroups(a.getId(), a.getMaxGroupSize(), lockAt,
				lockAt != null && !clock.instant().isBefore(lockAt), PaperGroupViews.groups(groups, members), ungrouped);
	}
}
