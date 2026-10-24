package com.ltmtu.ielts.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ltmtu.ielts.stats.dto.ClassStatsDtos.RankingRow;
import com.ltmtu.ielts.stats.dto.ProgressDtos.StudentRef;

/** Cách xếp hạng của bảng xếp hạng lớp ({@link ClassStatsService#rank}). */
class ClassRankingTest {

	private static RankingRow row(String code, String band) {
		return new RankingRow(null, new StudentRef(1L, code, code), band == null ? null : new BigDecimal(band), 0, 0, 0);
	}

	@Test
	@DisplayName("Bằng band thì cùng hạng, hạng sau nhảy (1, 1, 3); cùng hạng xếp theo mã SV; chưa có band đứng cuối, không có hạng")
	void competitionRanking() {
		List<RankingRow> ranked = ClassStatsService.rank(List.of(row("SV4", null), row("SV3", "6.50"), row("SV2", "7.00"),
				row("SV1", "6.50"), row("SV5", "5.00"), row("SV0", null)));
		assertThat(ranked).extracting(r -> r.student().studentCode())
				.containsExactly("SV2", "SV1", "SV3", "SV5", "SV0", "SV4");
		assertThat(ranked).extracting(RankingRow::rank).containsExactly(1, 2, 2, 4, null, null);
	}

	@Test
	@DisplayName("Lớp rỗng → bảng rỗng")
	void empty() {
		assertThat(ClassStatsService.rank(List.of())).isEmpty();
	}
}
