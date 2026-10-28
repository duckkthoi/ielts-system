package com.ltmtu.ielts.paper;

import static com.ltmtu.ielts.support.ApiStatus.apiStatus;
import static com.ltmtu.ielts.support.ApiStatus.apiStatusOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.util.ArrayList;
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
import org.springframework.http.MediaType;

import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;

import tools.jackson.databind.JsonNode;

/** Nhóm viết paper (UC-34, BR-27): giáo viên chia nhóm, sinh viên tự lập / vào / rời nhóm, hạn chốt, đủ người. */
class PaperGroupIntegrationTest extends PaperTestSupport {

	private String teacher;
	private final List<User> students = new ArrayList<>();

	@BeforeEach
	void setUp() throws Exception {
		teacher = accessToken(newUser(Role.TEACHER));
		students.clear();
		for (int i = 0; i < 5; i++) {
			students.add(newUser(Role.STUDENT));
		}
	}

	@Test
	@DisplayName("BR-27: giáo viên tạo nhóm, chuyển sinh viên sang nhóm khác (tự rời nhóm cũ), xoá nhóm → người đó lại chưa có nhóm")
	void teacherManagesGroups() throws Exception {
		long id = publishedPaper(teacher, Map.of("maxGroupSize", 3), students.toArray(User[]::new));
		JsonNode v = data(call(get(groupsUrl(id)), teacher));
		assertThat(v.get("ungrouped")).hasSize(5);
		assertThat(v.get("maxGroupSize").asInt()).isEqualTo(3);

		v = data(call(postJson(groupsUrl(id), Map.of("name", "Nhóm A", "studentIds", ids(0, 1))), teacher));
		long groupA = v.get("groups").get(0).get("id").asLong();
		v = data(call(postJson(groupsUrl(id), Map.of("name", "Nhóm B", "studentIds", ids(2))), teacher));
		long groupB = v.get("groups").get(1).get("id").asLong();
		assertThat(v.get("ungrouped")).hasSize(2);

		v = data(call(put(groupsUrl(id) + "/" + groupB).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("name", "Nhóm B", "studentIds", ids(1, 2)))), teacher));
		assertThat(members(v, groupA)).containsExactly(students.get(0).getId());
		assertThat(members(v, groupB)).containsExactlyInAnyOrder(students.get(1).getId(), students.get(2).getId());

		call(postJson(groupsUrl(id), Map.of("name", "Nhóm A", "studentIds", List.of())), teacher)
				.andExpect(apiStatus(409)).andExpect(jsonPath("$.errorCode").value("DUPLICATE"));
		call(postJson(groupsUrl(id), Map.of("name", "Quá đông", "studentIds", ids(0, 1, 2, 3))), teacher)
				.andExpect(apiStatus(400));
		call(postJson(groupsUrl(id), Map.of("name", "Người ngoài", "studentIds",
				List.of(newUser(Role.STUDENT).getId()))), teacher).andExpect(apiStatus(400));

		v = data(call(delete(groupsUrl(id) + "/" + groupA), teacher));
		assertThat(v.get("groups")).hasSize(1);
		assertThat(v.get("ungrouped")).hasSize(3);
	}

	@Test
	@DisplayName("BR-27: chia ngẫu nhiên người chưa có nhóm — đều nhau, không quá cỡ; lọc theo lớp; nhóm đã có giữ nguyên")
	void autoSplit() throws Exception {
		List<User> more = new ArrayList<>(students);
		for (int i = 0; i < 5; i++) {
			more.add(newUser(Role.STUDENT));
		}
		long classId = createClass(teacher, more.subList(0, 7).toArray(User[]::new));
		long id = publishedPaper(teacher, Map.of(), more.toArray(User[]::new));
		data(call(postJson(groupsUrl(id), Map.of("name", "Sẵn có", "studentIds", List.of(more.get(0).getId()))), teacher));

		JsonNode v = data(call(postJson(groupsUrl(id) + "/auto", Map.of("classId", classId)), teacher));
		// Lớp có 7 người, 1 người đã có nhóm → chia 6 người, tối đa 4 → 3 + 3.
		assertThat(v.get("groups")).hasSize(3);
		assertThat(v.get("groups").get(1).get("members")).hasSize(3);
		assertThat(v.get("groups").get(2).get("members")).hasSize(3);
		assertThat(v.get("ungrouped")).hasSize(3);

		v = data(call(post(groupsUrl(id) + "/auto"), teacher));
		assertThat(v.get("ungrouped")).isEmpty();
		assertThat(v.get("groups").valueStream().map(g -> g.get("name").asString()).toList())
				.contains("Nhóm 1", "Nhóm 2", "Nhóm 3");
	}

	@Test
	@DisplayName("Nhóm chỉ cho bài PAPER đã phát hành; giáo viên khác → 404; sinh viên không được giao → 404")
	void access() throws Exception {
		long draft = createPaper(teacher, Map.of()).get("id").asLong();
		call(get(groupsUrl(draft)), teacher).andExpect(apiStatus(409));
		long essay = data(call(postJson("/api/teacher/assignments", Map.of("title", "Bài luận")), teacher)).get("id")
				.asLong();
		call(get(groupsUrl(essay)), teacher).andExpect(apiStatus(409));

		long id = publishedPaper(teacher, Map.of(), students.get(0));
		call(get(groupsUrl(id)), accessToken(newUser(Role.TEACHER))).andExpect(apiStatus(404));
		call(get(studentUrl(id)), accessToken(students.get(1))).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("BR-27: sinh viên tự lập nhóm, người khác vào; đủ người → 409 GROUP_FULL; đã có nhóm → 409 ALREADY_IN_GROUP; rời hết → nhóm tự xoá")
	void studentSelfService() throws Exception {
		long id = publishedPaper(teacher, Map.of("maxGroupSize", 2), students.toArray(User[]::new));
		String s0 = accessToken(students.get(0));
		JsonNode v = data(call(postJson(studentUrl(id), Map.of("name", "Nhóm tự lập")), s0));
		long gid = v.get("myGroupId").asLong();
		assertThat(v.get("canChange").asBoolean()).isTrue();
		assertThat(v.get("groups").get(0).get("studentMade").asBoolean()).isTrue();

		call(postJson(studentUrl(id), Map.of("name", "Nhóm thứ hai")), s0).andExpect(apiStatus(409))
				.andExpect(jsonPath("$.errorCode").value("ALREADY_IN_GROUP"));
		String s1 = accessToken(students.get(1));
		assertThat(data(call(post(studentUrl(id) + "/" + gid + "/join"), s1)).get("myGroupId").asLong()).isEqualTo(gid);
		call(post(studentUrl(id) + "/" + gid + "/join"), accessToken(students.get(2))).andExpect(apiStatus(409))
				.andExpect(jsonPath("$.errorCode").value("GROUP_FULL"));

		data(call(delete(studentUrl(id) + "/mine"), s0));
		v = data(call(delete(studentUrl(id) + "/mine"), s1));
		assertThat(v.get("myGroupId").isNull()).isTrue();
		assertThat(v.get("groups")).as("nhóm sinh viên lập, không còn ai → xoá").isEmpty();
		call(delete(studentUrl(id) + "/mine"), s1).andExpect(apiStatus(404));
	}

	@Test
	@DisplayName("BR-27: sau hạn chốt nhóm sinh viên không tự đổi được (409 GROUP_LOCKED), giáo viên vẫn chia được")
	void groupLock() throws Exception {
		long id = publishedPaper(teacher, Map.of("groupLockAt", clock.instant().plus(Duration.ofDays(2)).toString()),
				students.toArray(User[]::new));
		clock.advance(Duration.ofDays(2));
		String s0 = accessToken(students.get(0));
		JsonNode v = data(call(get(studentUrl(id)), s0));
		assertThat(v.get("canChange").asBoolean()).isFalse();
		call(postJson(studentUrl(id), Map.of("name", "Muộn")), s0).andExpect(apiStatus(409))
				.andExpect(jsonPath("$.errorCode").value("GROUP_LOCKED"));
	}

	@Test
	@DisplayName("BR-27: hai sinh viên vào chỗ cuối cùng của nhóm ĐỒNG THỜI → đúng một người được")
	void concurrentJoin() throws Exception {
		long id = publishedPaper(teacher, Map.of("maxGroupSize", 2), students.toArray(User[]::new));
		long gid = data(call(postJson(studentUrl(id), Map.of("name", "Một chỗ trống")), accessToken(students.get(0))))
				.get("myGroupId").asLong();
		String s1 = accessToken(students.get(1));
		String s2 = accessToken(students.get(2));
		CountDownLatch go = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			List<Future<Integer>> results = new ArrayList<>();
			for (String token : List.of(s1, s2)) {
				Callable<Integer> task = () -> {
					go.await();
					return apiStatusOf(call(post(studentUrl(id) + "/" + gid + "/join"), token).andReturn());
				};
				results.add(pool.submit(task));
			}
			go.countDown();
			assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(200, 409);
		} finally {
			pool.shutdown();
		}
		JsonNode v = data(call(get(studentUrl(id)), s1));
		assertThat(v.get("groups").get(0).get("members")).hasSize(2);
	}

	private List<Long> ids(int... indexes) {
		return java.util.Arrays.stream(indexes).mapToObj(i -> students.get(i).getId()).toList();
	}

	private static List<Long> members(JsonNode view, long groupId) {
		return view.get("groups").valueStream().filter(g -> g.get("id").asLong() == groupId).findFirst().orElseThrow()
				.get("members").valueStream().map(m -> m.get("studentId").asLong()).toList();
	}

	private static String groupsUrl(long id) {
		return "/api/teacher/assignments/" + id + "/groups";
	}

	private static String studentUrl(long id) {
		return "/api/student/assignments/" + id + "/groups";
	}
}
