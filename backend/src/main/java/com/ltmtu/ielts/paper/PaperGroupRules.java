package com.ltmtu.ielts.paper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Chia nhóm và đặt tên nhóm (BR-27). Class thuần, không phụ thuộc Spring. */
public final class PaperGroupRules {

	private PaperGroupRules() {
	}

	/**
	 * Chia {@code students} thành ít nhóm nhất mà mỗi nhóm không quá {@code maxSize} người, các nhóm chênh nhau tối đa
	 * một người (vd 10 người, tối đa 4 → 4 + 3 + 3, không phải 4 + 4 + 2). Trộn ngẫu nhiên theo {@code random}.
	 */
	public static List<List<Long>> split(Collection<Long> students, int maxSize, Random random) {
		List<Long> pool = new ArrayList<>(students);
		Collections.shuffle(pool, random);
		int n = pool.size();
		if (n == 0) {
			return List.of();
		}
		int groups = (n + maxSize - 1) / maxSize;
		int base = n / groups;
		int bigger = n % groups;
		List<List<Long>> result = new ArrayList<>();
		int from = 0;
		for (int g = 0; g < groups; g++) {
			int size = base + (g < bigger ? 1 : 0);
			result.add(List.copyOf(pool.subList(from, from + size)));
			from += size;
		}
		return result;
	}

	/** Tên "Nhóm N" kế tiếp chưa dùng trong bài. */
	public static String nextName(Set<String> used) {
		for (int n = 1;; n++) {
			String name = "Nhóm " + n;
			if (!used.contains(name)) {
				return name;
			}
		}
	}
}
