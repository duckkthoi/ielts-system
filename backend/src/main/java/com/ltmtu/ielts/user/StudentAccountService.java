package com.ltmtu.ielts.user;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Tạo / tra cứu tài khoản sinh viên cho luồng import (UC-11, UC-12).
 * Tên đăng nhập của sinh viên = mã sinh viên (BR-01).
 */
@Service
@RequiredArgsConstructor
public class StudentAccountService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	/** Kết quả tra cứu một mã sinh viên trước khi import. */
	public sealed interface Lookup {
		/** Đã có tài khoản sinh viên với mã này. */
		record Existing(User student) implements Lookup {
		}

		/** Chưa có, và mã chưa bị tài khoản khác dùng làm tên đăng nhập → tạo được. */
		record Available() implements Lookup {
		}

		/** Mã trùng tên đăng nhập của một tài khoản không phải sinh viên (vd giáo viên). */
		record UsernameTaken() implements Lookup {
		}
	}

	@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
	public Lookup lookup(String studentCode) {
		Optional<User> byCode = userRepository.findByStudentCodeIgnoreCase(studentCode);
		if (byCode.isPresent()) {
			return new Lookup.Existing(byCode.get());
		}
		return userRepository.existsByUsernameIgnoreCase(studentCode) ? new Lookup.UsernameTaken()
				: new Lookup.Available();
	}

	/**
	 * Tạo tài khoản sinh viên với mật khẩu tạm, bắt đổi mật khẩu lần đầu (BR-02).
	 * Phải chạy trong transaction của dòng import đang xử lý.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public User createStudent(String studentCode, String fullName, String email, String temporaryPassword) {
		User u = new User();
		u.setUsername(studentCode);
		u.setStudentCode(studentCode);
		u.setFullName(fullName);
		u.setEmail(email);
		u.setRole(Role.STUDENT);
		u.setPasswordHash(passwordEncoder.encode(temporaryPassword));
		u.setMustChangePassword(true);
		return userRepository.saveAndFlush(u);
	}
}
