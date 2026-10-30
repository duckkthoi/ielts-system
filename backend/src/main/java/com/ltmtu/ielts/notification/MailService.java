package com.ltmtu.ielts.notification;

import java.util.List;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ltmtu.ielts.config.AppProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gửi email (SMTP). Luôn chạy nền ({@code @Async}): lỗi gửi chỉ ghi log, không làm hỏng nghiệp vụ đã xong
 * (UC-11 ngoại lệ E3). Không ghi mật khẩu hay nội dung email vào log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

	private final JavaMailSender mailSender;
	private final AppProperties props;

	/** Thông tin một tài khoản sinh viên vừa được tạo, để gửi email đăng nhập (FR-14). */
	public record NewAccountMail(String email, String fullName, String username, String temporaryPassword) {
	}

	/** Người nhận email thông báo. */
	public record Recipient(String email, String fullName) {
	}

	/**
	 * FR-51: email đi kèm thông báo trong ứng dụng (bài mới, đã trả bài, yêu cầu sửa, nhắc hạn). {@code link} là đường
	 * dẫn trong frontend, được ghép với {@code app.mail.app-url}. Người nhận không có email thì bỏ qua.
	 */
	@Async
	public void sendNotification(List<Recipient> recipients, String subject, String body, String link) {
		int sent = 0;
		for (Recipient r : recipients) {
			if (r.email() == null || r.email().isBlank()) {
				continue;
			}
			SimpleMailMessage msg = new SimpleMailMessage();
			msg.setFrom(props.mail().from());
			msg.setTo(r.email());
			msg.setSubject(subject);
			msg.setText("""
					Chào %s,

					%s
					%s
					Xem chi tiết: %s%s

					Đây là email tự động, vui lòng không trả lời.
					""".formatted(r.fullName(), subject, body == null ? "" : body + "\n", props.mail().appUrl(),
					link == null ? "" : link));
			try {
				mailSender.send(msg);
				sent++;
			} catch (MailException e) {
				log.warn("Không gửi được một email thông báo: {}", e.getMessage());
			}
		}
		log.info("Đã gửi {}/{} email thông báo", sent, recipients.size());
	}

	@Async
	public void sendNewStudentAccounts(String classroomName, List<NewAccountMail> accounts) {
		int sent = 0;
		for (NewAccountMail a : accounts) {
			if (a.email() == null || a.email().isBlank()) {
				continue;
			}
			try {
				mailSender.send(newAccountMessage(classroomName, a));
				sent++;
			} catch (MailException e) {
				log.warn("Không gửi được email tài khoản cho {}: {}", a.username(), e.getMessage());
			}
		}
		log.info("Đã gửi {} email tài khoản (lớp {})", sent, classroomName);
	}

	private SimpleMailMessage newAccountMessage(String classroomName, NewAccountMail a) {
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setFrom(props.mail().from());
		msg.setTo(a.email());
		msg.setSubject("Tài khoản hệ thống AcaWrite");
		msg.setText("""
				Chào %s,

				Bạn đã được thêm vào lớp "%s" trên AcaWrite — hệ thống giao bài và chấm bài IELTS Writing, paper học thuật.

				Tên đăng nhập: %s
				Mật khẩu tạm:  %s

				Đăng nhập tại: %s
				Bạn sẽ được yêu cầu đổi mật khẩu ở lần đăng nhập đầu tiên.

				Đây là email tự động, vui lòng không trả lời.
				""".formatted(a.fullName(), classroomName, a.username(), a.temporaryPassword(),
				props.mail().loginUrl()));
		return msg;
	}
}
