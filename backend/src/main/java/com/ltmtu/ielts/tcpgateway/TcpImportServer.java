package com.ltmtu.ielts.tcpgateway;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.ltmtu.ielts.classroom.StudentImportService;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.security.AccessTokenVerifier;

import lombok.extern.slf4j.Slf4j;

/**
 * Server TCP import sinh viên (docs/05 phần C, Mức 2). Chạy trong cùng ứng dụng Spring (monolith):
 * <ul>
 * <li>một luồng chạy vòng {@code accept()}; mỗi kết nối giao cho thread pool cỡ cố định;</li>
 * <li>số phiên đồng thời giới hạn bằng {@link Semaphore} (bằng số luồng): hết chỗ thì <b>từ chối ngay</b> bằng
 * {@code ERR 503}, để client biết server bận thay vì treo trong hàng đợi;</li>
 * <li>khởi động / dừng theo vòng đời Spring ({@link SmartLifecycle}): tắt ứng dụng thì đóng {@code ServerSocket}
 * và dừng pool.</li>
 * </ul>
 */
@Slf4j
@Component
public class TcpImportServer implements SmartLifecycle {

	private final AppProperties.Tcp config;
	private final AccessTokenVerifier verifier;
	private final StudentImportService importService;

	private volatile boolean running;
	private ServerSocket serverSocket;
	private ExecutorService pool;
	private Semaphore slots;
	private Thread acceptor;

	public TcpImportServer(AppProperties props, AccessTokenVerifier verifier, StudentImportService importService) {
		this.config = props.tcp();
		this.verifier = verifier;
		this.importService = importService;
	}

	@Override
	public synchronized void start() {
		if (running || config == null || !config.enabled()) {
			return;
		}
		try {
			serverSocket = new ServerSocket(config.port());
		} catch (IOException e) {
			throw new UncheckedIOException("Không mở được cổng TCP " + config.port(), e);
		}
		AtomicInteger seq = new AtomicInteger();
		pool = Executors.newFixedThreadPool(config.threads(), r -> {
			Thread t = new Thread(r, "tcp-import-" + seq.incrementAndGet());
			t.setDaemon(true);
			return t;
		});
		slots = new Semaphore(config.threads());
		running = true;
		acceptor = new Thread(this::acceptLoop, "tcp-import-acceptor");
		acceptor.setDaemon(true);
		acceptor.start();
		log.info("Server TCP import đang lắng nghe cổng {} ({} luồng)", serverSocket.getLocalPort(), config.threads());
	}

	private void acceptLoop() {
		while (running) {
			Socket socket;
			try {
				socket = serverSocket.accept();
			} catch (SocketException e) {
				return; // ServerSocket đã đóng khi dừng ứng dụng.
			} catch (IOException e) {
				log.warn("TCP: lỗi accept: {}", e.getMessage());
				continue;
			}
			if (!slots.tryAcquire()) {
				reject(socket);
				continue;
			}
			try {
				socket.setSoTimeout((int) config.readTimeout().toMillis());
				TcpImportSession session = new TcpImportSession(socket, verifier, importService, config.readTimeout(),
						config.sessionTimeout(), config.maxPayload());
				pool.execute(() -> {
					try {
						session.run();
					} finally {
						slots.release();
					}
				});
			} catch (IOException | RuntimeException e) {
				slots.release();
				closeQuietly(socket);
			}
		}
	}

	/** Số phiên đang xử lý (để theo dõi / test). */
	public int activeSessions() {
		return running ? config.threads() - slots.availablePermits() : 0;
	}

	/** Pool đầy: báo bận rồi đóng ngay, không để client chờ (docs/05 mục C.1). */
	private static void reject(Socket socket) {
		try (socket; OutputStream out = socket.getOutputStream()) {
			out.write("ERR 503 Server bận, hãy thử lại sau\n".getBytes(StandardCharsets.UTF_8));
			out.flush();
		} catch (IOException ignored) {
			// Client đã đi.
		}
		log.warn("TCP: từ chối kết nối {} vì pool đầy", socket.getRemoteSocketAddress());
	}

	private static void closeQuietly(Socket socket) {
		try {
			socket.close();
		} catch (IOException ignored) {
			// Không cần xử lý.
		}
	}

	@Override
	public synchronized void stop() {
		if (!running) {
			return;
		}
		running = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			log.warn("TCP: lỗi khi đóng cổng: {}", e.getMessage());
		}
		pool.shutdownNow();
		log.info("Server TCP import đã dừng");
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	/** Cổng thực tế (khi cấu hình cổng 0 thì hệ điều hành chọn). -1 nếu chưa chạy. */
	public int getLocalPort() {
		return running ? serverSocket.getLocalPort() : -1;
	}
}
