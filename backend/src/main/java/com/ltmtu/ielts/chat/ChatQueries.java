package com.ltmtu.ielts.chat;

import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ltmtu.ielts.chat.ChatChannel.Type;
import com.ltmtu.ielts.chat.dto.ChatDtos.ChannelSummary;
import com.ltmtu.ielts.chat.dto.ChatDtos.LastMessage;
import com.ltmtu.ielts.chat.dto.ChatDtos.MessageEvent;
import com.ltmtu.ielts.chat.dto.ChatDtos.Reader;

import lombok.RequiredArgsConstructor;

/**
 * Truy vấn chỉ-đọc của chat trên bảng của module khác (bài nộp, lớp, người dùng) — ngoại lệ được phép ở coding rules
 * mục 3. Không ghi gì ở đây.
 */
@Repository
@RequiredArgsConstructor
class ChatQueries {

	/** Điều kiện "tin thuộc kênh" theo loại kênh; tên cột lấy từ enum, không từ dữ liệu người dùng. */
	private static String channelColumn(Type type) {
		return switch (type) {
		case SUBMISSION -> "submission_id";
		case CLASSROOM -> "classroom_id";
		case DIRECT -> "direct_channel_id";
		};
	}

	private final NamedParameterJdbcTemplate jdbc;

	/**
	 * Thành viên kênh (UC-29): bài nộp → sinh viên của bài làm (bài PAPER: mọi thành viên nhóm — view
	 * {@code submission_members}) và giáo viên chủ bài tập; lớp → giáo viên của lớp và thành viên hiện tại.
	 */
	boolean isMember(Long userId, ChatChannel channel) {
		MapSqlParameterSource p = new MapSqlParameterSource("ref", channel.refId()).addValue("me", userId);
		String sql = switch (channel.type()) {
		case SUBMISSION -> """
				SELECT EXISTS (
				    SELECT 1 FROM submissions s JOIN assignment_parts ap ON ap.id = s.part_id
				    JOIN assignments a ON a.id = ap.assignment_id
				    WHERE s.id = :ref AND (a.teacher_id = :me OR EXISTS (
				        SELECT 1 FROM submission_members sm WHERE sm.submission_id = s.id AND sm.student_id = :me)))""";
		case CLASSROOM -> """
				SELECT EXISTS (
				    SELECT 1 FROM classrooms c WHERE c.id = :ref
				      AND (c.teacher_id = :me
				           OR EXISTS (SELECT 1 FROM class_members m WHERE m.classroom_id = c.id AND m.student_id = :me)))""";
		case DIRECT -> """
				SELECT EXISTS (
				    SELECT 1 FROM direct_chat_channels d
				    WHERE d.id = :ref AND (d.teacher_id = :me OR d.student_id = :me))""";
		};
		return Boolean.TRUE.equals(jdbc.queryForObject(sql, p, Boolean.class));
	}

	/** Tên đăng nhập của những người khác trong kênh bài nộp (sinh viên / cả nhóm và giáo viên chủ bài), trừ người gửi. */
	List<String> otherSubmissionMembers(long submissionId, Long exceptUserId) {
		return jdbc.queryForList("""
				SELECT u.username FROM users u
				WHERE u.id <> :me AND (
				    u.id IN (SELECT sm.student_id FROM submission_members sm WHERE sm.submission_id = :ref)
				    OR u.id = (SELECT a.teacher_id FROM submissions s JOIN assignment_parts ap ON ap.id = s.part_id
				               JOIN assignments a ON a.id = ap.assignment_id WHERE s.id = :ref))""",
				new MapSqlParameterSource("ref", submissionId).addValue("me", exceptUserId), String.class);
	}

	/** Tên đăng nhập của bên kia trong kênh riêng giáo viên–sinh viên. */
	List<String> otherDirectMembers(long channelId, Long exceptUserId) {
		return jdbc.queryForList("""
				SELECT u.username FROM direct_chat_channels d JOIN users u ON u.id IN (d.teacher_id, d.student_id)
				WHERE d.id = :ref AND u.id <> :me""",
				new MapSqlParameterSource("ref", channelId).addValue("me", exceptUserId), String.class);
	}

