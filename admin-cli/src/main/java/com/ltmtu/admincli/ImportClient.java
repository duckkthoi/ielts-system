package com.ltmtu.admincli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Client của giao thức TCP import sinh viên (docs/05 phần C): gửi lệnh từng dòng, gửi phần CSV đúng số byte đã khai
 * báo, đọc phản hồi từng dòng. Chỉ dùng {@link Socket} của JDK.
 */
public final class ImportClient implements AutoCloseable {

	/** Phản hồi {@code ERR <code> <message>} của server. */
	public static final class ServerError extends IOException {

		private final int code;

		ServerError(int code, String message) {
			super(message);
			this.code = code;
		}

		public int code() {
			return code;
		}
	}

	public record RowError(int row, String reason) {
	}

	public record Credential(String studentCode, String fullName, String temporaryPassword) {
	}

	public record Result(int created, int existing, int skipped, List<RowError> errors, List<Credential> credentials) {
	}

	private final Socket socket;
	private final OutputStream out;
	private final BufferedReader in;

	private ImportClient(Socket socket) throws IOException {
		this.socket = socket;
		this.out = socket.getOutputStream();
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
	}

	/** Kết nối; {@code timeoutMillis} dùng cho cả lúc kết nối và mỗi lần chờ phản hồi. */
	public static ImportClient connect(String host, int port, int timeoutMillis) throws IOException {
		Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(host, port), timeoutMillis);
			socket.setSoTimeout(timeoutMillis);
			return new ImportClient(socket);
		} catch (IOException e) {
			socket.close();
			throw e;
		}
	}

	public void auth(String accessToken) throws IOException {
		send("AUTH " + accessToken);
		expect("OK");
	}

	public void ping() throws IOException {
		send("PING");
		expect("PONG");
	}

	public void quit() throws IOException {
		send("QUIT");
		expect("BYE");
	}

	/**
	 * {@code IMPORT <classCode> <byteLength> [EMAIL]} rồi gửi nội dung CSV. {@code progress} nhận (đã xử lý, tổng) ở
	 * mỗi dòng {@code PROGRESS}. Server đóng kết nối sau {@code END}.
	 */
	public Result importCsv(String classCode, byte[] csv, boolean sendEmail, BiConsumer<Integer, Integer> progress)
			throws IOException {
		send("IMPORT " + classCode + " " + csv.length + (sendEmail ? " EMAIL" : ""));
		out.write(csv);
		out.flush();

		int created = 0;
		int existing = 0;
		int skipped = 0;
		List<RowError> errors = new ArrayList<>();
		List<Credential> credentials = new ArrayList<>();
		while (true) {
			String line = readLine();
			if (line.equals("END")) {
				return new Result(created, existing, skipped, errors, credentials);
			}
			String[] parts = line.split(" ", 2);
			String rest = parts.length > 1 ? parts[1] : "";
			switch (parts[0]) {
				case "PROGRESS" -> {
					String[] n = rest.split("/");
					progress.accept(Integer.parseInt(n[0]), Integer.parseInt(n[1]));
				}
				case "DONE" -> {
					for (String kv : rest.split(" ")) {
						String[] p = kv.split("=");
						int v = Integer.parseInt(p[1]);
						switch (p[0]) {
							case "created" -> created = v;
							case "existing" -> existing = v;
							case "skipped" -> skipped = v;
							default -> {
								// "errors" đếm lại từ các dòng ERRLINE.
							}
						}
					}
				}
				case "ERRLINE" -> {
					String[] p = rest.split(" ", 2);
					errors.add(new RowError(Integer.parseInt(p[0]), p.length > 1 ? p[1] : ""));
				}
				case "CRED" -> {
					String[] p = rest.split("\\|", 3);
					credentials.add(new Credential(p[0], p[1], p[2]));
				}
				default -> throw new IOException("Phản hồi không hiểu được từ server: " + line);
			}
		}
	}

	private void send(String line) throws IOException {
		out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
		out.flush();
	}

	private void expect(String expected) throws IOException {
		String line = readLine();
		if (!line.equals(expected)) {
			throw new IOException("Server trả \"" + line + "\", cần \"" + expected + "\"");
		}
	}

	/** Một dòng phản hồi; {@code ERR …} → {@link ServerError}; server đóng kết nối bất ngờ → lỗi. */
	private String readLine() throws IOException {
		String line = in.readLine();
		if (line == null) {
			throw new IOException("Server đã đóng kết nối");
		}
		if (line.startsWith("ERR ")) {
			String[] p = line.split(" ", 3);
			int code;
			try {
				code = Integer.parseInt(p[1]);
			} catch (NumberFormatException e) {
				code = 500;
			}
			throw new ServerError(code, p.length > 2 ? p[2] : "");
		}
		return line;
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}
}
