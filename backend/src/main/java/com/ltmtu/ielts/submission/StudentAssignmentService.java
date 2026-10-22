package com.ltmtu.ielts.submission;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.assignment.StudentAssignmentAccess;
import com.ltmtu.ielts.submission.dto.StudentAssignmentDtos.AssignmentDetail;
import com.ltmtu.ielts.submission.dto.StudentAssignmentDtos.AssignmentItem;
import com.ltmtu.ielts.submission.dto.StudentAssignmentDtos.AssignmentList;
import com.ltmtu.ielts.submission.dto.StudentAssignmentDtos.ChecklistItemView;
import com.ltmtu.ielts.submission.dto.StudentAssignmentDtos.CriterionView;
import com.ltmtu.ielts.submission.dto.StudentAssignmentDtos.PartDetail;
import com.ltmtu.ielts.submission.dto.StudentAssignmentDtos.PartStatus;
import com.ltmtu.ielts.submission.dto.StudentAssignmentDtos.Tab;

import lombok.RequiredArgsConstructor;

/** Màn "Bài được giao" và chi tiết bài phía sinh viên (UC-22, docs/05-api.md A.8 — quy tắc tab). */
@Service
@RequiredArgsConstructor
public class StudentAssignmentService {

	static final Duration DUE_SOON = Duration.ofHours(24);

