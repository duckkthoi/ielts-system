package com.ltmtu.ielts.realtime;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.ltmtu.ielts.assignment.Assignment;
import com.ltmtu.ielts.assignment.StudentAssignmentAccess;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.realtime.RealtimeDtos.PresenceEvent;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Trạng thái online của sinh viên và phần đang mở (FR-54), giữ trong bộ nhớ (monolith một instance, không Redis).
 * <ul>
 * <li>Online = có ít nhất một kết nối WebSocket; kết nối đầu / ngắt kết nối cuối thì báo cho giáo viên của mọi bài
 * sinh viên đang được giao ({@code /topic/assignments/{id}/presence}).</li>
 * <li>Phần đang mở: sinh viên gửi {@code /app/presence/viewing}; theo từng kết nối (mỗi tab một kết nối).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

	private record Viewing(Long assignmentId, Long partId) {
	}

	/** Sinh viên → các kết nối đang mở. */
	private final Map<Long, Set<String>> connections = new ConcurrentHashMap<>();
	/** Kết nối → bài / phần đang mở. */
	private final Map<String, Viewing> viewing = new ConcurrentHashMap<>();
	/** Kết nối → sinh viên (để xử lý ngắt kết nối khi chỉ biết id kết nối). */
	private final Map<String, Long> owners = new ConcurrentHashMap<>();

	private final StudentAssignmentAccess assignmentAccess;
	private final SimpMessagingTemplate messaging;
	private final Clock clock;

	public boolean isOnline(Long studentId) {
		Set<String> c = connections.get(studentId);
		return c != null && !c.isEmpty();
	}

	/** Phần sinh viên đang mở trong bài (kết nối nào cũng được); {@code null} nếu không mở bài này. */
	public Long viewingPart(Long studentId, Long assignmentId) {
		Set<String> c = connections.get(studentId);
		if (c == null) {
			return null;
		}
		return c.stream().map(viewing::get).filter(Objects::nonNull).filter(v -> v.assignmentId().equals(assignmentId))
				.map(Viewing::partId).filter(Objects::nonNull).findFirst().orElse(null);
	}

	@EventListener
	void onConnected(SessionConnectedEvent event) {
		AuthPrincipal p = WsSessionRegistry.principal(event.getUser());
		String connectionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
		if (p == null || connectionId == null || p.role() != Role.STUDENT) {
			return;
		}
		owners.put(connectionId, p.id());
		Set<String> c = connections.computeIfAbsent(p.id(), k -> ConcurrentHashMap.newKeySet());
		boolean first = c.isEmpty();
		c.add(connectionId);
		if (first) {
			broadcastToAll(p.id(), true);
		}
	}

	@EventListener
	void onDisconnect(SessionDisconnectEvent event) {
		String connectionId = event.getSessionId();
		Long studentId = owners.remove(connectionId);
		if (studentId == null) {
			return; // Không phải sinh viên, hoặc đã xử lý (sự kiện ngắt kết nối có thể đến hai lần).
		}
		Viewing was = viewing.remove(connectionId);
		Set<String> c = connections.get(studentId);
		if (c != null) {
			c.remove(connectionId);
			if (c.isEmpty()) {
				connections.remove(studentId, c);
				broadcastToAll(studentId, false);
				return;
			}
		}
		if (was != null) {
			send(was.assignmentId(), studentId, viewingPart(studentId, was.assignmentId()));
		}
	}

	/**
	 * Sinh viên mở / đổi / rời một phần. Bài không được thấy hoặc phần không thuộc bài thì bỏ qua (không lộ thông tin).
	 */
	void updateViewing(AuthPrincipal student, String connectionId, Long assignmentId, Long partId) {
		if (assignmentId == null || !owners.containsKey(connectionId)) {
			return;
		}
		try {
			if (partId == null) {
				assignmentAccess.requireVisibleAssignment(assignmentId, student.id(), clock.instant());
			} else if (!assignmentAccess.requireVisiblePart(partId, student.id(), clock.instant()).getAssignment().getId()
					.equals(assignmentId)) {
				return;
			}
		} catch (ApiException e) {
			log.debug("Presence: sinh viên {} gửi bài {} không được thấy", student.id(), assignmentId);
			return;
		}
		Viewing previous = partId == null ? viewing.remove(connectionId)
				: viewing.put(connectionId, new Viewing(assignmentId, partId));
		if (previous != null && !previous.assignmentId().equals(assignmentId)) {
			send(previous.assignmentId(), student.id(), viewingPart(student.id(), previous.assignmentId()));
		}
		send(assignmentId, student.id(), viewingPart(student.id(), assignmentId));
	}

	private void broadcastToAll(Long studentId, boolean online) {
		List<Assignment> assignments = assignmentAccess.visibleAssignments(studentId, clock.instant());
		Instant now = clock.instant();
		for (Assignment a : assignments) {
			messaging.convertAndSend(topic(a.getId()), new PresenceEvent(studentId, online, null, now));
		}
	}

	private void send(Long assignmentId, Long studentId, Long partId) {
		messaging.convertAndSend(topic(assignmentId),
				new PresenceEvent(studentId, isOnline(studentId), partId, clock.instant()));
	}

	static String topic(Long assignmentId) {
		return "/topic/assignments/" + assignmentId + "/presence";
	}
}
