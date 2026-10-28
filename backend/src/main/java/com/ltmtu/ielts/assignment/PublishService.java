package com.ltmtu.ielts.assignment;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentDetail;
import com.ltmtu.ielts.assignment.dto.PublishDtos.PublishResult;
import com.ltmtu.ielts.assignment.dto.PublishDtos.RemindRequest;
import com.ltmtu.ielts.assignment.dto.PublishDtos.RemindResult;
import com.ltmtu.ielts.assignment.dto.PublishDtos.TargetClass;
import com.ltmtu.ielts.assignment.dto.PublishDtos.TargetStudent;
import com.ltmtu.ielts.assignment.dto.PublishDtos.Targets;
import com.ltmtu.ielts.assignment.dto.PublishDtos.TargetsAdded;
import com.ltmtu.ielts.assignment.dto.PublishDtos.TargetsRequest;
import com.ltmtu.ielts.audit.AuditAction;
import com.ltmtu.ielts.audit.AuditService;
import com.ltmtu.ielts.classroom.Classroom;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.user.User;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Giao bài và vòng đời bài tập sau khi soạn: phát hành, giao thêm, đóng, mở lại, nhân bản (UC-14, UC-15). */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishService {

	private final AssignmentService assignmentService;
	private final AssignmentRepository assignmentRepository;
	private final AssignmentTargetRepository targetRepository;
	private final ClassroomService classroomService;
	private final UserQueryService userQueryService;
	private final ApplicationEventPublisher events;
	private final Clock clock;
	private final RemindThrottle remindThrottle;
	private final AuditService audit;

	/**
	 * UC-14: giao bài và phát hành. Điều kiện: bài nháp, có ≥ 1 phần, mỗi phần có ≥ 1 tiêu chí, có hạn nộp ở tương
	 * lai, có ít nhất một người nhận. Mọi vấn đề được liệt kê cùng lúc để giáo viên sửa một lần.
	 */
	@Transactional
	public PublishResult publish(Long teacherId, Long id, TargetsRequest req) {
		Assignment a = assignmentService.requireOwned(teacherId, id);
		if (!a.isDraft()) {
			throw ApiException.conflict("INVALID_STATE", "Bài tập đã được phát hành trước đó");
		}
		Instant now = clock.instant();
		List<String> problems = readinessProblems(a, now);
		if (!problems.isEmpty()) {
			throw ApiException.badRequest("NOT_READY", "Chưa thể phát hành: " + String.join("; ", problems));
		}

		addTargets(teacherId, a, req, now);
		Set<Long> recipients = targetRepository.findRecipientIds(a.getId());
		if (recipients.isEmpty()) {
			// Ném lỗi → transaction rollback, các target vừa thêm cũng bị huỷ.
			throw ApiException.badRequest("NOT_READY", "Chưa thể phát hành: chưa có sinh viên nào nhận bài "
					+ "(chọn lớp có thành viên hoặc chọn từng sinh viên)");
		}
		a.setStatus(AssignmentStatus.PUBLISHED);
		a.setPublishedAt(now);
		if (opensNow(a, now)) {
			a.setOpenNotifiedAt(now); // thông báo gửi ngay theo sự kiện bên dưới; job mở bài bỏ qua bài này
		}
		events.publishEvent(new AssignmentEvents.Published(a.getId(), a.getTitle(), a.getDueAt(), Set.copyOf(recipients),
				opensNow(a, now)));
		audit.record(teacherId, AuditAction.PUBLISH_ASSIGNMENT, "ASSIGNMENT", id, Map.of("recipients", recipients.size()));
		log.info("Giáo viên {} phát hành bài tập {} cho {} sinh viên", teacherId, id, recipients.size());
		return new PublishResult(recipients.size());
	}

	/** Giao thêm lớp / sinh viên cho bài đang mở. Chỉ người nhận MỚI được thông báo (UC-14 A2). */
	@Transactional
	public TargetsAdded addMoreTargets(Long teacherId, Long id, TargetsRequest req) {
		Assignment a = assignmentService.requireOwned(teacherId, id);
		if (a.getStatus() != AssignmentStatus.PUBLISHED) {
			throw ApiException.conflict("INVALID_STATE",
					a.isDraft() ? "Bài nháp: chọn người nhận khi phát hành" : "Bài đã đóng: hãy mở lại trước khi giao thêm");
		}
		Instant now = clock.instant();
		Set<Long> before = targetRepository.findRecipientIds(id);
		addTargets(teacherId, a, req, now);
		Set<Long> added = new HashSet<>(targetRepository.findRecipientIds(id));
		int total = added.size();
		added.removeAll(before);
		if (!added.isEmpty()) {
			events.publishEvent(new AssignmentEvents.RecipientsAdded(id, a.getTitle(), a.getDueAt(), Set.copyOf(added),
					opensNow(a, now)));
		}
		return new TargetsAdded(added.size(), total);
	}

	@Transactional(readOnly = true)
	public Targets targets(Long teacherId, Long id) {
		assignmentService.requireOwned(teacherId, id);
		List<TargetClass> classes = new ArrayList<>();
		List<TargetStudent> students = new ArrayList<>();
		for (AssignmentTarget t : targetRepository.findWithDetails(id)) {
			if (t.getClassroom() != null) {
				Classroom c = t.getClassroom();
				classes.add(new TargetClass(c.getId(), c.getCode(), c.getName(), classroomService.memberCount(c.getId())));
			} else {
				User s = t.getStudent();
				students.add(new TargetStudent(s.getId(), s.getStudentCode(), s.getFullName()));
			}
		}
		return new Targets(classes, students, targetRepository.findRecipientIds(id).size());
	}

	/**
	 * UC-16: giáo viên nhắc một số sinh viên (vd từ ma trận tiến độ). Chỉ bài đang mở; sinh viên phải là người nhận bài.
	 * Thông báo do module thông báo tạo từ sự kiện {@link AssignmentEvents.ManualReminder}.
	 */
	@Transactional
	public RemindResult remind(Long teacherId, Long id, RemindRequest req) {
		Assignment a = assignmentService.requireOwned(teacherId, id);
		if (a.getStatus() != AssignmentStatus.PUBLISHED) {
			throw ApiException.conflict("INVALID_STATE", "Chỉ nhắc được bài đang mở");
		}
		AssignmentPart part = null;
		if (req.partId() != null) {
			part = a.getParts().stream().filter(p -> p.getId().equals(req.partId())).findFirst()
					.orElseThrow(() -> ApiException.badRequest("VALIDATION_ERROR", "Phần này không thuộc bài tập"));
		}
		if (!targetRepository.findRecipientIds(id).containsAll(req.studentIds())) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Có sinh viên không được giao bài này");
		}
		Duration wait = remindThrottle.blockedFor(teacherId, id);
		if (wait != null) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Bạn vừa nhắc bài này nhiều lần. Vui lòng thử lại sau "
					+ Math.max(1, (wait.toSeconds() + 59) / 60) + " phút");
		}
		Instant due = part != null && part.getPartDueAt() != null ? part.getPartDueAt() : a.getDueAt();
		events.publishEvent(new AssignmentEvents.ManualReminder(a.getId(), a.getTitle(),
				part == null ? null : part.getTitle(), due, Set.copyOf(req.studentIds())));
		remindThrottle.reminded(teacherId, id);
		log.info("Giáo viên {} nhắc {} sinh viên làm bài {}", teacherId, req.studentIds().size(), id);
		return new RemindResult(req.studentIds().size());
	}

	/** Đóng bài: sinh viên không nộp được nữa nhưng vẫn xem được bài và kết quả (docs/06 mục 1). */
	@Transactional
	public AssignmentDetail close(Long teacherId, Long id) {
		Assignment a = assignmentService.requireOwned(teacherId, id);
		if (a.getStatus() != AssignmentStatus.PUBLISHED) {
			throw ApiException.conflict("INVALID_STATE", "Chỉ đóng được bài đang mở");
		}
		a.setStatus(AssignmentStatus.CLOSED);
		return AssignmentMapper.toDetail(a);
	}

	@Transactional
	public AssignmentDetail reopen(Long teacherId, Long id) {
		Assignment a = assignmentService.requireOwned(teacherId, id);
		if (a.getStatus() != AssignmentStatus.CLOSED) {
			throw ApiException.conflict("INVALID_STATE", "Chỉ mở lại được bài đã đóng");
		}
		a.setStatus(AssignmentStatus.PUBLISHED);
		return AssignmentMapper.toDetail(a);
	}

	/**
	 * UC-15: nhân bản thành bài nháp mới gồm đủ phần + rubric. Không chép người nhận và các mốc thời gian
	 * (thường đã ở quá khứ) — giáo viên đặt lại trước khi phát hành.
	 */
	@Transactional
	public AssignmentDetail duplicate(Long teacherId, Long id) {
		Assignment src = assignmentService.requireOwned(teacherId, id);
		Assignment copy = new Assignment();
		copy.setTeacher(src.getTeacher());
		copy.setTitle(truncate(src.getTitle() + " (bản sao)", 200));
		copy.setDescription(src.getDescription());
		copy.setAllowLate(src.isAllowLate());
		copy.setMaxRevisions(src.getMaxRevisions());
		copy.setShowSuggestionsOnRevision(src.isShowSuggestionsOnRevision());
		// Bài paper: giữ loại, kiểu trích dẫn, cỡ nhóm; không chép hạn chốt nhóm (mốc thời gian) và các nhóm.
		copy.setKind(src.getKind());
		copy.setCitationStyle(src.getCitationStyle());
		copy.setMaxGroupSize(src.getMaxGroupSize());
		for (AssignmentPart p : src.getParts()) {
			AssignmentPart np = new AssignmentPart();
			np.setOrderNo(p.getOrderNo());
			np.setTitle(p.getTitle());
			np.setTaskType(p.getTaskType());
			np.setPrompt(p.getPrompt());
			np.setImageUrl(p.getImageUrl());
			np.setMinWords(p.getMinWords());
			np.setSuggestedMinutes(p.getSuggestedMinutes());
			np.setWeight(p.getWeight());
			np.setExamMode(p.isExamMode());
			np.setExamDurationMinutes(p.getExamDurationMinutes());
			for (PaperSection s : p.getSections()) {
				PaperSection ns = new PaperSection();
				ns.setOrderNo(s.getOrderNo());
				ns.setSectionKey(s.getSectionKey());
				ns.setTitle(s.getTitle());
				ns.setRequired(s.isRequired());
				ns.setMinWords(s.getMinWords());
				ns.setMaxWords(s.getMaxWords());
				np.addSection(ns);
			}
			for (RubricCriterion c : p.getCriteria()) {
				RubricCriterion nc = new RubricCriterion();
				nc.setOrderNo(c.getOrderNo());
				nc.setCode(c.getCode());
				nc.setName(c.getName());
				nc.setDescription(c.getDescription());
				nc.setMaxScore(c.getMaxScore());
				nc.setStep(c.getStep());
				nc.setWeight(c.getWeight());
				for (ChecklistItem i : c.getChecklist()) {
					ChecklistItem ni = new ChecklistItem();
					ni.setOrderNo(i.getOrderNo());
					ni.setContent(i.getContent());
					ni.setKind(i.getKind());
					ni.setMaxBand(i.getMaxBand());
					nc.addItem(ni);
				}
				for (RubricLevel l : c.getLevels()) {
					RubricLevel nl = new RubricLevel();
					nl.setOrderNo(l.getOrderNo());
					nl.setLabel(l.getLabel());
					nl.setMinScore(l.getMinScore());
					nl.setMaxScore(l.getMaxScore());
					nl.setDescriptor(l.getDescriptor());
					nc.addLevel(nl);
				}
				np.addCriterion(nc);
			}
			copy.addPart(np);
		}
		assignmentRepository.save(copy);
		log.info("Giáo viên {} nhân bản bài tập {} → {}", teacherId, id, copy.getId());
		return AssignmentMapper.toDetail(copy);
	}

	/** Liệt kê mọi lý do bài chưa phát hành được. */
	static List<String> readinessProblems(Assignment a, Instant now) {
		List<String> problems = new ArrayList<>();
		if (a.getParts().isEmpty()) {
			problems.add("bài chưa có phần nào");
		}
		for (AssignmentPart p : a.getParts()) {
			if (p.getCriteria().isEmpty()) {
				problems.add("phần " + p.getOrderNo() + " (" + p.getTitle() + ") chưa có tiêu chí chấm");
			}
			if (a.isPaper() && p.getSections().isEmpty()) {
				problems.add("phần " + p.getOrderNo() + " (" + p.getTitle() + ") chưa có khung mục");
			}
			if (p.getPartDueAt() != null && !p.getPartDueAt().isAfter(now)) {
				problems.add("hạn riêng của phần " + p.getOrderNo() + " đã qua");
			}
		}
		if (a.getDueAt() == null) {
			problems.add("chưa đặt hạn nộp");
		} else if (!a.getDueAt().isAfter(now)) {
			problems.add("hạn nộp đã qua");
		}
		return problems;
	}

	/** Thêm target cho các lớp / sinh viên chưa được giao (bỏ qua cái đã có — UNIQUE từng phần trong DB). */
	private void addTargets(Long teacherId, Assignment a, TargetsRequest req, Instant now) {
		Set<Long> classIds = req.classIds() == null ? Set.of() : req.classIds();
		Set<Long> studentIds = req.studentIds() == null ? Set.of() : req.studentIds();
		List<AssignmentTarget> existing = targetRepository.findWithDetails(a.getId());
		Set<Long> existingClasses = new HashSet<>();
		Set<Long> existingStudents = new HashSet<>();
		for (AssignmentTarget t : existing) {
			if (t.getClassroom() != null) {
				existingClasses.add(t.getClassroom().getId());
			} else {
				existingStudents.add(t.getStudent().getId());
			}
		}
		if (!classIds.isEmpty()) {
			for (Classroom c : classroomService.requireOwnedClassrooms(teacherId, classIds)) {
				if (!existingClasses.contains(c.getId())) {
					targetRepository.save(AssignmentTarget.forClass(a, c, now));
				}
			}
		}
		if (!studentIds.isEmpty()) {
			for (User s : userQueryService.requireStudents(studentIds)) {
				if (!existingStudents.contains(s.getId())) {
					targetRepository.save(AssignmentTarget.forStudent(a, s, now));
				}
			}
		}
		targetRepository.flush();
	}

	private static boolean opensNow(Assignment a, Instant now) {
		return a.getOpenAt() == null || !a.getOpenAt().isAfter(now);
	}

	private static String truncate(String s, int max) {
		return Objects.requireNonNull(s).length() <= max ? s : s.substring(0, max);
	}
}