	private final StudentAssignmentAccess access;
	private final SubmissionRepository submissionRepository;
	private final SubmissionVersionRepository versionRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public AssignmentList list(Long studentId, Tab filter) {
		Instant now = clock.instant();
		List<Assignment> assignments = access.visibleAssignments(studentId, now);
		Progress progress = loadProgress(studentId, assignments.stream().flatMap(a -> a.getParts().stream()).toList());

		Map<Tab, Integer> counts = new EnumMap<>(Tab.class);
		for (Tab t : Tab.values()) {
			counts.put(t, 0);
		}
		List<AssignmentItem> items = assignments.stream()
				.map(a -> toItem(a, progress, now))
				.peek(item -> item.tabs().forEach(t -> counts.merge(t, 1, Integer::sum)))
				.filter(item -> filter == null || item.tabs().contains(filter))
				.sorted(Comparator.comparing(AssignmentItem::nextDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(AssignmentItem::id))
				.toList();
		return new AssignmentList(counts, items, now);
	}

	@Transactional(readOnly = true)
	public AssignmentDetail detail(Long studentId, Long assignmentId) {
		Instant now = clock.instant();
		Assignment a = access.requireVisibleAssignment(assignmentId, studentId, now);
		Progress progress = loadProgress(studentId, a.getParts());
		List<PartStatus> statuses = a.getParts().stream().map(p -> partStatus(p, progress, now)).toList();
		List<PartDetail> parts = a.getParts().stream().map(p -> {
			PartStatus st = partStatus(p, progress, now);
			Submission s = progress.submissions().get(p.getId());
			boolean revising = s != null && s.getStatus() == SubmissionStatus.REVISION_REQUESTED;
			return new PartDetail(p.getId(), p.getOrderNo(), p.getTitle(), p.getTaskType(), p.getPrompt(),
					p.getImageUrl(), p.getMinWords(), p.getSuggestedMinutes(), st.effectiveDueAt(), st.status(),
					st.currentVersion(), st.late(), st.canSubmit(), revising ? s.getRevisionNote() : null,
					revising ? s.getRevisionDueAt() : null,
					p.getCriteria().stream().map(c -> new CriterionView(c.getCode(), c.getName(), c.getDescription(),
							c.getMaxScore(), c.getStep(), c.getChecklist().stream()
									.map(i -> new ChecklistItemView(i.getContent(), i.getKind(), i.getMaxBand()))
									.toList()))
							.toList());
		}).toList();
		return new AssignmentDetail(a.getId(), a.getTitle(), a.getDescription(), a.getTeacher().getFullName(),
				a.getStatus(), a.getOpenAt(), a.getDueAt(), a.isAllowLate(), a.getMaxRevisions(),
				tabs(a, statuses, now), parts, now);
	}

	// ─── Tính trạng thái ─────────────────────────────────────────────────────

	private record Progress(Map<Long, Submission> submissions, Map<Long, SubmissionVersion> latestBySubmission) {
	}

	/** Một truy vấn cho mọi bài làm + một truy vấn cho version mới nhất (tránh N+1). */
	private Progress loadProgress(Long studentId, List<AssignmentPart> parts) {
		if (parts.isEmpty()) {
			return new Progress(Map.of(), Map.of());
		}
		Map<Long, Submission> byPart = submissionRepository
				.findByStudentIdAndPartIdIn(studentId, parts.stream().map(AssignmentPart::getId).toList()).stream()
				.collect(Collectors.toMap(s -> s.getPart().getId(), Function.identity()));
		List<Long> submitted = byPart.values().stream().filter(s -> s.getCurrentVersion() > 0).map(Submission::getId)
				.toList();
		Map<Long, SubmissionVersion> latest = submitted.isEmpty() ? Map.of()
				: versionRepository.findLatest(submitted).stream()
						.collect(Collectors.toMap(v -> v.getSubmission().getId(), Function.identity()));
		return new Progress(byPart, latest);
	}

	private PartStatus partStatus(AssignmentPart p, Progress progress, Instant now) {
		Submission s = progress.submissions().get(p.getId());
		SubmissionStatus status = s == null ? SubmissionStatus.NOT_STARTED : s.getStatus();
		SubmissionVersion latest = s == null ? null : progress.latestBySubmission().get(s.getId());
		boolean canSubmit = p.getAssignment().getStatus() == AssignmentStatus.PUBLISHED && status.isOpenForStudent()
				&& DeadlineRules.canStillSubmit(p, s, now);
		return new PartStatus(p.getId(), p.getOrderNo(), p.getTitle(), p.getTaskType(), status.forStudent(),
				s == null ? 0 : s.getCurrentVersion(), latest != null && latest.isLate(), DeadlineRules.effectiveDue(p, s),
				canSubmit);
	}

	private AssignmentItem toItem(Assignment a, Progress progress, Instant now) {
		List<PartStatus> parts = a.getParts().stream().map(p -> partStatus(p, progress, now)).toList();
		Instant nextDue = parts.stream().filter(p -> p.status().isOpenForStudent()).map(PartStatus::effectiveDueAt)
				.filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
		return new AssignmentItem(a.getId(), a.getTitle(), a.getTeacher().getFullName(), a.getStatus(), a.getOpenAt(),
				a.getDueAt(), nextDue, tabs(a, parts, now), parts);
	}

	/** Quy tắc tab — docs/05-api.md A.8. */
	static Set<Tab> tabs(Assignment a, List<PartStatus> parts, Instant now) {
		Set<Tab> tabs = EnumSet.noneOf(Tab.class);
		boolean open = a.getStatus() == AssignmentStatus.PUBLISHED;
		boolean anyPending = false;
		for (PartStatus p : parts) {
			if (p.status() == SubmissionStatus.REVISION_REQUESTED) {
				tabs.add(Tab.REVISION);
				anyPending = true;
			} else if (p.status().isOpenForStudent()) {
				anyPending = true;
				boolean pastDue = DeadlineRules.isPastDue(p.effectiveDueAt(), now);
				if (!open || pastDue) {
					tabs.add(Tab.OVERDUE);
				} else {
					tabs.add(Tab.TODO);
					if (p.effectiveDueAt() != null && !p.effectiveDueAt().isAfter(now.plus(DUE_SOON))) {
						tabs.add(Tab.DUE_SOON);
					}
				}
			}
		}
		if (!anyPending) {
			tabs.add(Tab.SUBMITTED);
		}
		return tabs;
	}
}
