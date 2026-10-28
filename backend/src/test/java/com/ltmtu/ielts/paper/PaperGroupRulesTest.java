package com.ltmtu.ielts.paper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PaperGroupRulesTest {

	@ParameterizedTest(name = "{0} người, tối đa {1} → {2}")
	@DisplayName("BR-27: chia ít nhóm nhất, không nhóm nào quá cỡ, các nhóm chênh nhau tối đa 1 người")
	@CsvSource({ "10, 4, 4 3 3", "8, 4, 4 4", "3, 4, 3", "9, 2, 2 2 2 2 1", "1, 1, 1", "7, 3, 3 2 2" })
	void split(int n, int max, String expectedSizes) {
		List<Long> students = IntStream.rangeClosed(1, n).mapToObj(i -> (long) i).toList();
		List<List<Long>> groups = PaperGroupRules.split(students, max, new Random(42));
		assertThat(groups.stream().map(g -> String.valueOf(g.size())).toList())
				.containsExactly(expectedSizes.split(" "));
		List<Long> all = new ArrayList<>();
		groups.forEach(all::addAll);
		assertThat(all).containsExactlyInAnyOrderElementsOf(students);
	}

	@Test
	@DisplayName("Không ai thì không tạo nhóm; tên nhóm kế tiếp bỏ qua tên đã dùng")
	void emptyAndNames() {
		assertThat(PaperGroupRules.split(List.of(), 4, new Random())).isEmpty();
		assertThat(PaperGroupRules.nextName(Set.of())).isEqualTo("Nhóm 1");
		assertThat(PaperGroupRules.nextName(Set.of("Nhóm 1", "Nhóm 2", "Nhóm 4"))).isEqualTo("Nhóm 3");
	}
}
