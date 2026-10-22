package com.ltmtu.ielts.submission;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** BR-20: chạy bộ ca kiểm thử chung với frontend ({@code shared/word-count-cases.json}). */
class WordCounterTest {

	static Stream<Arguments> sharedCases() throws Exception {
		JsonNode root = JsonMapper.builder().build()
				.readTree(Files.readString(Path.of("..", "shared", "word-count-cases.json")));
		return root.get("cases").valueStream()
				.map(c -> Arguments.of(c.get("name").asString(), c.get("text").asString(), c.get("count").asInt()));
	}

	@ParameterizedTest(name = "BR-20 đếm từ: {0}")
	@MethodSource("sharedCases")
	void countsLikeIelts(String name, String text, int expected) {
		assertThat(WordCounter.count(text)).as(name).isEqualTo(expected);
	}
}
