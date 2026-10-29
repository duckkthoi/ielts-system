package com.ltmtu.ielts.paper;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ltmtu.ielts.submission.WordCounter;

import tools.jackson.databind.JsonNode;

/**
 * Quy tắc thuần trên JSON trình soạn của bài PAPER (dạng ProseMirror: {@code {type, attrs, content, text}}) — dùng khi
 * nộp (B25a): mục nào có nội dung (BR-29), số từ phần thân các mục, tài liệu nào được trích dẫn (BR-30). Cùng cách
 * đọc với {@code lib/utils/paperDoc.ts} ở client.
 */
public final class PaperContentRules {

	/** Khối không có chữ nhưng vẫn tính là "có nội dung" của mục (hình, bảng, công thức, khối code). */
	private static final Set<String> CONTENT_ATOMS = Set.of("image", "table", "blockMath", "inlineMath", "codeBlock",
			"citation");

	private PaperContentRules() {
	}

	/** Thống kê một mục: có nội dung không, số từ phần thân (không tính tiêu đề mục, công thức). */
	public record SectionStat(boolean hasContent, int words) {
	}

	/** Thống kê từng mục theo {@code sectionKey}, đúng thứ tự trong tài liệu. */
	public static Map<String, SectionStat> sections(JsonNode doc) {
		Map<String, SectionStat> out = new LinkedHashMap<>();
		for (JsonNode n : children(doc)) {
			if (!"paperSection".equals(type(n))) {
				continue;
			}
			StringBuilder text = new StringBuilder();
			boolean[] atom = { false };
			collect(n, text, atom);
			String key = n.path("attrs").path("sectionKey").asString("");
			out.put(key, new SectionStat(atom[0] || !text.toString().isBlank(), WordCounter.count(text.toString())));
		}
		return out;
	}

	/** Tiêu đề các mục bắt buộc còn trống (BR-29), theo thứ tự của khung. */
	public static List<String> emptyRequired(Map<String, SectionStat> stats, List<RequiredSection> required) {
		return required.stream()
				.filter(r -> !stats.getOrDefault(r.sectionKey(), new SectionStat(false, 0)).hasContent())
				.map(RequiredSection::title).toList();
	}

	public record RequiredSection(String sectionKey, String title) {
	}

	/** Id tài liệu được trích trong bài theo thứ tự xuất hiện lần đầu ({@code citation.attrs.refIds = "3,7"}). */
	public static Set<Long> citedReferenceIds(JsonNode doc) {
		Set<Long> ids = new LinkedHashSet<>();
		walkCitations(doc, ids);
		return ids;
	}

	// ─── Duyệt cây ───

	private static void collect(JsonNode node, StringBuilder text, boolean[] atom) {
		String type = type(node);
		if ("text".equals(type)) {
			text.append(node.path("text").asString(""));
			return;
		}
		if (CONTENT_ATOMS.contains(type)) {
			atom[0] = true;
			if (type.endsWith("Math")) {
				return; // công thức không tính là chữ
			}
		}
		for (JsonNode c : children(node)) {
			collect(c, text, atom);
		}
		// Mỗi khối một dòng để từ ở hai khối liền nhau không dính vào nhau.
		text.append('\n');
	}

	private static void walkCitations(JsonNode node, Set<Long> ids) {
		if ("citation".equals(type(node))) {
			for (String part : node.path("attrs").path("refIds").asString("").split(",")) {
				try {
					long id = Long.parseLong(part.trim());
					if (id > 0) {
						ids.add(id);
					}
				} catch (NumberFormatException e) {
					// phần không phải số: bỏ qua như client (parseRefIds)
				}
			}
			return;
		}
		for (JsonNode c : children(node)) {
			walkCitations(c, ids);
		}
	}

	private static String type(JsonNode n) {
		return n.path("type").asString("");
	}

	private static Iterable<JsonNode> children(JsonNode n) {
		JsonNode c = n.path("content");
		return c.isArray() ? c : List.of();
	}
}
