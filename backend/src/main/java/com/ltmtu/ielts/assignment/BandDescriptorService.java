package com.ltmtu.ielts.assignment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.ltmtu.ielts.assignment.AssignmentEnums.TaskType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Bảng mô tả band thu gọn bằng tiếng Việt (diễn giải từ IELTS Writing Band Descriptors, May 2023) — dữ liệu tĩnh
 * trong {@code resources/ielts/band-descriptors-vi.json}. Không chép nguyên văn bản gốc (docs/08 mục 5.4).
 */
@Service
public class BandDescriptorService {

	private final JsonNode data;

	public BandDescriptorService(ObjectMapper objectMapper) {
		try (InputStream in = new ClassPathResource("ielts/band-descriptors-vi.json").getInputStream()) {
			this.data = objectMapper.readTree(in);
		} catch (IOException e) {
			throw new UncheckedIOException("Không đọc được bảng mô tả band", e);
		}
	}

	/** Bảng mô tả của các tiêu chí áp dụng cho loại phần; {@code taskType} NULL = tất cả. */
	public Map<String, Object> forTaskType(TaskType taskType) {
		Map<String, Object> criteria = new LinkedHashMap<>();
		for (var entry : data.get("criteria").properties()) {
			boolean applies = taskType == null;
			for (JsonNode t : entry.getValue().get("taskTypes")) {
				applies |= taskType != null && t.asString().equals(taskType.name());
			}
			if (applies) {
				criteria.put(entry.getKey(), entry.getValue());
			}
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("source", data.get("source"));
		result.put("note", data.get("note").asString());
		result.put("criteria", criteria);
		return result;
	}
}
