package com.ltmtu.ielts.paper;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import com.ltmtu.ielts.support.StompTestClient;
import com.ltmtu.ielts.support.StompTestClient.Connection;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Tài liệu paper cùng sửa theo Yjs qua WebSocket thật (B22, docs/12 mục 5.2, BR-28, BR-32). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaperDocumentIntegrationTest extends PaperTestSupport {

	@LocalServerPort
	private int port;

	@Autowired
	private PaperDocUpdateRepository updateRepository;

	private StompTestClient stomp;
	private String teacher;
	private User a;
	private User b;
	private User loner;
	private long assignmentId;
	private long partId;

	@BeforeEach
	void setUp() throws Exception {
		documentService.clear();
		stomp = new StompTestClient(port, json);
		teacher = accessToken(newUser(Role.TEACHER));
		a = newUser(Role.STUDENT);
		b = newUser(Role.STUDENT);
		loner = newUser(Role.STUDENT);
		assignmentId = publishedPaper(teacher,
				Map.of("parts", List.of(stage("Bản cuối", List.of(section("Abstract", true))))), a, b, loner);
		partId = data(call(get("/api/teacher/assignments/" + assignmentId), teacher)).get("parts").get(0).get("id")
				.asLong();
		call(postJson("/api/teacher/assignments/" + assignmentId + "/groups",
				Map.of("name", "Nhóm AB", "studentIds", List.of(a.getId(), b.getId()))), teacher).andExpect(apiStatus(200));
	}

	@AfterEach
	void tearDown() {
		stomp.close();
	}

	@Test
	@DisplayName("BR-28: mở tài liệu — cả nhóm dùng chung một tài liệu; chưa có nhóm → 409 NO_GROUP; không được giao → 404")
	void open() throws Exception {
		JsonNode docA = data(call(get(docUrl()), accessToken(a)));
		assertThat(docA.get("lastSeq").asLong()).isZero();
		assertThat(docA.get("snapshot").isNull()).isTrue();
		assertThat(docA.get("editable").asBoolean()).isTrue();
		assertThat(data(call(get(docUrl()), accessToken(b))).get("documentId").asLong())
				.isEqualTo(docA.get("documentId").asLong());
		call(get(docUrl()), accessToken(loner)).andExpect(apiStatus(409))
				.andExpect(jsonPath("$.errorCode").value("NO_GROUP"));
		call(get(docUrl()), accessToken(newUser(Role.STUDENT))).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("Update Yjs: server cấp seq tăng dần, phát cho cả nhóm + giáo viên (chỉ đọc); lấy bù theo afterSeq")
	void relayAndCatchUp() throws Exception {
		String ta = accessToken(a);
		long doc = openDoc(ta);
		BlockingQueue<JsonNode> toB = stomp.subscribe(stomp.connect(accessToken(b)), topic(doc));
		BlockingQueue<JsonNode> toTeacher = stomp.subscribe(stomp.connect(teacher), topic(doc));
		Connection ca = stomp.connect(ta);

		stomp.send(ca, sendUrl(doc), Map.of("update", b64(1, 2, 3), "clientId", "c-1", "charsAdded", 12));
		JsonNode first = toB.poll(5, TimeUnit.SECONDS);
		assertThat(first.get("seq").asLong()).isEqualTo(1);
		assertThat(first.get("authorId").asLong()).isEqualTo(a.getId());
		assertThat(first.get("clientId").asString()).isEqualTo("c-1");
		assertThat(first.get("update").asString()).isEqualTo(b64(1, 2, 3));
		assertThat(toTeacher.poll(5, TimeUnit.SECONDS).get("seq").asLong()).isEqualTo(1);

		Connection cb = stomp.connect(accessToken(b));
		stomp.send(cb, sendUrl(doc), Map.of("update", b64(4, 5), "clientId", "c-2"));
		assertThat(toB.poll(5, TimeUnit.SECONDS).get("seq").asLong()).isEqualTo(2);

		JsonNode after = data(call(get("/api/papers/docs/" + doc + "/updates").param("afterSeq", "1"), ta));
		assertThat(after.get("resync").asBoolean()).isFalse();
		assertThat(after.get("updates")).hasSize(1);
		assertThat(after.get("updates").get(0).get("seq").asLong()).isEqualTo(2);
		assertThat(data(call(get("/api/papers/docs/" + doc), teacher)).get("updates")).hasSize(2);
	}

	@Test
	@DisplayName("Snapshot: gộp tới upToSeq, update cũ bỏ nội dung nhưng giữ dòng (đóng góp); snapshot cũ hơn bị bỏ qua; vượt seq / JSON sai → 400")
	void snapshot() throws Exception {
		String ta = accessToken(a);
		long doc = openDoc(ta);
		Connection ca = stomp.connect(ta);
		BlockingQueue<JsonNode> echo = stomp.subscribe(ca, topic(doc));
		stomp.send(ca, sendUrl(doc), Map.of("update", b64(1), "clientId", "x", "charsAdded", 5));
		stomp.send(ca, sendUrl(doc), Map.of("update", b64(2), "clientId", "y", "charsAdded", 7));
		echo.poll(5, TimeUnit.SECONDS);
		echo.poll(5, TimeUnit.SECONDS);

		JsonNode s = data(snapshot(ta, doc, 2, "{\"type\":\"doc\"}"));
		assertThat(s.get("snapshotSeq").asLong()).isEqualTo(2);
		assertThat(s.get("updates")).isEmpty();
		assertThat(updateRepository.countByIdDocumentId(doc))
				.as("dòng update vẫn còn để thống kê đóng góp").isEqualTo(2);
		JsonNode after = data(call(get("/api/papers/docs/" + doc + "/updates").param("afterSeq", "0"), ta));
		assertThat(after.get("resync").asBoolean()).isTrue();

		assertThat(data(snapshot(ta, doc, 1, null)).get("snapshotSeq").asLong()).as("cũ hơn → bỏ qua").isEqualTo(2);
		snapshot(ta, doc, 9, null).andExpect(apiStatus(400));
		snapshot(ta, doc, 2, "{không phải json").andExpect(apiStatus(400));
		snapshot(accessToken(loner), doc, 2, null).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("Quyền: người ngoài nhóm nghe kênh → ERROR, gửi update → lỗi riêng 404; giáo viên gửi → ERROR; giáo viên khác đọc → 404")
	void access() throws Exception {
		long doc = openDoc(accessToken(a));
		Connection outsider = stomp.connect(accessToken(loner));
		outsider.session().subscribe(topic(doc), new StompSessionHandlerAdapter() {
		});
		assertThat(outsider.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền nhận kênh");

		Connection loner2 = stomp.connect(accessToken(loner));
		BlockingQueue<JsonNode> errors = stomp.subscribe(loner2, "/user/queue/errors");
		stomp.send(loner2, sendUrl(doc), Map.of("update", b64(1)));
		assertThat(errors.poll(5, TimeUnit.SECONDS).get("errorCode").asString()).isEqualTo("NOT_FOUND");

		Connection t = stomp.connect(teacher);
		stomp.send(t, sendUrl(doc), Map.of("update", b64(1)));
		assertThat(t.errors.poll(5, TimeUnit.SECONDS)).contains("Không có quyền gửi");
		call(get("/api/papers/docs/" + doc), accessToken(newUser(Role.TEACHER))).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("Nhóm đã nộp → tài liệu chỉ đọc (update bị từ chối INVALID_STATE); nhóm có chỉnh sửa → không tự rời nhóm")
	void lockedAfterSubmitAndGroupWork() throws Exception {
		String ta = accessToken(a);
		long doc = openDoc(ta);
		Connection ca = stomp.connect(ta);
		BlockingQueue<JsonNode> echo = stomp.subscribe(ca, topic(doc));
		BlockingQueue<JsonNode> errors = stomp.subscribe(ca, "/user/queue/errors");
		stomp.send(ca, sendUrl(doc), Map.of("update", b64(1)));
		assertThat(echo.poll(5, TimeUnit.SECONDS)).isNotNull();
		call(delete("/api/student/assignments/" + assignmentId + "/groups/mine"), accessToken(b)).andExpect(apiStatus(409));

		// B25a: nộp lấy snapshot của tài liệu, nên phải lưu snapshot tới thay đổi mới nhất trước.
		snapshot(a, doc, docJson(sectionsOf("abstract", "Bản nộp")), "Abstract Bản nộp");
		call(postJson("/api/student/parts/" + partId + "/submit", Map.of("expectedVersion", 0)), ta)
				.andExpect(apiStatus(200));
		assertThat(data(call(get(docUrl()), ta)).get("editable").asBoolean()).isFalse();
		stomp.send(ca, sendUrl(doc), Map.of("update", b64(2)));
		assertThat(errors.poll(5, TimeUnit.SECONDS).get("errorCode").asString()).isEqualTo("INVALID_STATE");
	}

	@Test
	@DisplayName("Hai thành viên gửi dồn ĐỒNG THỜI 2 × 10 update → seq 1…20 liên tiếp, không trùng, không mất")
	void concurrentUpdates() throws Exception {
		String ta = accessToken(a);
		long doc = openDoc(ta);
		BlockingQueue<JsonNode> watch = stomp.subscribe(stomp.connect(teacher), topic(doc));
		Connection ca = stomp.connect(ta);
		Connection cb = stomp.connect(accessToken(b));
		for (int i = 0; i < 10; i++) {
			stomp.send(ca, sendUrl(doc), Map.of("update", b64(i)));
			stomp.send(cb, sendUrl(doc), Map.of("update", b64(100 + i)));
		}
		Set<Long> seqs = new HashSet<>();
		for (int i = 0; i < 20; i++) {
			JsonNode e = watch.poll(5, TimeUnit.SECONDS);
			assertThat(e).as("nhận đủ 20 update").isNotNull();
			seqs.add(e.get("seq").asLong());
		}
		assertThat(seqs).hasSize(20).allMatch(s -> s >= 1 && s <= 20);
		assertThat(data(call(get("/api/papers/docs/" + doc), ta)).get("lastSeq").asLong()).isEqualTo(20);
	}

	@Test
	@DisplayName("Awareness (con trỏ, tên người đang sửa) chỉ chuyển tiếp cho nhóm, không lưu; update quá lớn → lỗi riêng 400")
	void awarenessAndLimits() throws Exception {
		String ta = accessToken(a);
		long doc = openDoc(ta);
		BlockingQueue<JsonNode> toB = stomp.subscribe(stomp.connect(accessToken(b)), topic(doc) + "/awareness");
		Connection ca = stomp.connect(ta);
		stomp.send(ca, "/app/paper/" + doc + "/awareness", Map.of("state", "AQID"));
		JsonNode aw = toB.poll(5, TimeUnit.SECONDS);
		assertThat(aw.get("userId").asLong()).isEqualTo(a.getId());
		assertThat(aw.get("userName").asString()).isEqualTo(a.getFullName());

		BlockingQueue<JsonNode> errors = stomp.subscribe(ca, "/user/queue/errors");
		stomp.send(ca, sendUrl(doc), Map.of("update", Base64.getEncoder().encodeToString(new byte[40_000])));
		assertThat(errors.poll(5, TimeUnit.SECONDS).get("errorCode").asString()).isEqualTo("VALIDATION_ERROR");
	}

	// ─── Hỗ trợ ──────────────────────────────────────────────────────────────

	private long openDoc(String token) throws Exception {
		return data(call(get(docUrl()), token)).get("documentId").asLong();
	}

	private org.springframework.test.web.servlet.ResultActions snapshot(String token, long doc, long upToSeq,
			String contentJson) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("state", b64(9, 9));
		body.put("upToSeq", upToSeq);
		body.put("contentJson", contentJson);
		body.put("contentText", "abc");
		return call(put("/api/papers/docs/" + doc + "/snapshot").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(body)), token);
	}

	private String docUrl() {
		return "/api/student/parts/" + partId + "/document";
	}

	private static String topic(long doc) {
		return "/topic/paper/" + doc;
	}

	private static String sendUrl(long doc) {
		return "/app/paper/" + doc + "/update";
	}

	private static String b64(int... bytes) {
		byte[] out = new byte[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			out[i] = (byte) bytes[i];
		}
		return Base64.getEncoder().encodeToString(out);
	}
}
