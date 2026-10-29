package com.ltmtu.ielts.paper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.ltmtu.ielts.common.ApiException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Quy tắc thuần cho tài liệu tham khảo dạng CSL-JSON (docs/12 mục 5.3, BR-30): chuẩn hoá DOI, chỉ giữ các trường CSL cần
 * cho trích dẫn (bản ghi của Crossref có cả danh mục tham khảo, cơ quan của từng tác giả… — rất nặng), và liệt kê các
 * trường bắt buộc còn thiếu để client cảnh báo. Định dạng IEEE / APA làm ở client.
 */
public final class CitationRules {

	/** Trường còn thiếu (BR-30): tác giả, năm, nguồn. Tiêu đề bắt buộc ngay khi lưu nên không có ở đây. */
	public static final String MISSING_AUTHOR = "AUTHOR";
	public static final String MISSING_YEAR = "YEAR";
	public static final String MISSING_SOURCE = "SOURCE";

	static final int MAX_NAMES = 100;
	static final int MAX_TEXT = 2000;

	/** Trường giá trị đơn (chữ / số / mảng chữ) được giữ. */
	private static final Set<String> SCALAR_FIELDS = Set.of("title", "title-short", "container-title",
			"container-title-short", "collection-title", "volume", "issue", "page", "number", "edition", "publisher",
			"publisher-place", "event", "event-place", "genre", "URL", "ISBN", "ISSN", "language", "note", "medium");
	private static final Set<String> NAME_FIELDS = Set.of("author", "editor");
	private static final Set<String> NAME_PARTS = Set.of("given", "family", "literal", "suffix", "non-dropping-particle",
			"dropping-particle");
	private static final Set<String> DATE_FIELDS = Set.of("issued", "accessed");
	private static final Set<String> DATE_PARTS = Set.of("date-parts", "literal", "raw", "season", "circa");

	private static final Pattern DOI_PREFIX = Pattern.compile("(?i)^(https?://(dx\\.)?doi\\.org/|doi:\\s*)");
	private static final Pattern DOI = Pattern.compile("^10\\.\\d{4,9}/\\S+$");
	private static final int MAX_DOI = 200;

	private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

	private CitationRules() {
	}

	/** CSL đã lọc + DOI đã chuẩn hoá ({@code null} nếu không có). */
	public record Sanitized(ObjectNode csl, String doi) {
	}

	/**
	 * Chuẩn hoá DOI người dùng nhập: bỏ {@code https://doi.org/}, {@code doi:}, khoảng trắng; chữ thường (DOI không phân biệt
	 * hoa thường). Sai dạng → 400.
	 */
	public static String normalizeDoi(String raw) {
		String s = raw == null ? "" : DOI_PREFIX.matcher(raw.trim()).replaceFirst("").toLowerCase(Locale.ROOT);
		if (s.length() > MAX_DOI || !DOI.matcher(s).matches()) {
			throw ApiException.badRequest("VALIDATION_ERROR", "DOI không hợp lệ, ví dụ đúng: 10.1145/3368089.3409731");
		}
		return s;
	}

