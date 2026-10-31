package com.ltmtu.ielts.paper;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** H4: giáo viên nhận xét / chỉ lỗi trên Paper đang viết — docs/05 mục A.10. */
@RecordApplicationEvents
class PaperLiveCommentIntegrationTest extends PaperTestSupport {

	@Autowired
	private PaperLiveCommentService commentService;

	@Autowired
	private PaperChannelRule channelRule;

	@Autowired
	private ApplicationEvents events;

	private User teacherUser;
	private String teacher;
	private User a;
	private long assignmentId;
	private long documentId;

	@BeforeEach
	void setUp() throws Exception {
		documentService.clear();
		commentService.clear();
		teacherUser = newUser(Role.TEACHER);
		teacher = accessToken(teacherUser);
		a = newUser(Role.STUDENT);
		assignmentId = publishedPaper(teacher, Map.of(), a);
		long partId = data(call(get("/api/teacher/assignments/" + assignmentId), teacher)).get("parts").get(0)
				.get("id").asLong();
		call(postJson("/api/teacher/assignments/" + assignmentId + "/groups",
				Map.of("name", "Nhóm A", "studentIds", List.of(a.getId()))), teacher).andExpect(apiStatus(200));
		documentId = openDocument(a, partId);
	}

	private String url() {
		return "/api/teacher/papers/docs/" + documentId + "/comments";
	}

	private static Map<String, Object> body(String kind, String comment) {
		Map<String, Object> b = new HashMap<>();
		b.put("startOffset", 3);
		b.put("endOffset", 12);
		b.put("quote", "đoạn được chọn");
		b.put("comment", comment);
		b.put("kind", kind);
		b.put("suggestion", "Viết lại cho rõ");
		b.put("sectionKey", "introduction");
		return b;
	}

