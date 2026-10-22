package com.ltmtu.ielts.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class PasswordGeneratorTest {

	@RepeatedTest(200)
	@DisplayName("BR-02: mật khẩu tạm 8 ký tự, có cả chữ và số, không có ký tự dễ nhầm")
	void format() {
		String p = PasswordGenerator.temporaryPassword();

		assertThat(p).hasSize(8).matches(".*[A-Za-z].*").matches(".*\\d.*").doesNotContainPattern("[0O1lI]");
	}

	@Test
	@DisplayName("Mật khẩu tạm không lặp lại")
	void unique() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			seen.add(PasswordGenerator.temporaryPassword());
		}
		assertThat(seen).hasSize(1000);
	}
}
