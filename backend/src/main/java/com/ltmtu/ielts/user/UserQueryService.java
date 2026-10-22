package com.ltmtu.ielts.user;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.common.ApiException;
import com.ltmtu.ielts.common.Pageables;

import lombok.RequiredArgsConstructor;

/** Truy vấn người dùng cho các module khác (lớp học, bài tập…) — không cho module khác gọi thẳng repository. */
@Service
@RequiredArgsConstructor
public class UserQueryService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public User requireUser(Long id) {
		return userRepository.findById(id).orElseThrow(() -> ApiException.unauthorized("Tài khoản không tồn tại"));
	}

	/** Sinh viên theo danh sách mã SV (chỉ vai trò STUDENT), không phân biệt hoa thường. */
	@Transactional(readOnly = true)
	public List<User> findStudentsByCodes(Collection<String> studentCodes) {
		List<String> upper = studentCodes.stream().map(c -> c.trim().toUpperCase()).distinct().toList();
		return upper.isEmpty() ? List.of() : userRepository.findByRoleAndStudentCodeInIgnoreCase(Role.STUDENT, upper);
	}

	/** Sinh viên theo id (dùng khi giao bài cá nhân). Có id không phải sinh viên / không tồn tại → 400. */
	@Transactional(readOnly = true)
	public List<User> requireStudents(Collection<Long> ids) {
		List<User> found = userRepository.findAllById(ids).stream().filter(u -> u.getRole() == Role.STUDENT).toList();
		if (found.size() != ids.size()) {
			throw ApiException.badRequest("VALIDATION_ERROR", "Có tài khoản không tồn tại hoặc không phải sinh viên");
		}
		return found;
	}

	/** Người dùng theo id (vd người nhận thông báo). Id không tồn tại thì bỏ qua. */
	@Transactional(readOnly = true)
	public List<User> findAllByIds(Collection<Long> ids) {
		return ids.isEmpty() ? List.of() : userRepository.findAllById(ids);
	}

	/** Tìm sinh viên theo mã hoặc tên, tối đa {@code limit} kết quả. */
	@Transactional(readOnly = true)
	public List<User> searchStudents(String keyword, int limit) {
		return userRepository.search(Pageables.likePattern(keyword), Role.STUDENT, true,
				PageRequest.of(0, limit, Sort.by("studentCode"))).getContent();
	}
}
