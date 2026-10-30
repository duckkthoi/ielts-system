package com.ltmtu.ielts.assignment;

import java.text.Normalizer;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentKind;
import com.ltmtu.ielts.assignment.AssignmentEnums.CitationStyle;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.AssignmentRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.PartRequest;
import com.ltmtu.ielts.assignment.dto.AssignmentDtos.SectionRequest;
import com.ltmtu.ielts.common.ApiException;

/** Quy tắc của bài PAPER (docs/12, BR-27, BR-29) mà annotation validate không diễn đạt được. Class thuần. */
public final class PaperRules {

	/** Cỡ nhóm khi giáo viên không nhập. */
	static final int DEFAULT_GROUP_SIZE = 4;
	static final CitationStyle DEFAULT_CITATION = CitationStyle.IEEE;

	private PaperRules() {
	}

	static AssignmentKind kindOf(AssignmentRequest req) {
		return req.kind() == null ? AssignmentKind.ESSAY : req.kind();
	}

	static void validate(AssignmentRequest req) {
		AssignmentKind kind = kindOf(req);
		List<PartRequest> parts = req.parts() == null ? List.of() : req.parts();
		if (kind == AssignmentKind.ESSAY) {
			for (int p = 0; p < parts.size(); p++) {
				if (parts.get(p).sections() != null && !parts.get(p).sections().isEmpty()) {
					throw invalid("Phần " + (p + 1) + ": chỉ bài paper mới có khung mục");
				}
			}
			return;
		}
		requireLockBeforeDue(req.groupLockAt(), req.dueAt());
		for (int p = 0; p < parts.size(); p++) {
			PartRequest part = parts.get(p);
			String where = "Phần " + (p + 1);
			if (Boolean.TRUE.equals(part.examMode())) {
				throw invalid(where + ": bài paper không dùng chế độ thi");
			}
			validateSections(part.sections() == null ? List.of() : part.sections(), where);
		}
	}

	static void requireLockBeforeDue(Instant lockAt, Instant dueAt) {
		if (lockAt != null && dueAt != null && lockAt.isAfter(dueAt)) {
			throw invalid("Hạn chốt nhóm phải trước hạn nộp");
		}
	}

	private static void validateSections(List<SectionRequest> sections, String where) {
		Set<String> keys = new HashSet<>();
		for (int i = 0; i < sections.size(); i++) {
			SectionRequest s = sections.get(i);
			String at = where + " – mục " + (i + 1);
			if (s.minWords() != null && s.maxWords() != null && s.minWords() > s.maxWords()) {
				throw invalid(at + ": số từ tối thiểu lớn hơn số từ tối đa");
			}
			if (s.sectionKey() != null && !s.sectionKey().isBlank() && !keys.add(slug(s.sectionKey()))) {
				throw invalid(at + ": mã mục \"" + s.sectionKey() + "\" bị lặp");
			}
		}
	}

	/**
	 * Mã mục ổn định cho trình soạn: chữ thường không dấu, nối bằng gạch ngang, tối đa 40 ký tự. Trùng với mã đã dùng
	 * trong phần thì thêm hậu tố {@code -2}, {@code -3}…
	 */
	static String sectionKey(String requested, String title, Set<String> used) {
		String base = slug(requested != null && !requested.isBlank() ? requested : title);
		if (base.isEmpty()) {
			base = "muc";
		}
		String key = base;
		for (int n = 2; !used.add(key); n++) {
			String suffix = "-" + n;
			key = base.substring(0, Math.min(base.length(), 40 - suffix.length())) + suffix;
		}
		return key;
	}

	static String slug(String text) {
		String ascii = Normalizer.normalize(text.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "");
		String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
		return slug.length() <= 40 ? slug : slug.substring(0, 40).replaceAll("-+$", "");
	}

	private static ApiException invalid(String message) {
		return ApiException.badRequest("VALIDATION_ERROR", message);
	}
}
