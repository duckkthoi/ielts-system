package com.ltmtu.ielts.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Một kênh chat (docs/05 B.2): bài nộp, lớp hoặc cặp giáo viên–sinh viên. Mã kênh xuất hiện trong đường dẫn REST và
 * destination STOMP.
 */
public record ChatChannel(Type type, long refId) {

	public enum Type {
		SUBMISSION, CLASSROOM, DIRECT
	}

	private static final Pattern ID = Pattern.compile("^(submission|class|direct)-(\\d{1,18})$");

	/** Phân tích mã kênh; sai dạng → {@code null}. */
	public static ChatChannel parse(String channelId) {
		if (channelId == null) {
			return null;
		}
		Matcher m = ID.matcher(channelId);
		if (!m.matches()) {
			return null;
		}
		Type type = switch (m.group(1)) {
		case "submission" -> Type.SUBMISSION;
		case "class" -> Type.CLASSROOM;
		default -> Type.DIRECT;
		};
		return new ChatChannel(type, Long.parseLong(m.group(2)));
	}

	public String id() {
		return switch (type) {
		case SUBMISSION -> "submission-" + refId;
		case CLASSROOM -> "class-" + refId;
		case DIRECT -> "direct-" + refId;
		};
	}

	public String topic() {
		return "/topic/chat/" + id();
	}
}
