package com.ltmtu.ielts.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.ltmtu.ielts.security.StompAuthInterceptor;

/**
 * WebSocket STOMP — docs/05-api.md phần B.1. WebSocket thuần (không SockJS), simple broker trong ứng dụng
 * (monolith, một instance, không cần broker ngoài).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	/** Bằng giới hạn khung STOMP mặc định của Spring (64 KB). */
	private static final int MAX_FRAME_BYTES = 64 * 1024;

	/** heart-beat 10 giây mỗi chiều: client không nhận được thì coi như mất kết nối và tự kết nối lại (SD-09). */
	private static final long[] HEARTBEAT = { 10_000, 10_000 };

	private final StompAuthInterceptor authInterceptor;
	private final AppProperties props;
	private final TaskScheduler brokerScheduler;

	/** {@code messageBrokerTaskScheduler} do chính cấu hình STOMP của Spring tạo, nên phải lấy {@code @Lazy}. */
	public WebSocketConfig(StompAuthInterceptor authInterceptor, AppProperties props,
			@Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler brokerScheduler) {
		this.authInterceptor = authInterceptor;
		this.props = props;
		this.brokerScheduler = brokerScheduler;
	}

	/**
	 * Bộ đệm khung WebSocket của Tomcat mặc định chỉ 8 KB — khung lớn hơn làm server đóng kết nối. Nâng bằng giới hạn
	 * khung STOMP (64 KB) để gói cập nhật Yjs của tài liệu paper (tối đa 32 KB, base64) đi được (B22). Đặt qua tham số
	 * khởi tạo của Tomcat nên chỉ có tác dụng khi chạy Tomcat thật (test MockMvc không có WebSocket container).
	 */
	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> webSocketBufferCustomizer() {
		return factory -> factory.addContextCustomizers(context -> {
			context.addParameter("org.apache.tomcat.websocket.textBufferSize", String.valueOf(MAX_FRAME_BYTES));
			context.addParameter("org.apache.tomcat.websocket.binaryBufferSize", String.valueOf(MAX_FRAME_BYTES));
		});
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOriginPatterns(props.cors().allowedOrigins().toArray(String[]::new));
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/queue").setHeartbeatValue(HEARTBEAT).setTaskScheduler(brokerScheduler);
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(authInterceptor);
	}
}
