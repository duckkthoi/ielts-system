package com.ltmtu.ielts.paper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.paper.PaperContentRules.RequiredSection;
import com.ltmtu.ielts.paper.PaperContentRules.SectionStat;
import com.ltmtu.ielts.submission.PaperContentSource;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

/**
 * Lấy bản nộp của nhóm từ tài liệu Yjs (B25a, docs/12 mục 5.2): server không đọc Yjs nên dùng snapshot mới nhất mà
 * client gửi kèm JSON + chữ thuần. Khoá dòng tài liệu (cùng khoá với nhận update) để không có update nào lọt vào giữa lúc
 * chụp và lúc bài chuyển sang "đã nộp" — update đến sau phải chờ, rồi bị từ chối vì tài liệu đã chỉ đọc.
 */
@Component
@RequiredArgsConstructor
public class PaperSubmissionContent implements PaperContentSource {

	private final PaperDocumentRepository documentRepository;
	private final PaperReferenceRepository referenceRepository;
	private final ObjectMapper json;

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public PaperContent capture(Long partId, Long groupId) {
		PaperDocument d = documentRepository.findByPartIdAndGroupId(partId, groupId)
				.flatMap(found -> documentRepository.findForUpdate(found.getId()))
				.orElseThrow(PaperSubmissionContent::empty);
		if (d.getLastSeq() == 0) {
			throw empty();
		}
		if (d.getSnapshot() == null || d.getSnapshotSeq() < d.getLastSeq() || d.getContentJson() == null
				|| d.getContentText() == null) {
			throw ApiException.conflict("SNAPSHOT_STALE",
					"Tài liệu có thay đổi chưa được lưu bản tổng hợp. Hãy đợi lưu xong rồi nộp lại");
		}
		JsonNode doc = json.readTree(d.getContentJson());
		Map<String, SectionStat> stats = PaperContentRules.sections(doc);
		if (stats.values().stream().noneMatch(SectionStat::hasContent)) {
			throw empty();
		}
		List<RequiredSection> required = d.getPart().getSections().stream().filter(s -> s.isRequired())
				.map(s -> new RequiredSection(s.getSectionKey(), s.getTitle())).toList();
		List<String> missing = PaperContentRules.emptyRequired(stats, required);
		if (!missing.isEmpty()) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "REQUIRED_SECTIONS_EMPTY",
					"Mục bắt buộc còn trống: " + String.join(", ", missing));
		}
		int words = stats.values().stream().mapToInt(SectionStat::words).sum();
		return new PaperContent(d.getContentText(), words, d.getContentJson(),
				citedReferences(groupId, PaperContentRules.citedReferenceIds(doc)));
	}

	/** Tài liệu được trích theo thứ tự xuất hiện, dạng {@code [{id, csl}]}; trích tới mục đã xoá khỏi thư viện thì bỏ. */
	private String citedReferences(Long groupId, Set<Long> cited) {
		ArrayNode out = json.createArrayNode();
		if (cited.isEmpty()) {
			return out.toString();
		}
		Map<Long, PaperReference> byId = new HashMap<>();
		referenceRepository.findAllById(cited).stream().filter(r -> r.getGroup().getId().equals(groupId))
				.forEach(r -> byId.put(r.getId(), r));
		for (Long id : cited) {
			PaperReference r = byId.get(id);
			if (r != null) {
				out.addObject().put("id", id).set("csl", json.readTree(r.getCsl()));
			}
		}
		return out.toString();
	}

	private static ApiException empty() {
		return ApiException.badRequest("VALIDATION_ERROR", "Tài liệu của nhóm đang trống, chưa có gì để nộp");
	}
}