	private MockHttpServletRequestBuilder putJson(String url, Object body) {
		return put(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
	}

	@Test
	@DisplayName("H4: giáo viên chủ bài tạo phản hồi có loại + gợi ý + mục; sinh viên trong nhóm đọc được; phát CREATED")
	void createAndList() throws Exception {
		JsonNode created = data(call(postJson(url(), body("GRAMMAR", "Sai thì động từ")), teacher));
		assertThat(created.get("kind").asString()).isEqualTo("GRAMMAR");
		assertThat(created.get("suggestion").asString()).isEqualTo("Viết lại cho rõ");
		assertThat(created.get("sectionKey").asString()).isEqualTo("introduction");
		assertThat(created.get("resolvedAt").isNull()).isTrue();

		JsonNode list = data(call(get("/api/papers/docs/" + documentId + "/comments"), accessToken(a)));
		assertThat(list).hasSize(1);
		assertThat(list.get(0).get("comment").asString()).isEqualTo("Sai thì động từ");
		assertThat(events.stream(PaperLiveCommentEvents.Changed.class).map(PaperLiveCommentEvents.Changed::type))
				.containsExactly("CREATED");
	}

	@Test
	@DisplayName("H4: sinh viên / giáo viên khác không tạo được; người ngoài không đọc được; tài liệu không tồn tại → 404")
	void permissions() throws Exception {
		call(postJson(url(), body("COMMENT", "x")), accessToken(a)).andExpect(apiStatus(403));
		String other = accessToken(newUser(Role.TEACHER));
		call(postJson(url(), body("COMMENT", "x")), other).andExpect(apiStatus(404));
		call(get("/api/papers/docs/" + documentId + "/comments"), other).andExpect(apiStatus(404));
		call(get("/api/papers/docs/" + documentId + "/comments"), accessToken(newUser(Role.STUDENT)))
				.andExpect(apiStatus(404));
		call(postJson("/api/teacher/papers/docs/999999999/comments", body("COMMENT", "x")), teacher)
				.andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("H4: kênh STOMP /topic/paper/{id}/comments — chỉ thành viên nhóm và giáo viên chủ bài")
	void stompRule() {
		String topic = "/topic/paper/" + documentId + "/comments";
		assertThat(channelRule.canSubscribe(principal(a, Role.STUDENT), topic)).isTrue();
		assertThat(channelRule.canSubscribe(principal(teacherUser, Role.TEACHER), topic)).isTrue();
		User stranger = newUser(Role.STUDENT);
		assertThat(channelRule.canSubscribe(principal(stranger, Role.STUDENT), topic)).isFalse();
	}

	@Test
	@DisplayName("H4: validation — nội dung trống, quote quá 2000 ký tự, offset ngược, loại lạ → 400")
	void validation() throws Exception {
		call(postJson(url(), body("COMMENT", " ")), teacher).andExpect(apiStatus(400));
		Map<String, Object> longQuote = body("COMMENT", "ok");
		longQuote.put("quote", "a".repeat(2001));
		call(postJson(url(), longQuote), teacher).andExpect(apiStatus(400));
		Map<String, Object> reversed = body("COMMENT", "ok");
		reversed.put("startOffset", 20);
		call(postJson(url(), reversed), teacher).andExpect(apiStatus(400));
		call(postJson(url(), body("SPELLING", "ok")), teacher).andExpect(apiStatus(400));
	}

	@Test
	@DisplayName("H4: chỉ tác giả sửa / xoá; sinh viên đánh dấu đã xử lý rồi mở lại; phát UPDATED, RESOLVED, DELETED")
	void updateResolveDelete() throws Exception {
		long id = data(call(postJson(url(), body("COMMENT", "Cần dẫn nguồn")), teacher)).get("id").asLong();

		call(putJson(url() + "/" + id, Map.of("comment", "Cần dẫn nguồn [1]", "kind", "CITATION")), teacher)
				.andExpect(apiStatus(200))
				.andExpect(jsonPath("$.data.kind").value("CITATION"))
				.andExpect(jsonPath("$.data.quote").value("đoạn được chọn"));
		call(putJson(url() + "/" + id, Map.of("comment", "x")), accessToken(a)).andExpect(apiStatus(403));

		String resolveUrl = "/api/papers/docs/" + documentId + "/comments/" + id + "/resolve";
		JsonNode resolved = data(call(putJson(resolveUrl, Map.of("resolved", true)), accessToken(a)));
		assertThat(resolved.get("resolvedAt").isNull()).isFalse();
		assertThat(resolved.get("resolvedByName").asString()).isEqualTo(a.getFullName());
		JsonNode reopened = data(call(putJson(resolveUrl, Map.of("resolved", false)), teacher));
		assertThat(reopened.get("resolvedAt").isNull()).isTrue();

		call(delete(url() + "/" + id), teacher).andExpect(apiStatus(200));
		call(delete(url() + "/" + id), teacher).andExpect(apiStatus(404));
		assertThat(events.stream(PaperLiveCommentEvents.Changed.class).map(PaperLiveCommentEvents.Changed::type))
				.containsExactly("CREATED", "UPDATED", "RESOLVED", "RESOLVED", "DELETED");
	}

	@Test
	@DisplayName("H4: giáo viên chủ bài thấy danh sách tài liệu nhóm kèm số nhận xét chưa xử lý; người khác → 404")
	void teacherDocuments() throws Exception {
		call(postJson(url(), body("COMMENT", "Một")), teacher).andExpect(apiStatus(200));
		String docsUrl = "/api/teacher/assignments/" + assignmentId + "/groups/documents";

		JsonNode docs = data(call(get(docsUrl), teacher));
		assertThat(docs).hasSize(1);
		assertThat(docs.get(0).get("documentId").asLong()).isEqualTo(documentId);
		assertThat(docs.get(0).get("groupName").asString()).isEqualTo("Nhóm A");
		assertThat(docs.get(0).get("members").asString()).isEqualTo(a.getFullName());
		assertThat(docs.get(0).get("openComments").asLong()).isEqualTo(1);
		call(get(docsUrl), accessToken(newUser(Role.TEACHER))).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("H4: quá 30 lần ghi / phút → 429 RATE_LIMITED")
	void rateLimited() throws Exception {
		for (int i = 0; i < PaperLiveCommentService.MAX_WRITES_PER_MINUTE; i++) {
			call(postJson(url(), body("COMMENT", "n" + i)), teacher).andExpect(apiStatus(200));
		}
		call(postJson(url(), body("COMMENT", "thêm")), teacher)
				.andExpect(apiStatus(429))
				.andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"));
	}

	private static AuthPrincipal principal(User u, Role role) {
		return new AuthPrincipal(u.getId(), u.getUsername(), role, 0L);
	}
}
