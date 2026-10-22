package com.ltmtu.ielts.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ltmtu.ielts.config.AppProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Lưu ảnh đề bài vào thư mục {@code app.upload-dir} (volume Docker khi triển khai) và trả về đường dẫn
 * {@code /uploads/yyyy/MM/<uuid>.<đuôi>}. Loại file xác định bằng <b>chữ ký byte đầu file</b>, không tin tên file hay
 * Content-Type do client gửi.
 */
@Slf4j
@Service
public class FileStorageService {

	public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
	public static final String URL_PREFIX = "/uploads/";

	private final Path root;
	private final Clock clock;

	public FileStorageService(AppProperties props, Clock clock) {
		this.root = Path.of(props.uploadDir()).toAbsolutePath().normalize();
		this.clock = clock;
	}

	public Path root() {
		return root;
	}

	public String storeImage(MultipartFile file) {
		if (file.isEmpty()) {
			throw ApiException.badRequest("BAD_FILE", "File rỗng");
		}
		if (file.getSize() > MAX_IMAGE_BYTES) {
			throw new ApiException(org.springframework.http.HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE",
					"Ảnh tối đa 5 MB");
		}
		byte[] head = new byte[12];
		try (InputStream in = file.getInputStream()) {
			int n = in.readNBytes(head, 0, head.length);
			head = Arrays.copyOf(head, n);
		} catch (IOException e) {
			throw ApiException.badRequest("BAD_FILE", "Không đọc được file");
		}
		String ext = detectImageExtension(head);
		if (ext == null) {
			throw ApiException.badRequest("BAD_FILE", "Chỉ nhận ảnh PNG, JPG hoặc WEBP");
		}
		ZonedDateTime now = clock.instant().atZone(ZoneOffset.UTC);
		String relative = "%d/%02d/%s.%s".formatted(now.getYear(), now.getMonthValue(), UUID.randomUUID(), ext);
		Path target = root.resolve(relative).normalize();
		try {
			Files.createDirectories(target.getParent());
			file.transferTo(target);
		} catch (IOException e) {
			throw new UncheckedIOException("Không lưu được file", e);
		}
		log.info("Đã lưu ảnh {} ({} byte)", relative, file.getSize());
		return URL_PREFIX + relative;
	}

	/** Nhận diện PNG / JPEG / WEBP theo magic bytes; không phải ảnh hỗ trợ → {@code null}. */
	static String detectImageExtension(byte[] h) {
		if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
			return "png";
		}
		if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
			return "jpg";
		}
		if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E'
				&& h[10] == 'B' && h[11] == 'P') {
			return "webp";
		}
		return null;
	}
}
