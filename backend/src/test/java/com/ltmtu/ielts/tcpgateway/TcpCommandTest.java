package com.ltmtu.ielts.tcpgateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Phân tích dòng lệnh TCP (docs/05 mục C.2). */
class TcpCommandTest {

	@Test
	@DisplayName("Lệnh đúng cú pháp; từ khoá không phân biệt hoa thường, nhiều dấu cách vẫn được")
	void valid() {
		assertThat(TcpCommand.parse("AUTH abc.def.ghi")).isEqualTo(new TcpCommand.Auth("abc.def.ghi"));
		assertThat(TcpCommand.parse("import  K15-A   128")).isEqualTo(new TcpCommand.Import("K15-A", 128, false));
		assertThat(TcpCommand.parse("IMPORT K15-A 128 email")).isEqualTo(new TcpCommand.Import("K15-A", 128, true));
		assertThat(TcpCommand.parse("PING")).isEqualTo(new TcpCommand.Ping());
		assertThat(TcpCommand.parse(" quit ")).isEqualTo(new TcpCommand.Quit());
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   ", "HELLO", "AUTH", "AUTH a b", "IMPORT K15", "IMPORT K15 abc", "IMPORT K15 0",
			"IMPORT K15 -5", "IMPORT K15 10 SMS", "IMPORT K15 10 EMAIL x", "IMPORT K15 99999999999", "PING now" })
	@DisplayName("Sai cú pháp → lỗi 400")
	void invalid(String line) {
		assertThatThrownBy(() -> TcpCommand.parse(line))
				.isInstanceOf(TcpException.class)
				.satisfies(e -> assertThat(((TcpException) e).code()).isEqualTo(400));
	}
}