	/** Giáo viên chỉ được mở chat với sinh viên đang thuộc lớp hoặc bài mình phụ trách. */
	boolean canTeacherContactStudent(Long teacherId, Long studentId) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
				SELECT EXISTS (
				    SELECT 1 FROM users s
				    WHERE s.id = :student AND s.role = 'STUDENT' AND EXISTS (
				        SELECT 1 FROM classrooms c JOIN class_members m ON m.classroom_id = c.id
				        WHERE c.teacher_id = :teacher AND m.student_id = s.id
				    )
				) OR EXISTS (
				    SELECT 1 FROM assignment_targets t JOIN assignments a ON a.id = t.assignment_id
				    WHERE a.teacher_id = :teacher AND (t.student_id = :student OR EXISTS (
				        SELECT 1 FROM class_members m WHERE m.classroom_id = t.classroom_id AND m.student_id = :student
				    ))
				)""", new MapSqlParameterSource("teacher", teacherId).addValue("student", studentId), Boolean.class));
	}

	ChannelSummary directSummary(long channelId, long viewerId) {
		return jdbc.queryForObject("""
				SELECT CASE WHEN d.teacher_id = :me THEN 'Trao đổi với ' || su.full_name
				            ELSE 'Trao đổi với ' || tu.full_name END AS title
				FROM direct_chat_channels d JOIN users tu ON tu.id = d.teacher_id JOIN users su ON su.id = d.student_id
				WHERE d.id = :ref""", new MapSqlParameterSource("ref", channelId).addValue("me", viewerId),
				(rs, i) -> new ChannelSummary("direct-" + channelId, Type.DIRECT.name(), rs.getString("title"), null, 0));
	}

	String fullName(Long userId) {
		return jdbc.queryForObject("SELECT full_name FROM users WHERE id = :id", new MapSqlParameterSource("id", userId),
				String.class);
	}

	/** Tin có thuộc kênh không (khi đánh dấu đã đọc). */
	boolean messageInChannel(Long messageId, ChatChannel channel) {
		return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM chat_messages WHERE id = :id AND "
				+ channelColumn(channel.type()) + " = :ref)",
				new MapSqlParameterSource("id", messageId).addValue("ref", channel.refId()), Boolean.class));
	}

	/** Tin trước {@code beforeId} (hoặc mới nhất), mới nhất trước. */
	List<MessageEvent> history(ChatChannel channel, Long beforeId, int size) {
		String sql = """
				SELECT m.id, m.sender_id, u.full_name, m.content, m.client_msg_id, m.created_at
				FROM chat_messages m JOIN users u ON u.id = m.sender_id
				WHERE m.""" + channelColumn(channel.type()) + """
				 = :ref AND (CAST(:beforeId AS BIGINT) IS NULL OR m.id < :beforeId)
				ORDER BY m.id DESC LIMIT :size""";
		MapSqlParameterSource p = new MapSqlParameterSource("ref", channel.refId()).addValue("size", size)
				.addValue("beforeId", beforeId, Types.BIGINT);
		return jdbc.query(sql, p, (rs, i) -> new MessageEvent("MESSAGE", rs.getLong("id"), channel.id(),
				rs.getLong("sender_id"), rs.getString("full_name"), rs.getString("content"), rs.getString("client_msg_id"),
				rs.getTimestamp("created_at").toInstant()));
	}

	/** Mốc "đã đọc" của mọi người khác trong kênh. */
	List<Reader> readers(ChatChannel channel, Long exceptUserId) {
		return jdbc.query("""
				SELECT r.user_id, u.full_name, r.last_read_message_id
				FROM chat_read_markers r JOIN users u ON u.id = r.user_id
				WHERE r.channel_type = :type AND r.channel_ref_id = :ref AND r.user_id <> :me
				ORDER BY r.user_id""",
				new MapSqlParameterSource("type", channel.type().name()).addValue("ref", channel.refId())
						.addValue("me", exceptUserId),
				(rs, i) -> new Reader(rs.getLong("user_id"), rs.getString("full_name"), rs.getLong("last_read_message_id")));
	}

	/**
	 * Kênh của một người: mọi lớp mình dạy / đang học (luôn có, kể cả chưa có tin) và các bài nộp đã có tin nhắn. Kèm tin
	 * cuối và số tin chưa đọc; kênh có tin mới nhất xếp trước.
	 */
	List<ChannelSummary> channelsOf(Long userId) {
		String sql = """
				WITH ch AS (
				    SELECT 'CLASSROOM' AS type, c.id AS ref, c.code || ' – ' || c.name AS title
				    FROM classrooms c
				    WHERE c.teacher_id = :me
				       OR EXISTS (SELECT 1 FROM class_members m WHERE m.classroom_id = c.id AND m.student_id = :me)
				    UNION ALL
				    SELECT 'SUBMISSION', s.id, a.title || ' – ' || ap.title || ' · ' || COALESCE(g.name, u.full_name)
				    FROM submissions s
				    JOIN assignment_parts ap ON ap.id = s.part_id
				    JOIN assignments a ON a.id = ap.assignment_id
				    JOIN users u ON u.id = s.student_id
				    LEFT JOIN paper_groups g ON g.id = s.group_id
				    WHERE (a.teacher_id = :me OR EXISTS (
				        SELECT 1 FROM submission_members sm WHERE sm.submission_id = s.id AND sm.student_id = :me))
				      AND EXISTS (SELECT 1 FROM chat_messages cm WHERE cm.submission_id = s.id)
				    UNION ALL
				    SELECT 'DIRECT', d.id,
				           CASE WHEN d.teacher_id = :me THEN 'Trao đổi với ' || su.full_name
				                ELSE 'Trao đổi với ' || tu.full_name END
				    FROM direct_chat_channels d
				    JOIN users tu ON tu.id = d.teacher_id
				    JOIN users su ON su.id = d.student_id
				    WHERE (d.teacher_id = :me OR d.student_id = :me)
				      AND EXISTS (SELECT 1 FROM chat_messages cm WHERE cm.direct_channel_id = d.id)
				), msg AS (
				    SELECT ch.type, ch.ref, cm.id, cm.sender_id, cm.content, cm.created_at
				    FROM ch JOIN chat_messages cm
				      ON (ch.type = 'SUBMISSION' AND cm.submission_id = ch.ref)
				      OR (ch.type = 'CLASSROOM' AND cm.classroom_id = ch.ref)
				      OR (ch.type = 'DIRECT' AND cm.direct_channel_id = ch.ref)
				)
				SELECT ch.type, ch.ref, ch.title,
				       last.id AS last_id, last.content AS last_content, last.created_at AS last_at, lu.full_name AS last_sender,
				       (SELECT count(*) FROM msg
				        WHERE msg.type = ch.type AND msg.ref = ch.ref AND msg.sender_id <> :me
				          AND msg.id > coalesce((SELECT r.last_read_message_id FROM chat_read_markers r
				                                 WHERE r.user_id = :me AND r.channel_type = ch.type
				                                   AND r.channel_ref_id = ch.ref), 0)) AS unread
				FROM ch
				LEFT JOIN LATERAL (SELECT * FROM msg WHERE msg.type = ch.type AND msg.ref = ch.ref
				                   ORDER BY msg.id DESC LIMIT 1) last ON TRUE
				LEFT JOIN users lu ON lu.id = last.sender_id
				ORDER BY last.created_at DESC NULLS LAST, ch.title""";
		return jdbc.query(sql, new MapSqlParameterSource("me", userId), (rs, i) -> {
			Type type = Type.valueOf(rs.getString("type"));
			ChatChannel channel = new ChatChannel(type, rs.getLong("ref"));
			long lastId = rs.getLong("last_id");
			LastMessage last = rs.wasNull() ? null
					: new LastMessage(lastId, rs.getString("last_sender"), preview(rs.getString("last_content")),
							rs.getTimestamp("last_at").toInstant());
			return new ChannelSummary(channel.id(), type.name(), rs.getString("title"), last, rs.getLong("unread"));
		});
	}

	private static String preview(String content) {
		return content.length() <= 100 ? content : content.substring(0, 100) + "…";
	}
}
