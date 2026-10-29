package com.ltmtu.ielts.paper;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static com.ltmtu.ielts.support.ApiStatus.apiStatusOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;

/**
 * Thư viện trích dẫn của nhóm và tra DOI (B24a, UC-36, BR-30). Tra DOI gọi một máy chủ giả ở cổng 18089
 * ({@code app.paper.doi-base-url} của profile test) — không gọi ra internet.
 */
class PaperReferenceIntegrationTest extends PaperTestSupport {

	private static HttpServer doiServer;
	private static final AtomicReference<String> lastAccept = new AtomicReference<>();

	@Autowired
	private DoiThrottle doiThrottle;

	private String teacher;
	private User a;
	private User b;
	private User other;
	private long assignmentId;
	private long groupId;
	private long otherGroupId;

	@BeforeAll
	static void startDoiServer() throws IOException {
		doiServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 18089), 0);
		doiServer.createContext("/", PaperReferenceIntegrationTest::answerDoi);
		doiServer.start();
	}

	@AfterAll
	static void stopDoiServer() {
		doiServer.stop(0);
	}

	/** Bắt chước doi.org: một DOI có bản ghi "nặng" như Crossref trả, một DOI không có, một DOI làm dịch vụ lỗi. */
	private static void answerDoi(HttpExchange ex) throws IOException {
		lastAccept.set(ex.getRequestHeaders().getFirst("Accept"));
		String path = ex.getRequestURI().getPath();
		int status;
		String body;
		if (path.equals("/10.1145/3368089.3409731")) {
			status = 200;
			body = """
					{"type": "paper-conference", "title": "Deep learning for code review", "DOI": "10.1145/3368089.3409731",
					 "author": [{"given": "An", "family": "Nguyễn", "affiliation": [{"name": "HUST"}]}],
					 "issued": {"date-parts": [[2020, 11]]}, "container-title": "Proc. ESEC/FSE",
					 "reference": [{"key": "ref1", "unstructured": "..."}], "reference-count": 42, "indexed": {"x": 1}}""";
		} else if (path.equals("/10.1000/broken")) {
			status = 500;
			body = "lỗi";
		} else {
			status = 404;
			body = "DOI Not Found";
		}
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		ex.sendResponseHeaders(status, bytes.length);
		ex.getResponseBody().write(bytes);
		ex.close();
	}

	@BeforeEach
	void setUp() throws Exception {
		doiThrottle.clear();
		teacher = accessToken(newUser(Role.TEACHER));
		a = newUser(Role.STUDENT);
		b = newUser(Role.STUDENT);
		other = newUser(Role.STUDENT);
		assignmentId = publishedPaper(teacher, Map.of(), a, b, other);
		groupId = createGroup("Nhóm AB", a, b);
		otherGroupId = createGroup("Nhóm C", other);
	}

	private long createGroup(String name, User... members) throws Exception {
		JsonNode v = data(call(postJson("/api/teacher/assignments/" + assignmentId + "/groups",
				Map.of("name", name, "studentIds", java.util.Arrays.stream(members).map(User::getId).toList())), teacher));
		for (JsonNode g : v.get("groups")) {
			if (g.get("name").asString().equals(name)) {
				return g.get("id").asLong();
			}
		}
		throw new AssertionError("Không tạo được nhóm " + name);
	}

	private String url(long gid) {
		return "/api/papers/groups/" + gid + "/references";
	}

	private static Map<String, Object> article(String title, String doi) {
		return doi == null ? Map.of("title", title) : Map.of("title", title, "DOI", doi);
	}

	private MockHttpServletRequestBuilder putJson(String path, Object body) {
		return put(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
	}

	@Test
	@DisplayName("BR-30: thêm tài liệu → cả nhóm và giáo viên chủ bài thấy, kèm trường còn thiếu; người ngoài nhóm → 404")
	void addAndList() throws Exception {
		JsonNode added = data(call(postJson(url(groupId), Map.of("csl", Map.of("title", "Attention is all you need",
				"author", List.of(Map.of("family", "Vaswani")), "DOI", "https://doi.org/10.48550/ARXIV.1706.03762"))),
				accessToken(a)));
		assertThat(added.get("doi").asString()).isEqualTo("10.48550/arxiv.1706.03762");
		assertThat(added.get("createdByName").asString()).isEqualTo(a.getFullName());
		assertThat(added.get("missing").toString()).isEqualTo("[\"YEAR\",\"SOURCE\"]");

		assertThat(data(call(get(url(groupId)), accessToken(b)))).hasSize(1);
		assertThat(data(call(get(url(groupId)), teacher)).get(0).get("csl").get("title").asString())
				.isEqualTo("Attention is all you need");
		call(get(url(groupId)), accessToken(other)).andExpect(apiStatus(404));
		call(get(url(groupId)), accessToken(newUser(Role.TEACHER))).andExpect(apiStatus(404));
		call(postJson(url(groupId), Map.of("csl", article("X", null))), accessToken(other)).andExpect(apiStatus(404));
		call(postJson(url(groupId), Map.of("csl", article("X", null))), teacher).andExpect(apiStatus(403));
	}

	@Test
	@DisplayName("Trùng DOI trong thư viện nhóm (khác hoa thường / tiền tố) → 409 DUPLICATE; nhóm khác thêm được")
	void duplicateDoi() throws Exception {
		call(postJson(url(groupId), Map.of("csl", article("A", "10.1000/abc"))), accessToken(a)).andExpect(apiStatus(200));
		call(postJson(url(groupId), Map.of("csl", article("A lần 2", "doi:10.1000/ABC"))), accessToken(b))
				.andExpect(apiStatus(409)).andExpect(jsonPath("$.errorCode").value("DUPLICATE"));
		call(postJson(url(otherGroupId), Map.of("csl", article("A", "10.1000/abc"))), accessToken(other))
				.andExpect(apiStatus(200));
	}

	@Test
	@DisplayName("Nhập nhiều mục: bỏ qua DOI đã có và DOI lặp trong lần nhập; một mục sai → 400 cả lần, ghi số thứ tự")
	void batch() throws Exception {
		call(postJson(url(groupId), Map.of("csl", article("Có rồi", "10.1000/old"))), accessToken(a))
				.andExpect(apiStatus(200));
		JsonNode r = data(call(postJson(url(groupId) + "/batch", Map.of("items", List.of(article("Cũ", "10.1000/OLD"),
				article("Mới", "10.1000/new"), article("Mới lặp", "10.1000/new"), article("Không DOI", null)))),
				accessToken(b)));
		assertThat(r.get("added")).hasSize(2);
		assertThat(r.get("skipped").asInt()).isEqualTo(2);
		assertThat(data(call(get(url(groupId)), accessToken(a)))).hasSize(3);

		call(postJson(url(groupId) + "/batch", Map.of("items", List.of(article("Đúng", null), Map.of("author", List.of())))),
				accessToken(a)).andExpect(apiStatus(400))
				.andExpect(jsonPath("$.errorMessage").value("Mục thứ 2: Tài liệu tham khảo cần có tiêu đề"));
		assertThat(data(call(get(url(groupId)), accessToken(a)))).hasSize(3);
	}

	@Test
	@DisplayName("Sửa / xoá: PUT thay toàn bộ; đổi sang DOI của mục khác → 409; mục của nhóm khác → 404")
	void updateAndDelete() throws Exception {
		long first = data(call(postJson(url(groupId), Map.of("csl", article("Một", "10.1000/one"))), accessToken(a)))
				.get("id").asLong();
		long second = data(call(postJson(url(groupId), Map.of("csl", article("Hai", null))), accessToken(a))).get("id")
				.asLong();
		long foreign = data(call(postJson(url(otherGroupId), Map.of("csl", article("Ngoài", null))), accessToken(other)))
				.get("id").asLong();

		JsonNode updated = data(call(putJson(url(groupId) + "/" + second,
				Map.of("csl", Map.of("title", "Hai (sửa)", "publisher", "MIT Press"))), accessToken(b)));
		assertThat(updated.get("csl").get("title").asString()).isEqualTo("Hai (sửa)");
		assertThat(updated.get("missing").toString()).isEqualTo("[\"AUTHOR\",\"YEAR\"]");
		call(putJson(url(groupId) + "/" + second, Map.of("csl", article("Hai", "10.1000/one"))), accessToken(b))
				.andExpect(apiStatus(409));
		call(putJson(url(groupId) + "/" + foreign, Map.of("csl", article("Cướp", null))), accessToken(a))
				.andExpect(apiStatus(404));
		call(delete(url(groupId) + "/" + foreign), accessToken(a)).andExpect(apiStatus(404));

		call(delete(url(groupId) + "/" + first), accessToken(b)).andExpect(apiStatus(200));
		assertThat(data(call(get(url(groupId)), accessToken(a)))).hasSize(1);
	}

	@Test
	@DisplayName("Bài đã đóng → không sửa thư viện (409), vẫn xem được; nhóm có thư viện → giáo viên không xoá nhóm (409)")
	void closedAndGroupWork() throws Exception {
		call(postJson(url(groupId), Map.of("csl", article("A", null))), accessToken(a)).andExpect(apiStatus(200));
		call(delete("/api/teacher/assignments/" + assignmentId + "/groups/" + groupId), teacher)
				.andExpect(apiStatus(409));

		call(post("/api/teacher/assignments/" + assignmentId + "/close"), teacher).andExpect(apiStatus(200));
		call(postJson(url(groupId), Map.of("csl", article("B", null))), accessToken(a)).andExpect(apiStatus(409))
				.andExpect(jsonPath("$.errorCode").value("INVALID_STATE"));
		assertThat(data(call(get(url(groupId)), accessToken(b)))).hasSize(1);
	}

	@Test
	@DisplayName("Hai thành viên thêm cùng một DOI cùng lúc → khoá dòng nhóm, đúng một người được (200 + 409)")
	void concurrentSameDoi() throws Exception {
		CountDownLatch go = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			List<Future<Integer>> results = new ArrayList<>();
			for (String token : List.of(accessToken(a), accessToken(b))) {
				Callable<Integer> task = () -> {
					go.await();
					return apiStatusOf(call(postJson(url(groupId), Map.of("csl", article("Đua", "10.1000/race"))), token)
							.andReturn());
				};
				results.add(pool.submit(task));
			}
			go.countDown();
			assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(200, 409);
		} finally {
			pool.shutdown();
		}
		assertThat(data(call(get(url(groupId)), accessToken(a)))).hasSize(1);
	}

	@Test
	@DisplayName("Tra DOI: gửi Accept CSL-JSON, lọc bản ghi; không có DOI → 404; dịch vụ lỗi → 503; DOI sai → 400")
	void lookupDoi() throws Exception {
		String token = accessToken(a);
		JsonNode r = data(call(get("/api/papers/doi").param("value", "https://doi.org/10.1145/3368089.3409731"), token));
		assertThat(lastAccept.get()).isEqualTo(DoiResolver.CSL_JSON);
		assertThat(r.get("doi").asString()).isEqualTo("10.1145/3368089.3409731");
		assertThat(r.get("csl").get("type").asString()).isEqualTo("paper-conference");
		assertThat(r.get("csl").get("author").get(0).get("family").asString()).isEqualTo("Nguyễn");
		assertThat(r.get("csl").has("reference")).isFalse();
		assertThat(r.get("csl").get("author").get(0).has("affiliation")).isFalse();
		assertThat(r.get("missing")).isEmpty();

		call(get("/api/papers/doi").param("value", "10.1000/khong-co"), token).andExpect(apiStatus(404));
		call(get("/api/papers/doi").param("value", "10.1000/broken"), token).andExpect(apiStatus(503))
				.andExpect(jsonPath("$.errorCode").value("DOI_UNAVAILABLE"));
		call(get("/api/papers/doi").param("value", "không phải doi"), token).andExpect(apiStatus(400));
	}

	@Test
	@DisplayName("Tra DOI quá 20 lần / phút / người → 429 RATE_LIMITED; người khác vẫn tra được")
	void lookupDoiRateLimited() throws Exception {
		String token = accessToken(a);
		for (int i = 0; i < 20; i++) {
			call(get("/api/papers/doi").param("value", "10.1000/x" + i), token).andExpect(apiStatus(404));
		}
		call(get("/api/papers/doi").param("value", "10.1000/x"), token).andExpect(apiStatus(429))
				.andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"));
		call(get("/api/papers/doi").param("value", "10.1000/x"), accessToken(b)).andExpect(apiStatus(404));
	}
}
