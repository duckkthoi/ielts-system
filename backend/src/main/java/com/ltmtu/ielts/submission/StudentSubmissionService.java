package com.ltmtu.ielts.submission;

import java.time.Clock;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.assignment.StudentAssignmentAccess;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.submission.dto.SubmissionDtos.DraftConflict;
import com.ltmtu.ielts.submission.dto.SubmissionDtos.DraftResponse;
import com.ltmtu.ielts.submission.dto.SubmissionDtos.DraftSource;
import com.ltmtu.ielts.submission.dto.SubmissionDtos.SaveDraftResponse;
import com.ltmtu.ielts.submission.dto.SubmissionDtos.SubmitResponse;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Sinh viên làm bài: nạp / lưu nháp, nộp từng phần (UC-23, UC-24, UC-26; SD-06, SD-07). */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentSubmissionService {

	private final StudentAssignmentAccess access;
	private final SubmissionRepository submissionRepository;
	private final SubmissionVersionRepository versionRepository;
	private final UserQueryService userQueryService;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	@Transactional(readOnly = true)
	public DraftResponse draft(Long studentId, Long partId) {
		access.requireVisiblePart(partId, studentId, clock.instant());
		// Bài PAPER: bản nháp chung của nhóm (BR-28). Chưa có nhóm thì chưa có gì để nạp — ghi mới bị chặn ở saveDraft.
		Submission s = submissionRepository.findMine(studentId, partId).orElse(null);
		if (s == null) {
			return new DraftResponse("", DraftSource.EMPTY, null, 0, SubmissionStatus.NOT_STARTED, 0, 0, null);
		}
		if (s.getDraftContent() != null) {
			return new DraftResponse(s.getDraftContent(), DraftSource.DRAFT, s.getDraftSavedAt(),
					WordCounter.count(s.getDraftContent()), s.getStatus().forStudent(), s.getCurrentVersion(),
					s.getDraftVersion(), s.getId());
		}
		if (s.getStatus() == SubmissionStatus.REVISION_REQUESTED && s.getCurrentVersion() > 0) {
			String last = versionRepository.findBySubmissionIdAndVersionNo(s.getId(), s.getCurrentVersion())
					.map(SubmissionVersion::getContent).orElse("");
			return new DraftResponse(last, DraftSource.LAST_VERSION, null, WordCounter.count(last), s.getStatus(),
					s.getCurrentVersion(), s.getDraftVersion(), s.getId());
		}
		return new DraftResponse("", DraftSource.EMPTY, null, 0, s.getStatus().forStudent(), s.getCurrentVersion(),
				s.getDraftVersion(), s.getId());
	}

	/**
	 * UC-23 / SD-06: lưu nháp (tự lưu sau 3 giây ngừng gõ). Chỉ khi phần còn mở và chưa quá hạn (trừ khi cho nộp muộn).
	 * Khoá dòng bài làm rồi so {@code expectedDraftVersion}: lệch (tab / máy khác vừa lưu) → 409 DRAFT_CONFLICT kèm
	 * bản trên server, không ghi đè. {@code null} = không kiểm tra.
	 */
	@Transactional
	public SaveDraftResponse saveDraft(Long studentId, Long partId, String content, Integer expectedDraftVersion) {
		Instant now = clock.instant();
		AssignmentPart part = access.requireVisiblePart(partId, studentId, now);
		Long groupId = requireGroup(part, studentId);
		Submission s = lockMine(part, studentId, groupId);
		requireWritable(part, s, now);
		requireExamStarted(part, studentId);
		int current = s == null ? 0 : s.getDraftVersion();
		if (expectedDraftVersion != null && expectedDraftVersion != current) {
			throw new ApiException(HttpStatus.CONFLICT, "DRAFT_CONFLICT",
					"Bản nháp vừa được lưu ở tab hoặc thiết bị khác. Hãy chọn giữ bản nào",
					new DraftConflict(s == null ? "" : s.getDraftContent(), current, s == null ? null : s.getDraftSavedAt()));
		}
		if (s == null) {
			s = Submission.start(part, userQueryService.requireUser(studentId));
			s.setGroupId(groupId);
		}
		boolean started = s.getStatus() == SubmissionStatus.NOT_STARTED;
		if (started) {
			s.setStatus(SubmissionStatus.DRAFT);
		}
		s.setDraftContent(content);
		s.setDraftSavedAt(now);
		s.setDraftVersion(current + 1);
		saveNew(s, "DRAFT_CONFLICT");
		if (started) {
			events.publishEvent(new SubmissionEvents.DraftStarted(s.getId(), part.getAssignment().getId(), partId,
					studentId));
		}
		return new SaveDraftResponse(now, WordCounter.count(content), s.getDraftVersion(), s.getId());
	}

	/**
	 * UC-24 / SD-07: nộp một phần. Khoá dòng bài làm ({@code FOR UPDATE}) rồi so {@code expectedVersion}: hai
	 * request đồng thời (bấm đúp, 2 tab, mạng gửi lại) chỉ tạo đúng một version. Hạn và cờ "muộn" theo giờ server.
	 */
	@Transactional
	public SubmitResponse submit(Long studentId, Long partId, String content, int expectedVersion) {
		Instant now = clock.instant();
		AssignmentPart part = access.requireVisiblePart(partId, studentId, now);
		Long groupId = requireGroup(part, studentId);
		Submission s = lockMine(part, studentId, groupId);
		int current = s == null ? 0 : s.getCurrentVersion();
		if (expectedVersion != current) {
			throw ApiException.conflict("VERSION_CONFLICT",
					"Phần này vừa được nộp ở tab hoặc thiết bị khác (version " + current + "). Hãy tải lại trang");
		}
		requireWritable(part, s, now);
		requireExamStarted(part, studentId);
		boolean late = DeadlineRules.isPastDue(DeadlineRules.effectiveDue(part, s), now);

		if (s == null) {
			s = Submission.start(part, userQueryService.requireUser(studentId));
			s.setGroupId(groupId);
		} else if (groupId != null) {
			// Bài nhóm: ghi người nộp gần nhất (BR-28).
			s.setStudent(userQueryService.requireUser(studentId));
		}
		int versionNo = current + 1;
		s.setCurrentVersion(versionNo);
		s.setStatus(SubmissionStatus.SUBMITTED);
		s.setDraftContent(null);
		s.setDraftSavedAt(null);
		saveNew(s, "VERSION_CONFLICT");
		SubmissionVersion v = versionRepository.save(SubmissionVersion.of(s, versionNo, content, now, late));

		events.publishEvent(new SubmissionEvents.Submitted(s.getId(), part.getAssignment().getId(), partId, studentId,
				part.getAssignment().getTeacher().getId(), versionNo, late, part.getAssignment().getTitle(),
				part.getTitle(), s.getStudent().getFullName()));
		log.info("Sinh viên {} nộp phần {} version {}{}", studentId, partId, versionNo, late ? " (muộn)" : "");
		return new SubmitResponse(versionNo, now, late, v.getWordCount());
	}

	/** Điều kiện để soạn / nộp: bài chưa đóng, phần còn mở với sinh viên, chưa quá hạn (hoặc cho nộp muộn). */
	private static void requireWritable(AssignmentPart part, Submission s, Instant now) {
		if (part.getAssignment().getStatus() == AssignmentStatus.CLOSED) {
			throw ApiException.conflict("INVALID_STATE", "Bài tập đã đóng, không nộp được nữa");
		}
		if (s != null && !s.getStatus().isOpenForStudent()) {
			throw ApiException.conflict("ALREADY_SUBMITTED", "Phần này đã được nộp");
		}
		if (!DeadlineRules.canStillSubmit(part, s, now)) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "DEADLINE_PASSED",
					"Đã quá hạn nộp. Liên hệ giáo viên nếu cần gia hạn");
		}
	}

	/** BR-28: bài PAPER → nhóm của sinh viên (chưa có nhóm → 409 NO_GROUP); bài luận → {@code null}. */
	private Long requireGroup(AssignmentPart part, Long studentId) {
		if (!part.getAssignment().isPaper()) {
			return null;
		}
		return submissionRepository.groupOf(part.getAssignment().getId(), studentId)
				.orElseThrow(() -> ApiException.conflict("NO_GROUP", "Bạn chưa có nhóm. Hãy vào hoặc lập nhóm trước khi viết"));
	}

	/** Khoá trước rồi mới đọc (SD-07): bài làm của nhóm nếu là bài PAPER, của chính mình nếu là bài luận. */
	private Submission lockMine(AssignmentPart part, Long studentId, Long groupId) {
		return (groupId == null ? submissionRepository.findForUpdate(part.getId(), studentId)
				: submissionRepository.findForUpdateByGroup(part.getId(), groupId)).orElse(null);
	}

	/** BR-23: phần ở chế độ thi chỉ soạn / nộp được sau khi bấm "Bắt đầu" (để đồng hồ thi tính từ lúc đó). */
	private void requireExamStarted(AssignmentPart part, Long studentId) {
		if (part.isExamMode() && !submissionRepository.examStarted(part.getId(), studentId)) {
			throw ApiException.conflict("EXAM_NOT_STARTED", "Bấm \"Bắt đầu làm bài\" để bắt đầu tính giờ trước khi viết");
		}
	}

	/**
	 * Lưu bài làm; nếu là dòng mới mà một request khác vừa tạo trước (lần lưu / nộp đầu tiên chạy đồng thời) thì
	 * UNIQUE (part, student) chặn lại → báo xung đột thay vì lỗi 500.
	 */
	/** Lưu; lần lưu đầu chạy song song (2 tab) vi phạm UNIQUE (part, student) → 409 với mã {@code conflictCode}. */
	private void saveNew(Submission s, String conflictCode) {
		try {
			submissionRepository.saveAndFlush(s);
		} catch (DataIntegrityViolationException e) {
			throw ApiException.conflict(conflictCode, "Phần này vừa được lưu ở tab hoặc thiết bị khác. Hãy tải lại trang");
		}
	}
}