	/**
	 * Chỉ giữ các trường CSL được phép, cắt chuỗi quá dài và danh sách tác giả quá dài; bắt buộc có tiêu đề; kiểu mặc định
	 * {@code document}; DOI trong bản ghi được chuẩn hoá. Sau khi lọc vẫn quá {@code maxBytes} → 400.
	 */
	public static Sanitized sanitize(JsonNode input, int maxBytes) {
		if (input == null || !input.isObject()) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Tài liệu tham khảo phải là một bản ghi CSL-JSON");
		}
		ObjectNode out = NODES.objectNode();
		JsonNode type = input.get("type");
		out.put("type", type != null && type.isString() && !type.asString().isBlank()
				? truncate(type.asString().trim(), 50) : "document");
		for (var e : input.properties()) {
			String key = e.getKey();
			JsonNode value = e.getValue();
			JsonNode kept = SCALAR_FIELDS.contains(key) ? scalar(value)
					: NAME_FIELDS.contains(key) ? names(value) : DATE_FIELDS.contains(key) ? date(value) : null;
			if (kept != null) {
				out.set(key, kept);
			}
		}
		if (!hasText(out.get("title"))) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Tài liệu tham khảo cần có tiêu đề");
		}
		String doi = null;
		JsonNode rawDoi = input.get("DOI");
		if (rawDoi != null && rawDoi.isString() && !rawDoi.asString().isBlank()) {
			doi = normalizeDoi(rawDoi.asString());
			out.put("DOI", doi);
		}
		if (out.toString().getBytes(StandardCharsets.UTF_8).length > maxBytes) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Tài liệu tham khảo quá lớn (tối đa " + maxBytes / 1024 + " KB)");
		}
		return new Sanitized(out, doi);
	}

	/** Các trường bắt buộc còn thiếu theo BR-30 — chỉ để cảnh báo, không chặn lưu / nộp. */
	public static List<String> missingFields(JsonNode csl) {
		List<String> missing = new ArrayList<>();
		if (!hasNames(csl.get("author")) && !hasNames(csl.get("editor"))) {
			missing.add(MISSING_AUTHOR);
		}
		if (!hasYear(csl.get("issued"))) {
			missing.add(MISSING_YEAR);
		}
		if (!hasText(csl.get("container-title")) && !hasText(csl.get("publisher")) && !hasText(csl.get("URL"))
				&& !hasText(csl.get("event"))) {
			missing.add(MISSING_SOURCE);
		}
		return missing;
	}

	// ─── Lọc từng loại trường ───

	/** Chữ (cắt bớt nếu quá dài), số, hoặc mảng chữ / số (vd ISSN của Crossref là mảng); kiểu khác bỏ. */
	private static JsonNode scalar(JsonNode v) {
		if (v.isString()) {
			String s = v.asString().trim();
			return s.isEmpty() ? null : NODES.stringNode(truncate(s, MAX_TEXT));
		}
		if (v.isNumber()) {
			return v;
		}
		if (v.isArray()) {
			ArrayNode arr = NODES.arrayNode();
			for (JsonNode item : v) {
				if ((item.isString() && !item.asString().isBlank()) || item.isNumber()) {
					arr.add(item.isString() ? NODES.stringNode(truncate(item.asString().trim(), MAX_TEXT)) : item);
				}
			}
			return arr.isEmpty() ? null : arr;
		}
		return null;
	}

	/** Danh sách người: mỗi người chỉ giữ các phần tên, tối đa {@link #MAX_NAMES} người. */
	private static JsonNode names(JsonNode v) {
		if (!v.isArray()) {
			return null;
		}
		ArrayNode arr = NODES.arrayNode();
		for (JsonNode person : v) {
			if (arr.size() >= MAX_NAMES) {
				break;
			}
			if (!person.isObject()) {
				continue;
			}
			ObjectNode p = NODES.objectNode();
			for (String part : NAME_PARTS) {
				JsonNode x = person.get(part);
				if (x != null && x.isString() && !x.asString().isBlank()) {
					p.put(part, truncate(x.asString().trim(), 200));
				}
			}
			if (!p.isEmpty()) {
				arr.add(p);
			}
		}
		return arr.isEmpty() ? null : arr;
	}

	/** Ngày CSL: {@code {"date-parts": [[2020, 5, 1]]}} hoặc {@code literal} / {@code raw}. */
	private static JsonNode date(JsonNode v) {
		if (!v.isObject()) {
			return null;
		}
		ObjectNode d = NODES.objectNode();
		for (String part : DATE_PARTS) {
			JsonNode x = v.get(part);
			if (x == null) {
				continue;
			}
			if (part.equals("date-parts") && x.isArray()) {
				d.set(part, x);
			} else if (x.isString() || x.isNumber() || x.isBoolean()) {
				d.set(part, x.isString() ? NODES.stringNode(truncate(x.asString(), 100)) : x);
			}
		}
		return d.isEmpty() ? null : d;
	}

	// ─── Kiểm tra ───

	private static boolean hasText(JsonNode v) {
		if (v == null) {
			return false;
		}
		if (v.isArray()) {
			return !v.isEmpty() && hasText(v.get(0));
		}
		return (v.isString() && !v.asString().isBlank()) || v.isNumber();
	}

	private static boolean hasNames(JsonNode v) {
		return v != null && v.isArray() && !v.isEmpty();
	}

	private static boolean hasYear(JsonNode issued) {
		if (issued == null || !issued.isObject()) {
			return false;
		}
		JsonNode first = issued.path("date-parts").path(0).path(0);
		if (first.isNumber() || (first.isString() && !first.asString().isBlank())) {
			return true;
		}
		return hasText(issued.get("literal")) || hasText(issued.get("raw"));
	}

	private static String truncate(String s, int max) {
		return s.length() <= max ? s : s.substring(0, max);
	}
}
