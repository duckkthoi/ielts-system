package com.ltmtu.ielts.realtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import lombok.RequiredArgsConstructor;

/** Gắn {@link WsSessionRegistry} vào tầng WebSocket (cấu hình STOMP chính ở {@code config.WebSocketConfig}). */
@Configuration
@RequiredArgsConstructor
public class RealtimeWebSocketConfigurer implements WebSocketMessageBrokerConfigurer {

	private final WsSessionRegistry registry;

	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
		registration.addDecoratorFactory(registry::decorate);
	}
}
