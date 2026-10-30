package com.ltmtu.ielts.exam;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.assignment.StudentAssignmentAccess;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.exam.ExamEnums.ExamEventType;
import com.ltmtu.ielts.exam.dto.ExamDtos.ExamSessionDto;
import com.ltmtu.ielts.submission.DeadlineRules;
import com.ltmtu.ielts.submission.StudentSubmissionAccess;
import com.ltmtu.ielts.submission.Submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Sinh viên làm bài ở chế độ thi (UC-27): bắt đầu tính giờ, xem phiên thi của mình. */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentExamService {

	private final StudentAssignmentAccess assignmentAccess;
	private final StudentSubmissionAccess submissionAccess;
	private final ExamSessionRepository sessionRepository;
	private final ExamEventService eventService;
	private final Clock clock;

	/** Phiên thi của tôi cho một phần; chưa bắt đầu → {@code null}. Phần không được thấy → 404. */
	@Transactional(readOnly = true)
	public ExamSessionDto current(Long studentId, Long partId) {
		assignmentAccess.requireVisiblePart(partId, studentId, clock.instant());
		return sessionRepository.findByPartIdAndStudentId(partId, studentId).map(s -> toDto(s, clock.instant()))
				.orElse(null);
	}

	/**
	 * BR-23: bấm "Bắt đầu" → {@code ends_at = now + thời lượng}, cố định. Gọi lại (tải lại trang, tab khác, bấm đúp)
	 * trả về đúng phiên cũ, đồng hồ không đặt lại. Chỉ bắt đầu được khi còn soạn được: bài chưa đóng, phần chưa nộp,
	 * chưa quá hạn (hoặc cho nộp muộn).
	 */
	@Transactional
	public ExamSessionDto start(Long studentId, Long partId) {
		Instant now = clock.instant();
		AssignmentPart part = assignmentAccess.requireVisiblePart(partId, studentId, now);
		if (!part.isExamMode()) {
			throw ApiException.conflict("INVALID_STATE", "Phần này không làm theo chế độ thi");
		}
		ExamSession existing = sessionRepository.findByPartIdAndStudentId(partId, studentId).orElse(null);
		if (existing != null) {
			return toDto(existing, now);
		}
		requireCanStart(part, submissionAccess.mine(studentId, partId).orElse(null), now);
		Instant endsAt = now.plus(Duration.ofMinutes(part.getExamDurationMinutes()));
		if (sessionRepository.insertIfAbsent(partId, studentId, now, endsAt) == 1) {
			ExamSession created = sessionRepository.findFull(partId, studentId).orElseThrow();
			eventService.record(created, ExamEventType.STARTED, null, now, false, now);
			log.info("Sinh viên {} bắt đầu thi phần {} (phiên {})", studentId, partId, created.getId());
			return toDto(created, now);
		}
		// Một request khác vừa tạo phiên trước: trả phiên đó.
		return toDto(sessionRepository.findByPartIdAndStudentId(partId, studentId).orElseThrow(), now);
	}

	private static void requireCanStart(AssignmentPart part, Submission s, Instant now) {
		if (part.getAssignment().getStatus() == AssignmentStatus.CLOSED) {
			throw ApiException.conflict("INVALID_STATE", "Bài tập đã đóng, không làm được nữa");
		}
		if (s != null && !s.getStatus().isOpenForStudent()) {
			throw ApiException.conflict("ALREADY_SUBMITTED", "Phần này đã được nộp");
		}
		if (!DeadlineRules.canStillSubmit(part, s, now)) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "DEADLINE_PASSED",
					"Đã quá hạn nộp. Liên hệ giáo viên nếu cần gia hạn");
		}
	}

	private static ExamSessionDto toDto(ExamSession s, Instant now) {
		return new ExamSessionDto(s.getId(), s.getPart().getId(), s.getStartedAt(), s.getEndsAt(), s.getFinishedAt(),
				ExamRules.status(s.getEndsAt(), s.getFinishedAt(), now), now);
	}
}
