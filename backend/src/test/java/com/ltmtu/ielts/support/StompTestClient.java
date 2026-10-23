package com.ltmtu.ielts.support;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Client STOMP thật cho test WebSocket (server chạy cổng ngẫu nhiên). Gọi {@link #close()} sau mỗi test. */
public final class StompTestClient implements AutoCloseable {

	/** Một kết nối: các khung ERROR server gửi về và trạng thái đã bị đóng chưa. */
	public static final class Connection extends StompSessionHandlerAdapter {
		public final BlockingQueue<String> errors = new LinkedBlockingQueue<>();
		private volatile boolean closedByServer;
		private StompSession session;

		@Override
		public Type getPayloadType(StompHeaders headers) {
			return byte[].class;
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			errors.add(String.valueOf(headers.getFirst("message")));
		}

		@Override
		public void handleTransportError(StompSession s, Throwable exception) {
			closedByServer = true;
		}

		public StompSession session() {
			return session;
		}

		/** Chờ tới khi server đóng kết nối; {@code false} nếu hết thời gian mà vẫn mở. */
		public boolean awaitClosed(long millis) throws InterruptedException {
			long until = System.nanoTime() + millis * 1_000_000;
			while (System.nanoTime() < until) {
				if (closedByServer || !session.isConnected()) {
					return true;
				}
				Thread.sleep(50);
			}
			return false;
		}
	}

	private final WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
	private final List<Connection> connections = new ArrayList<>();
	private final int port;
	private final ObjectMapper json;

	public StompTestClient(int port, ObjectMapper json) {
		this.port = port;
		this.json = json;
	}

	public Connection connect(String accessToken) throws Exception {
		Connection c = new Connection();
		StompHeaders headers = new StompHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
		c.session = client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers, c)
				.get(5, TimeUnit.SECONDS);
		connections.add(c);
		return c;
	}

	/** SUBSCRIBE rồi chờ một chút để broker đăng ký xong (simple broker không hỗ trợ RECEIPT). */
	public BlockingQueue<JsonNode> subscribe(Connection c, String destination) throws InterruptedException {
		BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();
		c.session.subscribe(destination, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return byte[].class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				received.add(json.readTree(new String((byte[]) payload, StandardCharsets.UTF_8)));
			}
		});
		Thread.sleep(300);
		return received;
	}

	/** SEND một payload JSON. */
	public void send(Connection c, String destination, Object payload) {
		StompHeaders headers = new StompHeaders();
		headers.setDestination(destination);
		headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
		c.session.send(headers, json.writeValueAsBytes(payload));
	}

	@Override
	public void close() {
		for (Connection c : connections) {
			try {
				if (c.session.isConnected()) {
					c.session.disconnect();
				}
			} catch (RuntimeException ignored) {
				// Server vừa đóng kết nối (sau khung ERROR hoặc FORCE_LOGOUT) mà client chưa kịp biết.
			}
		}
		client.stop();
	}
}
