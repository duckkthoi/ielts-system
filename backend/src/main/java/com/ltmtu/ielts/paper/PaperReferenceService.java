package com.ltmtu.ielts.paper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.assignment.AssignmentEnums.AssignmentStatus;
import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.config.AppProperties;
import com.ltmtu.ielts.paper.CitationRules.Sanitized;
import com.ltmtu.ielts.paper.dto.PaperReferenceDtos.BatchResult;
import com.ltmtu.ielts.paper.dto.PaperReferenceDtos.DoiLookup;
import com.ltmtu.ielts.paper.dto.PaperReferenceDtos.ReferenceDto;
import com.ltmtu.ielts.security.AuthPrincipal;
import com.ltmtu.ielts.user.Role;
import com.ltmtu.ielts.user.User;
import com.ltmtu.ielts.user.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Thư viện trích dẫn của nhóm (UC-36, docs/12 mục 5.3, BR-30). Thành viên <b>hiện tại</b> của nhóm thêm / sửa / xoá;
 * giáo viên chủ bài chỉ xem; người khác → 404. Bài đã đóng → không sửa được nữa. Mọi thao tác ghi khoá dòng nhóm trước
 * (cùng khoá với vào nhóm) để kiểm tra trùng DOI / giới hạn số mục không bị hai người đua nhau làm sai.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperReferenceService {

	private final PaperReferenceRepository referenceRepository;
	private final PaperGroupRepository groupRepository;
	private final PaperGroupMemberRepository memberRepository;
	private final UserQueryService userQueryService;
	private final DoiResolver doiResolver;
	private final DoiThrottle doiThrottle;
	private final AppProperties props;
	private final ObjectMapper json;

	/** Thư viện của nhóm, theo thứ tự thêm. */
	@Transactional(readOnly = true)
	public List<ReferenceDto> list(AuthPrincipal user, Long groupId) {
		PaperGroup g = groupRepository.findById(groupId).orElseThrow(PaperReferenceService::groupNotFound);
		if (!canRead(user, g)) {
			throw groupNotFound();
		}
		return referenceRepository.findByGroup(groupId).stream().map(this::toDto).toList();
	}

	/** Thêm một mục. DOI đã có trong thư viện nhóm → 409 DUPLICATE. */
	@Transactional
	public ReferenceDto add(Long studentId, Long groupId, JsonNode csl) {
		PaperGroup g = lockWritable(studentId, groupId);
		Sanitized s = CitationRules.sanitize(csl, props.paper().maxCslBytes());
		requireRoom(groupId, 1);
		if (s.doi() != null && referenceRepository.existsByGroupIdAndDoi(groupId, s.doi())) {
			throw duplicate(s.doi());
		}
		PaperReference r = saveNew(newReference(g, studentId, s));
		log.info("Sinh viên {} thêm tài liệu tham khảo {} vào nhóm {}", studentId, r.getId(), groupId);
		return toDto(r);
	}

	/**
	 * Nhập nhiều mục (vd từ BibTeX). Mục có DOI đã có trong thư viện hoặc lặp lại trong lần nhập bị bỏ qua (đếm vào
	 * {@code skipped}); một mục sai → 400 cả lần nhập, message ghi số thứ tự mục.
	 */
	@Transactional
	public BatchResult addBatch(Long studentId, Long groupId, List<JsonNode> items) {
		PaperGroup g = lockWritable(studentId, groupId);
		List<Sanitized> clean = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			try {
				clean.add(CitationRules.sanitize(items.get(i), props.paper().maxCslBytes()));
			} catch (ApiException e) {
				throw ApiException.badRequest("VALIDATION_ERROR", "Mục thứ " + (i + 1) + ": " + e.getMessage());
			}
		}
		Set<String> dois = new HashSet<>();
		clean.stream().map(Sanitized::doi).filter(d -> d != null).forEach(dois::add);
		Set<String> seen = dois.isEmpty() ? new HashSet<>() : new HashSet<>(referenceRepository.existingDois(groupId, dois));
		List<Sanitized> fresh = clean.stream().filter(s -> s.doi() == null || seen.add(s.doi())).toList();
		requireRoom(groupId, fresh.size());
		List<ReferenceDto> added = fresh.stream().map(s -> toDto(saveNew(newReference(g, studentId, s)))).toList();
		log.info("Sinh viên {} nhập {} tài liệu tham khảo vào nhóm {} (bỏ qua {})", studentId, added.size(), groupId,
				items.size() - added.size());
		return new BatchResult(added, items.size() - added.size());
	}

	/** Thay bản ghi CSL của một mục (PUT = thay toàn bộ). Đổi sang DOI của mục khác trong nhóm → 409 DUPLICATE. */
	@Transactional
	public ReferenceDto update(Long studentId, Long groupId, Long referenceId, JsonNode csl) {
		lockWritable(studentId, groupId);
		PaperReference r = referenceRepository.findInGroup(referenceId, groupId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy tài liệu tham khảo"));
		Sanitized s = CitationRules.sanitize(csl, props.paper().maxCslBytes());
		if (s.doi() != null && referenceRepository.existsByGroupIdAndDoiAndIdNot(groupId, s.doi(), referenceId)) {
			throw duplicate(s.doi());
		}
		r.setCsl(s.csl().toString());
		r.setDoi(s.doi());
		return toDto(referenceRepository.saveAndFlush(r));
	}

	/** Xoá một mục. Trích dẫn trỏ tới mục này trong tài liệu do client cảnh báo trước khi xoá. */
	@Transactional
	public void delete(Long studentId, Long groupId, Long referenceId) {
		lockWritable(studentId, groupId);
		PaperReference r = referenceRepository.findInGroup(referenceId, groupId)
				.orElseThrow(() -> ApiException.notFound("Không tìm thấy tài liệu tham khảo"));
		referenceRepository.delete(r);
		log.info("Sinh viên {} xoá tài liệu tham khảo {} của nhóm {}", studentId, referenceId, groupId);
	}

	/** Tra DOI ra bản ghi CSL đã lọc (chưa lưu). Mỗi người tối đa N lần / phút → 429. */
	public DoiLookup lookupDoi(Long userId, String rawDoi) {
		String doi = CitationRules.normalizeDoi(rawDoi);
		Duration wait = doiThrottle.tryLookup(userId);
		if (wait != null) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
					"Bạn tra DOI quá nhiều lần. Vui lòng thử lại sau " + Math.max(1, wait.toSeconds()) + " giây");
		}
		Sanitized s = CitationRules.sanitize(doiResolver.resolve(doi), props.paper().maxCslBytes());
		return new DoiLookup(doi, s.csl(), CitationRules.missingFields(s.csl()));
	}

	// ─── Quyền ───

	/** Khoá dòng nhóm trước rồi mới kiểm tra: phải là thành viên hiện tại (khác → 404) và bài chưa đóng (→ 409). */
	private PaperGroup lockWritable(Long studentId, Long groupId) {
		PaperGroup g = groupRepository.findForUpdate(groupId).orElseThrow(PaperReferenceService::groupNotFound);
		if (!isMember(studentId, g)) {
			throw groupNotFound();
		}
		if (g.getAssignment().getStatus() == AssignmentStatus.CLOSED) {
			throw ApiException.conflict("INVALID_STATE", "Bài tập đã đóng, không sửa thư viện trích dẫn được nữa");
		}
		return g;
	}

	private boolean canRead(AuthPrincipal user, PaperGroup g) {
		if (user.role() == Role.TEACHER) {
			return g.getAssignment().getTeacher().getId().equals(user.id());
		}
		return user.role() == Role.STUDENT && isMember(user.id(), g);
	}

	private boolean isMember(Long studentId, PaperGroup g) {
		return memberRepository.findByAssignmentIdAndIdStudentId(g.getAssignment().getId(), studentId)
				.map(m -> m.getId().getGroupId().equals(g.getId())).orElse(false);
	}

	private void requireRoom(Long groupId, int adding) {
		int max = props.paper().maxReferences();
		if (referenceRepository.countByGroupId(groupId) + adding > max) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Thư viện trích dẫn của nhóm có tối đa " + max + " tài liệu");
		}
	}

	// ─── Lưu và ánh xạ ───

	private PaperReference newReference(PaperGroup g, Long studentId, Sanitized s) {
		PaperReference r = new PaperReference();
		r.setGroup(g);
		r.setCsl(s.csl().toString());
		r.setDoi(s.doi());
		r.setCreatedBy(userQueryService.requireUser(studentId));
		return r;
	}

	/** UNIQUE (nhóm, DOI) là chốt chặn cuối nếu vẫn lọt trùng. */
	private PaperReference saveNew(PaperReference r) {
		try {
			return referenceRepository.saveAndFlush(r);
		} catch (DataIntegrityViolationException e) {
			throw duplicate(r.getDoi());
		}
	}

	private ReferenceDto toDto(PaperReference r) {
		JsonNode csl = json.readTree(r.getCsl());
		User by = r.getCreatedBy();
		return new ReferenceDto(r.getId(), csl, r.getDoi(), CitationRules.missingFields(csl),
				by == null ? null : by.getId(), by == null ? null : by.getFullName(), r.getCreatedAt(), r.getUpdatedAt());
	}

	private static ApiException duplicate(String doi) {
		return ApiException.conflict("DUPLICATE", "Tài liệu có DOI " + doi + " đã có trong thư viện của nhóm");
	}

	private static ApiException groupNotFound() {
		return ApiException.notFound("Không tìm thấy nhóm");
	}
}
