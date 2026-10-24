package com.ltmtu.ielts.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.common.ApiException;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Ghi và tra cứu nhật ký hệ thống (UC-07, FR-08). Chỉ ghi id, IP và số liệu tóm tắt — KHÔNG ghi mật khẩu, token, nội
 * dung file hay bài làm (CLAUDE.md mục 2).
 */
@Service
@RequiredArgsConstructor
public class AuditService {

	private final AuditLogRepository repository;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	/**
	 * Ghi trong <b>cùng transaction</b> với thao tác nghiệp vụ: thao tác rollback thì không có dòng nhật ký "ma". IP lấy
	 * từ request HTTP hiện tại ({@link RequestIp}).
	 */
	@Transactional
	public void record(Long actorId, AuditAction action, String targetType, Long targetId, Map<String, ?> detail) {
		save(actorId, action, targetType, targetId, RequestIp.current(), detail);
	}

	/**
	 * Ghi trong transaction <b>riêng</b>, giữ lại kể cả khi thao tác của người gọi thất bại (vd đăng nhập sai: request
	 * ném lỗi 401 nhưng vẫn phải có nhật ký). {@code ip} null thì lấy từ request hiện tại.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordAlone(Long actorId, AuditAction action, String targetType, Long targetId, String ip,
			Map<String, ?> detail) {
		save(actorId, action, targetType, targetId, ip != null ? ip : RequestIp.current(), detail);
	}

	private void save(Long actorId, AuditAction action, String targetType, Long targetId, String ip,
			Map<String, ?> detail) {
		String json = detail == null || detail.isEmpty() ? null : objectMapper.writeValueAsString(detail);
		repository.save(AuditLog.of(actorId, action, targetType, targetId, ip, json, clock.instant()));
	}

	/** UC-07: tra cứu, mới nhất trước (bỏ sort của client). {@code ip} lọc theo tiền tố, vd "192.168.". */
	@Transactional(readOnly = true)
	public Page<AuditLogDto> search(Long actorId, AuditAction action, String ip, Instant from, Instant to,
			Pageable pageable) {
		if (from != null && to != null && !from.isBefore(to)) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Thời điểm bắt đầu phải trước thời điểm kết thúc");
		}
		String ipPrefix = ip == null || ip.isBlank() ? null
				: ip.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
		return repository.search(actorId, action == null ? null : action.name(), ipPrefix, from, to,
				PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
				.map(this::toDto);
	}

	private AuditLogDto toDto(AuditLogRepository.Row r) {
		JsonNode detail = r.getDetail() == null ? null : objectMapper.readTree(r.getDetail());
		return new AuditLogDto(r.getId(), r.getActorId(), r.getActorUsername(), r.getActorName(),
				AuditAction.valueOf(r.getAction()), r.getTargetType(), r.getTargetId(), r.getIp(), detail,
				r.getCreatedAt());
	}

	public record AuditLogDto(Long id, Long actorId, String actorUsername, String actorName, AuditAction action,
			String targetType, Long targetId, String ip, JsonNode detail, Instant createdAt) {
	}
}
