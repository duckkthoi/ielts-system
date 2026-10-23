package com.ltmtu.ielts.tcpgateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Đọc khung tin: dòng lệnh + phần dữ liệu đúng số byte (docs/05 mục C.1). */
class FrameReaderTest {

	private static FrameReader reader(String data, int maxLine) {
		return new FrameReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), maxLine);
	}

	@Test
	@DisplayName("Tách dòng theo LF, bỏ CR cuối; đọc đúng số byte phần CSV ngay sau dòng IMPORT (kể cả chữ có dấu)")
	void linesThenPayload() throws Exception {
		String csv = "ma_sv,ho_ten\n2151000001,Nguyễn Văn A\n";
		int bytes = csv.getBytes(StandardCharsets.UTF_8).length;
		FrameReader r = reader("AUTH x\r\nIMPORT K15 " + bytes + "\n" + csv + "PING\n", 1024);
		assertThat(r.readLine()).isEqualTo("AUTH x");
		assertThat(r.readLine()).isEqualTo("IMPORT K15 " + bytes);
		assertThat(new String(r.readExactly(bytes), StandardCharsets.UTF_8)).isEqualTo(csv);
		assertThat(r.readLine()).isEqualTo("PING");
		assertThat(r.readLine()).isNull();
	}

	@Test
	@DisplayName("Dòng cuối không có LF vẫn đọc được; luồng rỗng → null")
	void lastLineWithoutNewline() throws Exception {
		FrameReader r = reader("QUIT", 1024);
		assertThat(r.readLine()).isEqualTo("QUIT");
		assertThat(r.readLine()).isNull();
	}

	@Test
	@DisplayName("Dòng dài quá giới hạn → lỗi 400")
	void lineTooLong() {
		FrameReader r = reader("A".repeat(20) + "\n", 10);
		assertThatThrownBy(r::readLine)
				.isInstanceOf(TcpException.class)
				.satisfies(e -> assertThat(((TcpException) e).code()).isEqualTo(400));
	}

	@Test
	@DisplayName("Thiếu byte dữ liệu (client đóng kết nối giữa chừng) → EOFException")
	void missingBytes() {
		FrameReader r = reader("abc", 1024);
		assertThatThrownBy(() -> r.readExactly(10)).isInstanceOf(EOFException.class);
	}
}
