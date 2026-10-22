package com.ltmtu.ielts.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ltmtu.ielts.config.AppProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Tạo tài khoản admin đầu tiên từ biến môi trường nếu hệ thống chưa có admin. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AppProperties props;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (userRepository.existsByRole(Role.ADMIN)) {
			return;
		}
		AppProperties.Admin cfg = props.admin();
		User admin = new User();
		admin.setUsername(cfg.username());
		admin.setPasswordHash(passwordEncoder.encode(cfg.password()));
		admin.setFullName(cfg.fullName());
		admin.setEmail(cfg.email());
		admin.setRole(Role.ADMIN);
		admin.setMustChangePassword(true);
		userRepository.save(admin);
		log.info("Đã tạo tài khoản admin '{}'. Hãy đổi mật khẩu ở lần đăng nhập đầu tiên.", cfg.username());
	}
}
