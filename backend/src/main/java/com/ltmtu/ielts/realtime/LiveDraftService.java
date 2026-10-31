package com.ltmtu.ielts.realtime;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.assignment.StudentAssignmentAccess;
import com.ltmtu.ielts.assignment.TeacherAssignmentAccess;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.SlidingWindowLimiter;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.realtime.RealtimeDtos.LiveEvent;
import com.ltmtu.ielts.realtime.RealtimeDtos.LiveSnapshot;
import com.ltmtu.ielts.realtime.RealtimeDtos.LiveSource;
import com.ltmtu.ielts.realtime.RealtimeDtos.LiveType;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.submission.StudentSubmissionAccess;
import com.ltmtu.ielts.submission.Submission;
import com.ltmtu.ielts.submission.SubmissionEvents;
import com.ltmtu.ielts.submission.SubmissionStatus;
import com.ltmtu.ielts.submission.TeacherSubmissionAccess;
import com.ltmtu.ielts.submission.WordCounter;

import lombok.extern.slf4j.Slf4j;

/**
 * Giáo viên xem trực tiếp bài sinh viên đang gõ (BR-26, B15b). Client sinh viên gửi toàn bộ nội dung ô soạn
 * ({@code /app/live/{partId}/content}, ~1 lần / giây khi đang gõ); server giữ bản mới nhất <b>trong bộ nhớ</b> (không
 * ghi DB — bản nháp vẫn đi đường tự lưu như cũ) và phát tới {@code /topic/live/parts/{partId}/students/{studentId}},
 * kênh chỉ giáo viên chủ bài nghe được ({@link LiveChannelRule}). Không log nội dung bài.
 * <p>
 * Bình luận trực tiếp không cần kênh mới: dùng kênh chat {@code submission-{id}} (đã có, dùng được cả khi bài còn nháp).
 */
@Slf4j
@Service
public class LiveDraftService {

	/** Bằng giới hạn của {@code PUT …/draft}. */
	private static final int MAX_CHARS = 30_000;

	private record Live(String content, int wordCount, Instant at) {
	}

	/** {@code partId:studentId} → nội dung mới nhất. Xoá khi sinh viên nộp. */
	private final Map<String, Live> drafts = new ConcurrentHashMap<>();

	private final StudentAssignmentAccess studentAssignmentAccess;
	private final StudentSubmissionAccess studentSubmissionAccess;
	private final TeacherAssignmentAccess teacherAssignmentAccess;
	private final TeacherSubmissionAccess teacherSubmissionAccess;
	private final PresenceService presence;
	private final SimpMessagingTemplate messaging;
	private final SlidingWindowLimiter limiter;
	private final Clock clock;

	public LiveDraftService(StudentAssignmentAccess studentAssignmentAccess,
			StudentSubmissionAccess studentSubmissionAccess, TeacherAssignmentAccess teacherAssignmentAccess,
			TeacherSubmissionAccess teacherSubmissionAccess, PresenceService presence, SimpMessagingTemplate messaging,
			AppProperties props, Clock clock) {
		this.studentAssignmentAccess = studentAssignmentAccess;
		this.studentSubmissionAccess = studentSubmissionAccess;
		this.teacherAssignmentAccess = teacherAssignmentAccess;
		this.teacherSubmissionAccess = teacherSubmissionAccess;
		this.presence = presence;
		this.messaging = messaging;
		this.limiter = new SlidingWindowLimiter(props.live().maxUpdatesPerMinute(), Duration.ofMinutes(1), clock);
		this.clock = clock;
	}

	/**
	 * Sinh viên gửi nội dung đang gõ. Phần không được thấy, phần không còn đang làm (đã nộp…), nội dung quá dài hoặc
	 * gửi quá dày → bỏ qua (không lỗi, không lộ thông tin), giống {@code /app/presence/viewing}.
	 */
	// Không bọc transaction: lỗi "không được thấy" của service cửa sẽ đánh dấu rollback transaction ngoài, bắt lại
	// rồi commit thì thành UnexpectedRollbackException (giống PresenceService.updateViewing).
	public void update(AuthPrincipal student, Long partId, String content) {
		if (partId == null || content == null || content.length() > MAX_CHARS) {
			return;
		}
		String k = key(partId, student.id());
		if (!limiter.tryAcquire(k)) {
			return;
		}
		Instant now = clock.instant();
		try {
			studentAssignmentAccess.requireVisiblePart(partId, student.id(), now);
			Submission s = studentSubmissionAccess.mine(student.id(), partId).orElse(null);
			if (s != null && !s.getStatus().isOpenForStudent()) {
				return;
			}
		} catch (ApiException e) {
			log.debug("Live: sinh viên {} gửi phần {} không được thấy", student.id(), partId);
			return;
		}
		int words = WordCounter.count(content);
		drafts.put(k, new Live(content, words, now));
		messaging.convertAndSend(topic(partId, student.id()),
				new LiveEvent(LiveType.CONTENT, partId, student.id(), content, words, now));
	}

	/** Nộp xong thì bỏ bản trong bộ nhớ và báo màn xem trực tiếp (sau commit — nộp bị rollback thì không báo). */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSubmitted(SubmissionEvents.Submitted e) {
		drafts.remove(key(e.partId(), e.studentId()));
		messaging.convertAndSend(topic(e.partId(), e.studentId()),
				new LiveEvent(LiveType.SUBMITTED, e.partId(), e.studentId(), "", 0, clock.instant()));
	}

	/**
	 * Trạng thái ban đầu của màn xem trực tiếp (BR-26): nội dung trong bộ nhớ nếu có, không thì bản nháp đã lưu.
	 * Phần của giáo viên khác → 404 (BR-15).
	 */
	@Transactional(readOnly = true)
	public LiveSnapshot snapshot(Long teacherId, Long partId, Long studentId) {
		AssignmentPart part = teacherAssignmentAccess.requireOwnedPart(teacherId, partId);
		Submission s = teacherSubmissionAccess.workInProgress(teacherId, partId, studentId).orElse(null);
		boolean online = presence.isOnline(studentId);
		Long viewing = presence.viewingPart(studentId, part.getAssignment().getId());
		Long submissionId = s == null ? null : s.getId();
		SubmissionStatus status = s == null ? SubmissionStatus.NOT_STARTED : s.getStatus();
		if (!status.isOpenForStudent()) {
			return new LiveSnapshot(partId, studentId, submissionId, status, LiveSource.NOT_WORKING, "", 0, null, online,
					viewing);
		}
		Live live = drafts.get(key(partId, studentId));
		if (live != null) {
			return new LiveSnapshot(partId, studentId, submissionId, status, LiveSource.LIVE, live.content(),
					live.wordCount(), live.at(), online, viewing);
		}
		if (s != null && s.getDraftContent() != null) {
			return new LiveSnapshot(partId, studentId, submissionId, status, LiveSource.DRAFT, s.getDraftContent(),
					WordCounter.count(s.getDraftContent()), s.getDraftSavedAt(), online, viewing);
		}
		return new LiveSnapshot(partId, studentId, submissionId, status, LiveSource.EMPTY, "", 0, null, online, viewing);
	}

	/** Xoá mọi bản trong bộ nhớ và bộ đếm (dùng trong test). */
	public void clear() {
		drafts.clear();
		limiter.clear();
	}

	static String topic(Long partId, Long studentId) {
		return "/topic/live/parts/" + partId + "/students/" + studentId;
	}

	private static String key(Long partId, Long studentId) {
		return partId + ":" + studentId;
	}
}
