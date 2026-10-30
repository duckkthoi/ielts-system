package com.ltmtu.ielts.exam;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.TeacherAssignmentAccess;
import com.ltmtu.ielts.classroom.ClassroomService;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.exam.ExamEnums.ExamEventType;
import com.ltmtu.ielts.exam.ExamEventRepository.TypeCount;
import com.ltmtu.ielts.exam.dto.ExamDtos.EventDto;
import com.ltmtu.ielts.exam.dto.ExamDtos.ExamMonitor;
import com.ltmtu.ielts.exam.dto.ExamDtos.ExamPartRef;
import com.ltmtu.ielts.exam.dto.ExamDtos.ExamPush;
import com.ltmtu.ielts.exam.dto.ExamDtos.ExamTimeline;
import com.ltmtu.ielts.exam.dto.ExamDtos.MonitorRow;
import com.ltmtu.ielts.realtime.PresenceService;

import lombok.RequiredArgsConstructor;

/** Giáo viên giám sát chế độ thi (UC-21): danh sách phiên thi của bài, dòng thời gian một phiên. */
@Service
@RequiredArgsConstructor
public class TeacherExamService {

	/** Số sự kiện nạp sẵn cho bảng tin (bằng số client giữ, {@code FEED_LIMIT}). */
	static final int RECENT_LIMIT = 50;

	private final TeacherAssignmentAccess assignmentAccess;
	private final ClassroomService classroomService;
	private final ExamSessionRepository sessionRepository;
	private final ExamEventRepository eventRepository;
	private final PresenceService presence;
	private final Clock clock;

	/**
	 * Màn giám sát của một bài: mọi phiên thi đã bắt đầu, kèm số lần rời tab / dán / mất kết nối. Lọc theo lớp
	 * ({@code classId} không phải lớp của mình → 400). Bài của giáo viên khác → 404 (BR-15).
	 */
	@Transactional(readOnly = true)
	public ExamMonitor monitor(Long teacherId, Long assignmentId, Long classId) {
		Assignment a = assignmentAccess.requireOwned(teacherId, assignmentId);
		if (classId != null) {
			classroomService.requireOwnedClassrooms(teacherId, List.of(classId));
		}
		Instant now = clock.instant();
		List<ExamSession> sessions = sessionRepository.findForMonitor(assignmentId, classId);
		Map<Long, Map<ExamEventType, TypeCount>> counts = counts(sessions);
		List<ExamPartRef> parts = a.getParts().stream().filter(p -> p.isExamMode())
				.map(p -> new ExamPartRef(p.getId(), p.getTitle(), p.getExamDurationMinutes())).toList();
		List<MonitorRow> rows = sessions.stream()
				.map(s -> toRow(s, counts.getOrDefault(s.getId(), Map.of()), now)).toList();
		return new ExamMonitor(a.getId(), a.getTitle(), parts, rows, now);
	}

	/**
	 * Bảng tin của màn giám sát: {@link #RECENT_LIMIT} sự kiện mới nhất của bài, mới trước, cùng dạng với tin đẩy qua
	 * {@code /topic/assignments/{id}/exam} để client gộp được. Lọc lớp và quyền như {@link #monitor}.
	 */
	@Transactional(readOnly = true)
	public List<ExamPush> recentEvents(Long teacherId, Long assignmentId, Long classId) {
		assignmentAccess.requireOwned(teacherId, assignmentId);
		if (classId != null) {
			classroomService.requireOwnedClassrooms(teacherId, List.of(classId));
		}
		Instant now = clock.instant();
		return eventRepository.findRecent(assignmentId, classId, PageRequest.of(0, RECENT_LIMIT)).stream().map(e -> {
			ExamSession s = e.getSession();
			return new ExamPush(s.getId(), s.getPart().getId(), s.getStudent().getId(), s.getStudent().getFullName(),
					e.getType(), e.getPasteChars(), ExamRules.status(s.getEndsAt(), s.getFinishedAt(), now),
					e.getOccurredAt(), e.isReportedLate());
		}).toList();
	}

	/** Dòng thời gian một phiên thi, cũ trước. Phiên thuộc bài của giáo viên khác → 404. */
	@Transactional(readOnly = true)
	public ExamTimeline timeline(Long teacherId, Long sessionId) {
		ExamSession s = sessionRepository.findFull(sessionId)
				.filter(x -> x.getPart().getAssignment().getTeacher().getId().equals(teacherId))
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy phiên thi"));
		List<EventDto> events = eventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(sessionId).stream()
				.map(e -> new EventDto(e.getId(), e.getType(), e.getPasteChars(), e.getOccurredAt(), e.isReportedLate()))
				.toList();
		return new ExamTimeline(toRow(s, counts(List.of(s)).getOrDefault(s.getId(), Map.of()), clock.instant()),
				events);
	}

	private Map<Long, Map<ExamEventType, TypeCount>> counts(List<ExamSession> sessions) {
		Map<Long, Map<ExamEventType, TypeCount>> result = new HashMap<>();
		if (sessions.isEmpty()) {
			return result;
		}
		for (TypeCount c : eventRepository.countByType(sessions.stream().map(ExamSession::getId).toList())) {
			result.computeIfAbsent(c.getSessionId(), k -> new EnumMap<>(ExamEventType.class)).put(c.getType(), c);
		}
		return result;
	}

	private MonitorRow toRow(ExamSession s, Map<ExamEventType, TypeCount> counts, Instant now) {
		Instant lastAt = counts.values().stream().map(TypeCount::getLastAt).max(Instant::compareTo).orElse(null);
		return new MonitorRow(s.getId(), s.getPart().getId(), s.getPart().getTitle(), s.getStudent().getId(),
				s.getStudent().getStudentCode(), s.getStudent().getFullName(), s.getStartedAt(), s.getEndsAt(),
				s.getFinishedAt(), ExamRules.status(s.getEndsAt(), s.getFinishedAt(), now),
				presence.isOnline(s.getStudent().getId()),
				count(counts, ExamEventType.TAB_HIDDEN) + count(counts, ExamEventType.WINDOW_BLUR),
				count(counts, ExamEventType.PASTE), count(counts, ExamEventType.DISCONNECT), lastAt);
	}

	private static int count(Map<ExamEventType, TypeCount> counts, ExamEventType type) {
		TypeCount c = counts.get(type);
		return c == null ? 0 : (int) c.getTotal();
	}
}
