package com.ltmtu.ielts;

import org.springframework.boot.SpringApplication;

public class TestIeltsApplication {

	public static void main(String[] args) {
		SpringApplication.from(IeltsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
