package com.ltmtu.ielts.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Tạo tài khoản cho dữ liệu mẫu (profile {@code demo}, kiểm thử tải): mật khẩu biết trước, <b>không</b> bắt đổi mật
 * khẩu lần đầu — khác luồng import thật (BR-02). Chỉ module {@code demo} gọi.
 */
@Service
@RequiredArgsConstructor
public class DemoAccountService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	/** {@code studentCode} chỉ có ở sinh viên (và bằng {@code username}, BR-01). */
	public record NewAccount(Role role, String username, String fullName, String email, String studentCode) {
	}

	@Transactional(readOnly = true)
	public boolean exists(String username) {
		return userRepository.existsByUsernameIgnoreCase(username);
	}

	/**
	 * Tạo cả nhóm tài khoản cùng một mật khẩu → id theo đúng thứ tự. BCrypt <b>một lần</b> cho cả nhóm: mọi tài khoản
	 * mẫu cùng mật khẩu, mã hoá 500 lần (lớp kiểm thử tải) sẽ làm khởi động chậm thêm gần một phút.
	 */
	@Transactional
	public List<Long> createAll(List<NewAccount> accounts, String rawPassword) {
		String hash = passwordEncoder.encode(rawPassword);
		List<User> users = new ArrayList<>(accounts.size());
		for (NewAccount a : accounts) {
			User u = new User();
			u.setRole(a.role());
			u.setUsername(a.username());
			u.setStudentCode(a.studentCode());
			u.setFullName(a.fullName());
			u.setEmail(a.email());
			u.setPasswordHash(hash);
			u.setMustChangePassword(false);
			users.add(u);
		}
		return userRepository.saveAll(users).stream().map(User::getId).toList();
	}
}
