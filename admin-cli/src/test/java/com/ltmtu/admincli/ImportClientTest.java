package com.ltmtu.admincli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ltmtu.admincli.ImportClient.Result;
import com.ltmtu.admincli.ImportClient.ServerError;

/** Client TCP với một server giả: kiểm tra đúng khung tin gửi đi và cách đọc phản hồi (docs/05 mục C.6). */
class ImportClientTest {

	/** Server giả: nhận AUTH, IMPORT + đúng số byte, trả phản hồi mẫu; ghi lại những gì nhận được. */
	private static CompletableFuture<List<String>> fakeServer(ServerSocket ss, String... replies) {
		return CompletableFuture.supplyAsync(() -> {
			List<String> received = new ArrayList<>();
			try (Socket s = ss.accept()) {
				InputStream in = new BufferedInputStream(s.getInputStream());
				OutputStream out = s.getOutputStream();
				String auth = line(in);
				received.add(auth);
				out.write("OK\n".getBytes(StandardCharsets.UTF_8));
				String importLine = line(in);
				received.add(importLine);
				int length = Integer.parseInt(importLine.split(" ")[2]);
				received.add(new String(in.readNBytes(length), StandardCharsets.UTF_8));
				for (String r : replies) {
					out.write((r + "\n").getBytes(StandardCharsets.UTF_8));
				}
				out.flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return received;
		});
	}

	private static String line(InputStream in) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int b;
		while ((b = in.read()) != '\n') {
			buf.write(b);
		}
		return buf.toString(StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("Gửi AUTH, IMPORT với số byte UTF-8 đúng + EMAIL, rồi đọc PROGRESS / DONE / ERRLINE / CRED / END")
	void importSession() throws Exception {
		String csv = "ma_sv,ho_ten\n2151000001,Nguyễn Văn A\n21A,Lê C\n";
		try (ServerSocket ss = new ServerSocket(0)) {
			var server = fakeServer(ss, "PROGRESS 1/2", "PROGRESS 2/2", "DONE created=1 existing=0 skipped=0 errors=1",
					"ERRLINE 3 Mã sinh viên không hợp lệ", "CRED 2151000001|Nguyễn Văn A|k7Pq2xZm", "END");
			List<String> progress = new ArrayList<>();
			Result r;
			try (ImportClient c = ImportClient.connect("localhost", ss.getLocalPort(), 5000)) {
				c.auth("tok");
				r = c.importCsv("K15-A", csv.getBytes(StandardCharsets.UTF_8), true, (d, t) -> progress.add(d + "/" + t));
			}
			List<String> received = server.get(5, TimeUnit.SECONDS);
			assertEquals("AUTH tok", received.get(0));
			assertEquals("IMPORT K15-A " + csv.getBytes(StandardCharsets.UTF_8).length + " EMAIL", received.get(1));
			assertEquals(csv, received.get(2));
			assertEquals(List.of("1/2", "2/2"), progress);
			assertEquals(1, r.created());
			assertEquals(1, r.errors().size());
			assertEquals(3, r.errors().get(0).row());
			assertEquals("Mã sinh viên không hợp lệ", r.errors().get(0).reason());
			assertEquals("k7Pq2xZm", r.credentials().get(0).temporaryPassword());
			assertEquals("Nguyễn Văn A", r.credentials().get(0).fullName());
		}
	}

	@Test
	@DisplayName("Server trả ERR → ServerError mang mã lỗi và lời nhắn")
	void serverError() throws Exception {
		try (ServerSocket ss = new ServerSocket(0)) {
			CompletableFuture.runAsync(() -> {
				try (Socket s = ss.accept()) {
					line(s.getInputStream());
					s.getOutputStream().write("ERR 401 Token sai hoặc hết hạn\n".getBytes(StandardCharsets.UTF_8));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			try (ImportClient c = ImportClient.connect("localhost", ss.getLocalPort(), 5000)) {
				ServerError e = assertThrows(ServerError.class, () -> c.auth("sai"));
				assertEquals(401, e.code());
				assertEquals("Token sai hoặc hết hạn", e.getMessage());
			}
		}
	}
}
