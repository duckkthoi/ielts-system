package com.ltmtu.ielts.paper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.AssignmentPart;
import com.ltmtu.ielts.assignment.StudentAssignmentAccess;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.SlidingWindowLimiter;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.AwarenessPush;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.DocumentState;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.SnapshotRequest;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.UpdateDto;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.UpdateMessage;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.UpdatePush;
import com.ltmtu.ielts.paper.dto.PaperDocumentDtos.UpdatesAfter;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Tài liệu paper cùng sửa theo Yjs (docs/12 mục 5.2, B22). Server không hiểu nội dung Yjs: chỉ cấp số thứ tự (khoá dòng
 * tài liệu nên update đồng thời nhận seq liên tiếp), lưu, và <b>sau commit</b> phát tới {@code /topic/paper/{id}}. Client
 * định kỳ gửi snapshot đã gộp kèm JSON + chữ thuần; update đã gộp bị bỏ nội dung, giữ người sửa cho thống kê.
 * Không log nội dung bài.
 */
@Slf4j
@Service
public class PaperDocumentService {

	private final StudentAssignmentAccess assignmentAccess;
	private final PaperDocAccess access;
	private final PaperDocumentRepository documentRepository;
	private final PaperDocUpdateRepository updateRepository;
	private final PaperGroupMemberRepository memberRepository;
	private final UserQueryService userQueryService;
	private final SimpMessagingTemplate messaging;
	private final ApplicationEventPublisher events;
	private final ObjectMapper json;
	private final Clock clock;
	private final AppProperties.Paper limits;
	private final SlidingWindowLimiter limiter;

	public PaperDocumentService(StudentAssignmentAccess assignmentAccess, PaperDocAccess access,
			PaperDocumentRepository documentRepository, PaperDocUpdateRepository updateRepository,
			PaperGroupMemberRepository memberRepository, UserQueryService userQueryService,
			SimpMessagingTemplate messaging, ApplicationEventPublisher events, ObjectMapper json, Clock clock,
			AppProperties props) {
		this.assignmentAccess = assignmentAccess;
		this.access = access;
		this.documentRepository = documentRepository;
		this.updateRepository = updateRepository;
		this.memberRepository = memberRepository;
		this.userQueryService = userQueryService;
		this.messaging = messaging;
		this.events = events;
		this.json = json;
		this.clock = clock;
		this.limits = props.paper();
		this.limiter = new SlidingWindowLimiter(limits.maxUpdatesPerMinute(), Duration.ofMinutes(1), clock);
	}

	/**
	 * Mở tài liệu của nhóm tôi cho một giai đoạn (tạo nếu chưa có). Bài không phải PAPER → 409; chưa có nhóm → 409
	 * NO_GROUP; phần không được thấy → 404.
	 */
	@Transactional
	public DocumentState open(Long studentId, Long partId) {
		Instant now = clock.instant();
		AssignmentPart part = assignmentAccess.requireVisiblePart(partId, studentId, now);
		if (!part.getAssignment().isPaper()) {
			throw ApiException.conflict("INVALID_STATE", "Phần này không phải bài paper");
		}
		Long groupId = memberRepository.findByAssignmentIdAndIdStudentId(part.getAssignment().getId(), studentId)
				.map(m -> m.getId().getGroupId())
				.orElseThrow(() -> ApiException.conflict("NO_GROUP", "Bạn chưa có nhóm. Hãy vào hoặc lập nhóm trước khi viết"));
		documentRepository.insertIfAbsent(partId, groupId, now);
		PaperDocument d = documentRepository.findByPartIdAndGroupId(partId, groupId).orElseThrow();
		return state(access.requireMember(studentId, d.getId()));
	}

	/** Lấy bù các update sau {@code afterSeq} (kết nối lại). Đã bị gộp vào snapshot → {@code resync}. */
	@Transactional(readOnly = true)
	public UpdatesAfter updatesAfter(AuthPrincipal user, Long documentId, long afterSeq) {
		PaperDocument d = access.requireReadable(user, documentId);
		if (afterSeq < d.getSnapshotSeq()) {
			return new UpdatesAfter(List.of(), d.getLastSeq(), true);
		}
		return new UpdatesAfter(toDtos(updateRepository.findAfter(documentId, afterSeq)), d.getLastSeq(), false);
	}

	/** Trạng thái đầy đủ (giáo viên xem chỉ đọc cũng dùng). */
	@Transactional(readOnly = true)
	public DocumentState state(AuthPrincipal user, Long documentId) {
		return state(access.requireReadable(user, documentId));
	}

