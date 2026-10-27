package com.ltmtu.ielts.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.Role;

class ChatChannelRuleTest {

	@Test
	void subscriptionAndSendUseFullChannelId() {
		ChatQueries queries = mock(ChatQueries.class);
		when(queries.isMember(eq(7L), any(ChatChannel.class))).thenReturn(true);
		ChatChannelRule rule = new ChatChannelRule(queries);
		AuthPrincipal teacher = new AuthPrincipal(7L, "teacher", Role.TEACHER, 1L);

		assertThat(rule.canSubscribe(teacher, "/topic/chat/class-4")).isTrue();
		verify(queries).isMember(7L, new ChatChannel(ChatChannel.Type.CLASSROOM, 4));
		assertThat(rule.canSend(teacher, "/app/chat/direct-12/send")).isTrue();
		verify(queries).isMember(7L, new ChatChannel(ChatChannel.Type.DIRECT, 12));
	}
}
