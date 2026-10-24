package com.ltmtu.ielts.tcpgateway;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.http.HttpStatus;

import com.ltmtu.ielts.classroom.StudentImportService;
import com.ltmtu.ielts.classroom.StudentImportService.CsvImportResult;
import com.ltmtu.ielts.classroom.StudentImportService.NewAccount;
import com.ltmtu.ielts.classroom.dto.ImportDtos.ImportReport;
import com.ltmtu.ielts.classroom.dto.ImportDtos.RowError;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.security.AccessTokenVerifier;
import com.ltmtu.ielts.tcpgateway.TcpCommand.Auth;
import com.ltmtu.ielts.tcpgateway.TcpCommand.Import;
import com.ltmtu.ielts.tcpgateway.TcpCommand.Ping;
import com.ltmtu.ielts.tcpgateway.TcpCommand.Quit;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.SessionAuthView;

import lombok.extern.slf4j.Slf4j;

/**
 * Một phiên TCP (một kết nối), chạy trên một luồng của pool — máy trạng thái docs/05 mục C.5. Lỗi gửi
 * {@code ERR <code> <message>} rồi đóng kết nối, trừ lỗi 400 khi đã xác thực. Không ghi log token, nội dung CSV hay
 * mật khẩu tạm; chỉ ghi id.
 */
@Slf4j
class TcpImportSession implements Runnable {

	static final int MAX_LINE_BYTES = 1024;

	private enum State {
		CONNECTED, AUTHENTICATED
	}

	private final Socket socket;
	private final AccessTokenVerifier verifier;
	private final StudentImportService importService;
	private final int readTimeoutMillis;
	private final long deadlineNanos;
	private final int maxPayload;

	private State state = State.CONNECTED;
	private SessionAuthView user;
	private OutputStream out;

	TcpImportSession(Socket socket, AccessTokenVerifier verifier, StudentImportService importService,
			Duration readTimeout, Duration sessionTimeout, int maxPayload) {
		this.socket = socket;
		this.verifier = verifier;
		this.importService = importService;
		this.readTimeoutMillis = (int) readTimeout.toMillis();
		this.deadlineNanos = System.nanoTime() + sessionTimeout.toNanos();
		this.maxPayload = maxPayload;
	}

	@Override
	public void run() {
		// Không dùng try-with-resources: socket phải còn mở khi khối catch gửi ERR cho client.
		try {
			out = socket.getOutputStream();
			FrameReader reader = new FrameReader(new BufferedInputStream(socket.getInputStream()), MAX_LINE_BYTES);
			loop(reader);
		} catch (SocketTimeoutException e) {
			trySend("ERR 408 Quá thời gian chờ dữ liệu");
		} catch (EOFException e) {
			log.debug("TCP {}: client đóng kết nối giữa chừng", socket.getRemoteSocketAddress());
		} catch (IOException | UncheckedIOException e) {
			log.debug("TCP {}: lỗi kết nối: {}", socket.getRemoteSocketAddress(), e.getMessage());
		} catch (RuntimeException e) {
			log.error("TCP {}: lỗi không mong muốn", socket.getRemoteSocketAddress(), e);
			trySend("ERR 500 Lỗi server");
		} finally {
			try {
				socket.close();
			} catch (IOException ignored) {
				// Đã đóng.
			}
		}
	}

	private void loop(FrameReader reader) throws IOException {
		while (true) {
			String line = readLine(reader);
			if (line == null) {
				return;
			}
			TcpCommand command;
			try {
				command = TcpCommand.parse(line);
			} catch (TcpException e) {
				if (!error(e)) {
					return;
				}
				continue;
			}
			try {
				if (!handle(command, reader)) {
					return;
				}
			} catch (TcpException e) {
				if (!error(e)) {
					return;
				}
			}
		}
	}

	/** Xử lý một lệnh; {@code false} = đóng kết nối. */
	private boolean handle(TcpCommand command, FrameReader reader) throws IOException {
		return switch (command) {
			case Quit q -> {
				send("BYE");
				yield false;
			}
			case Auth a -> {
				requireState(State.CONNECTED, "Đã xác thực rồi");
				authenticate(a.token());
				send("OK");
				yield true;
			}
			case Ping p -> {
				requireState(State.AUTHENTICATED, "Cần AUTH trước");
				send("PONG");
				yield true;
			}
			case Import i -> {
				requireState(State.AUTHENTICATED, "Cần AUTH trước");
				importCsv(i, reader);
				yield false;
			}
		};
	}