	/**
	 * Lưu snapshot đã gộp tới {@code upToSeq}. Cũ hơn snapshot đang có → bỏ qua (thành viên khác vừa lưu bản mới hơn);
	 * vượt số thứ tự đã cấp → 400. Update ≤ upToSeq bị bỏ nội dung.
	 */
	@Transactional
	public DocumentState saveSnapshot(Long studentId, Long documentId, SnapshotRequest req) {
		PaperDocument d = lockFirst(documentId);
		access.requireMember(studentId, documentId);
		byte[] state = decode(req.state(), limits.maxSnapshotBytes(), "Snapshot");
		if (req.upToSeq() > d.getLastSeq()) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Snapshot vượt quá các thay đổi đã lưu");
		}
		if (req.contentText() != null && req.contentText().length() > limits.maxTextChars()) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Bài quá dài");
		}
		requireJson(req.contentJson());
		if (req.upToSeq() > d.getSnapshotSeq() || d.getSnapshot() == null) {
			d.setSnapshot(state);
			d.setSnapshotSeq(req.upToSeq());
			d.setContentJson(req.contentJson());
			d.setContentText(req.contentText());
			documentRepository.flush();
			updateRepository.compact(documentId, req.upToSeq());
		}
		return state(access.requireMember(studentId, documentId));
	}

	/**
	 * Một update Yjs từ thành viên nhóm (STOMP). Không phải thành viên → 404; tài liệu đã khoá (đã nộp / bài đóng) →
	 * 409; quá lớn / sai base64 → 400; gửi quá dày → 429. Lỗi trả riêng người gửi trên {@code /user/queue/errors}.
	 */
	@Transactional
	public void applyUpdate(AuthPrincipal student, Long documentId, UpdateMessage msg) {
		if (msg == null || msg.update() == null) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Thiếu nội dung cập nhật");
		}
		PaperDocument locked = lockFirst(documentId);
		PaperDocument d = access.requireMember(student.id(), documentId);
		if (!access.editable(d)) {
			throw ApiException.conflict("INVALID_STATE", "Nhóm đã nộp giai đoạn này hoặc bài đã đóng — tài liệu chỉ đọc");
		}
		String key = documentId + ":" + student.id();
		if (limiter.isLimited(key)) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Gửi thay đổi quá dày, hãy thử lại sau");
		}
		limiter.hit(key);
		byte[] payload = decode(msg.update(), limits.maxUpdateBytes(), "Gói cập nhật");
		long seq = locked.getLastSeq() + 1;
		locked.setLastSeq(seq);
		updateRepository.save(new PaperDocUpdate(documentId, seq, payload, userQueryService.requireUser(student.id()),
				nonNegative(msg.charsAdded()), nonNegative(msg.charsRemoved()), clock.instant()));
		events.publishEvent(new PaperEvents.UpdateSaved(documentId,
				new UpdatePush(documentId, seq, msg.update(), student.id(), msg.clientId())));
	}

	/** Con trỏ / vùng chọn (Yjs awareness): chỉ chuyển tiếp tới nhóm, không lưu. */
	@Transactional(readOnly = true)
	public void awareness(AuthPrincipal student, Long documentId, String state) {
		if (state == null || state.length() > 8_192) {
			return;
		}
		access.requireMember(student.id(), documentId);
		messaging.convertAndSend(topic(documentId) + "/awareness",
				new AwarenessPush(student.id(), userQueryService.requireUser(student.id()).getFullName(), state));
	}

	/** Xoá bộ đếm giới hạn (dùng trong test). */
	public void clear() {
		limiter.clear();
	}

	static String topic(Long documentId) {
		return "/topic/paper/" + documentId;
	}

	// ─── Nội bộ ──────────────────────────────────────────────────────────────

	/**
	 * Khoá dòng tài liệu <b>trước</b> mọi lần đọc khác trong transaction (AI-CODING-RULES mục 10): nếu kiểm quyền nạp
	 * tài liệu trước thì JPA trả lại bản đã nạp với {@code last_seq} cũ — hai update đồng thời nhận trùng số thứ tự.
	 */
	private PaperDocument lockFirst(Long documentId) {
		return documentRepository.findForUpdate(documentId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy tài liệu"));
	}

	private DocumentState state(PaperDocument d) {
		List<UpdateDto> updates = toDtos(updateRepository.findAfter(d.getId(), d.getSnapshotSeq()));
		return new DocumentState(d.getId(), d.getPart().getId(), d.getGroup().getId(),
				d.getSnapshot() == null ? null : Base64.getEncoder().encodeToString(d.getSnapshot()), d.getSnapshotSeq(),
				d.getLastSeq(), updates, access.editable(d));
	}

	private static List<UpdateDto> toDtos(List<PaperDocUpdate> updates) {
		return updates.stream().map(u -> new UpdateDto(u.getId().getSeq(), Base64.getEncoder().encodeToString(u.getPayload()),
				u.getAuthor() == null ? null : u.getAuthor().getId(), u.getCreatedAt())).toList();
	}

	private static byte[] decode(String base64, int maxBytes, String what) {
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(base64);
		} catch (IllegalArgumentException e) {
			throw ApiException.badRequest("VALIDATION_ERROR", what + " không đúng định dạng");
		}
		if (bytes.length == 0 || bytes.length > maxBytes) {
			throw ApiException.badRequest("VALIDATION_ERROR", what + " rỗng hoặc quá lớn");
		}
		return bytes;
	}

	private void requireJson(String contentJson) {
		if (contentJson == null) {
			return;
		}
		try {
			json.readTree(contentJson);
		} catch (JacksonException e) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Nội dung JSON của tài liệu không hợp lệ");
		}
	}

	private static int nonNegative(Integer n) {
		return n == null || n < 0 ? 0 : n;
	}
}
