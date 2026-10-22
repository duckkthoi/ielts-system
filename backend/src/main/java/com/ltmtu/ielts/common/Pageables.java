package com.ltmtu.ielts.common;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Kiểm soát tham số phân trang / sắp xếp từ client. */
public final class Pageables {

	private Pageables() {
	}

	/**
	 * Chỉ cho sắp xếp theo các trường trong {@code allowed} (tránh lỗi 500 và lộ tên trường nội bộ).
	 * Không có {@code sort} → dùng {@code defaultSort}.
	 */
	public static Pageable sanitize(Pageable pageable, Set<String> allowed, Sort defaultSort) {
		return sanitize(pageable, allowed.stream().collect(Collectors.toMap(Function.identity(), Function.identity())),
				defaultSort);
	}

	/**
	 * Như trên, nhưng ánh xạ tên trường client gửi sang đường dẫn thuộc tính trong query
	 * (vd {@code fullName} → {@code student.fullName}).
	 */
	public static Pageable sanitize(Pageable pageable, Map<String, String> allowedToPath, Sort defaultSort) {
		Sort sort = Sort.unsorted();
		for (Sort.Order order : pageable.getSort()) {
			String path = allowedToPath.get(order.getProperty());
			if (path == null) {
				throw ApiException.badRequest("VALIDATION_ERROR",
						"Không sắp xếp được theo trường '" + order.getProperty() + "'");
			}
			sort = sort.and(Sort.by(order.getDirection(), path));
		}
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.isSorted() ? sort : defaultSort);
	}

	/** Ký tự escape của mẫu LIKE. Query phải khai báo {@code LIKE :keyword ESCAPE '!'}. */
	public static final char LIKE_ESCAPE = '!';

	/**
	 * Chuẩn hoá từ khoá tìm kiếm thành mẫu LIKE {@code %…%} chữ thường; rỗng → NULL (không lọc).
	 * Ký tự {@code % _ !} trong từ khoá được escape để khớp đúng nghĩa đen (vd tên đăng nhập có dấu gạch dưới).
	 */
	public static String likePattern(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		String escaped = keyword.trim().toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_");
		return "%" + escaped + "%";
	}
}
