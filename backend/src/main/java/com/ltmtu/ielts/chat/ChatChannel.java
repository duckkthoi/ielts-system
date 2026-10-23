package com.ltmtu.ielts.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Một kênh chat (docs/05 B.2): {@code submission-{id}} (giáo viên ↔ sinh viên của một bài nộp) hoặc {@code class-{id}}
 * (cả lớp). Mã kênh xuất hiện trong đường dẫn REST và destination STOMP.
 */
public record ChatChannel(Type type, long refId) {

	public enum Type {
		SUBMISSION, CLASSROOM
	}

	private static final Pattern ID = Pattern.compile("^(submission|class)-(\\d{1,18})$");

	/** Phân tích mã kênh; sai dạng → {@code null}. */
	public static ChatChannel parse(String channelId) {
		if (channelId == null) {
			return null;
		}
		Matcher m = ID.matcher(channelId);
		if (!m.matches()) {
			return null;
		}
		return new ChatChannel("submission".equals(m.group(1)) ? Type.SUBMISSION : Type.CLASSROOM,
				Long.parseLong(m.group(2)));
	}

	public String id() {
		return (type == Type.SUBMISSION ? "submission-" : "class-") + refId;
	}

	public String topic() {
		return "/topic/chat/" + id();
	}
}