	private void authenticate(String token) {
		user = verifier.verifyHeader("Bearer " + token)
				.orElseThrow(() -> new TcpException(401, "Token sai hoặc hết hạn"));
		if (user.role() != Role.ADMIN && user.role() != Role.TEACHER) {
			throw new TcpException(403, "Chỉ admin hoặc giáo viên được import qua TCP");
		}
		if (user.mustChangePassword()) {
			throw new TcpException(403, "Tài khoản phải đổi mật khẩu trước");
		}
		state = State.AUTHENTICATED;
		log.info("TCP {}: người dùng {} đã xác thực", socket.getRemoteSocketAddress(), user.userId());
	}

	private void importCsv(Import cmd, FrameReader reader) throws IOException {
		if (cmd.byteLength() > maxPayload) {
			throw new TcpException(413, "Dữ liệu vượt " + maxPayload + " byte");
		}
		boolean admin = user.role() == Role.ADMIN;
		CsvImportResult result;
		try {
			importService.requireImportableClass(user.userId(), admin, cmd.classCode());
			applyTimeout();
			byte[] csv = reader.readExactly(cmd.byteLength());
			result = importService.importCsv(user.userId(), admin, cmd.classCode(), csv, cmd.sendEmail(),
					socket.getInetAddress().getHostAddress(), (done, total) -> sendUnchecked("PROGRESS " + done + "/" + total));
		} catch (ApiException e) {
			throw toTcp(e);
		}
		ImportReport r = result.report();
		send("DONE created=" + r.created() + " existing=" + r.existing() + " skipped=" + r.skipped() + " errors="
				+ r.errors().size());
		for (RowError e : r.errors()) {
			send("ERRLINE " + e.row() + " " + oneLine(e.reason()));
		}
		for (NewAccount a : result.accounts()) {
			send("CRED " + a.studentCode() + "|" + oneLine(a.fullName()).replace('|', '/') + "|" + a.temporaryPassword());
		}
		send("END");
	}

	/** Lỗi nghiệp vụ → mã TCP (docs/05 mục C.4). */
	private static TcpException toTcp(ApiException e) {
		if ("BAD_FILE".equals(e.getCode())) {
			return new TcpException(415, e.getMessage());
		}
		HttpStatus status = e.getStatus();
		int code = switch (status) {
			case NOT_FOUND, FORBIDDEN, BAD_REQUEST, UNAUTHORIZED -> status.value();
			default -> 500;
		};
		return new TcpException(code, e.getMessage());
	}

	/** Gửi lỗi; {@code true} = giữ kết nối (chỉ lỗi 400 khi đã xác thực). */
	private boolean error(TcpException e) throws IOException {
		send("ERR " + e.code() + " " + oneLine(e.getMessage()));
		return e.code() == 400 && state == State.AUTHENTICATED;
	}

	private void requireState(State expected, String message) {
		if (state != expected) {
			throw new TcpException(400, message);
		}
	}

	private String readLine(FrameReader reader) throws IOException {
		applyTimeout();
		return reader.readLine();
	}

	/** Timeout đọc = min(timeout giữa hai lần nhận, thời gian phiên còn lại). Hết phiên → ERR 408. */
	private void applyTimeout() throws IOException {
		long remainingMillis = (deadlineNanos - System.nanoTime()) / 1_000_000;
		if (remainingMillis <= 0) {
			throw new SocketTimeoutException("Hết thời gian phiên");
		}
		socket.setSoTimeout((int) Math.min(readTimeoutMillis, remainingMillis));
	}

	private void send(String line) throws IOException {
		out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
		out.flush();
	}

	private void sendUnchecked(String line) {
		try {
			send(line);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void trySend(String line) {
		try {
			send(line);
		} catch (IOException | RuntimeException ignored) {
			// Kết nối đã hỏng, không còn gì để báo.
		}
	}

	private static String oneLine(String s) {
		return s == null ? "" : s.replace('\r', ' ').replace('\n', ' ');
	}
}
