package com.ltmtu.ielts.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.ltmtu.ielts.support.IntegrationTest;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Khoá lạc quan của bản nháp (SD-06, bước 10): 2 tab / 2 máy cùng sửa không ghi đè lên nhau. */
class DraftConflictIntegrationTest extends IntegrationTest {

	private String student;
	private long partId;

	@BeforeEach
	void setUp() throws Exception {
		String teacher = accessToken(newUser(Role.TEACHER));
		User s = newUser(Role.STUDENT);
		student = accessToken(s);
		Map<String, Object> part = Map.of("title", "Task 2", "taskType", "WRITING_TASK2", "prompt", "<p>Đề</p>",
				"criteria", List.of(Map.of("code", "TR", "name", "TR", "checklist", List.of())));
		String body = mvc.perform(postJson("/api/teacher/assignments", Map.of("title", "Bài nháp", "dueAt",
				clock.instant().plus(Duration.ofDays(3)).toString(), "parts", List.of(part)))
				.header(HttpHeaders.AUTHORIZATION, bearer(teacher))).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		JsonNode a = json.readTree(body).get("data");
		partId = a.get("parts").get(0).get("id").asLong();
		mvc.perform(postJson("/api/teacher/assignments/" + a.get("id").asLong() + "/publish",
				Map.of("studentIds", List.of(s.getId()))).header(HttpHeaders.AUTHORIZATION, bearer(teacher)))
				.andExpect(status().isOk());
	}

	private ResultActions save(String content, Integer draftVersion) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("content", content);
		body.put("draftVersion", draftVersion);
		return mvc.perform(put("/api/student/parts/" + partId + "/draft").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(body)).header(HttpHeaders.AUTHORIZATION, bearer(student)));
	}

	@Test
	@DisplayName("SD-06: mỗi lần lưu tăng draftVersion; gửi số cũ → 409 DRAFT_CONFLICT kèm bản trên server, không ghi đè")
	void staleVersionConflicts() throws Exception {
		mvc.perform(get("/api/student/parts/" + partId + "/draft").header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(jsonPath("$.data.draftVersion").value(0));
		save("Tab A lần 1", 0).andExpect(status().isOk()).andExpect(jsonPath("$.data.draftVersion").value(1));
		save("Tab A lần 2", 1).andExpect(status().isOk()).andExpect(jsonPath("$.data.draftVersion").value(2));

		// Tab B vẫn cầm số 1.
		save("Tab B ghi đè?", 1)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DRAFT_CONFLICT"))
				.andExpect(jsonPath("$.data.serverContent").value("Tab A lần 2"))
				.andExpect(jsonPath("$.data.draftVersion").value(2))
				.andExpect(jsonPath("$.data.savedAt").isNotEmpty());
		mvc.perform(get("/api/student/parts/" + partId + "/draft").header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(jsonPath("$.data.content").value("Tab A lần 2"))
				.andExpect(jsonPath("$.data.draftVersion").value(2));

		// Người dùng chọn giữ bản của tab B → gửi lại với số mới nhất.
		save("Tab B ghi đè?", 2).andExpect(status().isOk()).andExpect(jsonPath("$.data.draftVersion").value(3));
	}

	@Test
	@DisplayName("Lần lưu đầu với số khác 0 → 409, bản trên server rỗng; bỏ trống draftVersion → không kiểm tra (như Mức 1); số âm → 400")
	void edgeCases() throws Exception {
		save("x", 5).andExpect(status().isConflict())
				.andExpect(jsonPath("$.data.serverContent").value(""))
				.andExpect(jsonPath("$.data.draftVersion").value(0));
		save("không kèm số", null).andExpect(status().isOk()).andExpect(jsonPath("$.data.draftVersion").value(1));
		save("không kèm số lần 2", null).andExpect(status().isOk()).andExpect(jsonPath("$.data.draftVersion").value(2));
		save("âm", -1).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("2 tab lưu ĐỒNG THỜI cùng draftVersion → đúng 1 lần thành công, lần kia 409 (khoá dòng FOR UPDATE)")
	void concurrentSaves() throws Exception {
		save("bắt đầu", 0).andExpect(status().isOk());
		CountDownLatch start = new CountDownLatch(1);
		Callable<Integer> task = () -> {
			start.await();
			return save("song song", 1).andReturn().getResponse().getStatus();
		};
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> a = pool.submit(task);
			Future<Integer> b = pool.submit(task);
			start.countDown();
			assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(200, 409);
		} finally {
			pool.shutdown();
		}
		mvc.perform(get("/api/student/parts/" + partId + "/draft").header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(jsonPath("$.data.draftVersion").value(2));
	}

	@Test
	@DisplayName("SD-11: GET /api/time không cần đăng nhập, trả giờ server")
	void serverTime() throws Exception {
		mvc.perform(get("/api/time")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.serverTime").value(clock.instant().toString()));
	}
}
