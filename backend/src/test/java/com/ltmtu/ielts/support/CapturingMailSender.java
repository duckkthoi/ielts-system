package com.ltmtu.ielts.support;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Giữ email trong bộ nhớ thay vì gửi qua SMTP, để test kiểm tra nội dung email. */
public class CapturingMailSender extends JavaMailSenderImpl {

	private final List<SimpleMailMessage> sent = new CopyOnWriteArrayList<>();

	@Override
	public void send(SimpleMailMessage... simpleMessages) {
		sent.addAll(List.of(simpleMessages));
	}

	public List<SimpleMailMessage> sent() {
		return List.copyOf(sent);
	}

	/** Email gửi nền (@Async) nên phải chờ; hết thời gian thì trả danh sách hiện có. */
	public List<SimpleMailMessage> awaitMatching(Predicate<SimpleMailMessage> filter, int expected, Duration timeout)
			throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (true) {
			List<SimpleMailMessage> matching = sent.stream().filter(filter).toList();
			if (matching.size() >= expected || System.nanoTime() > deadline) {
				return matching;
			}
			Thread.sleep(50);
		}
	}
}
