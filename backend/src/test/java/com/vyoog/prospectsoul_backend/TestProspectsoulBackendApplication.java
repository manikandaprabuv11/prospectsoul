package com.vyoog.prospectsoul_backend;

import org.springframework.boot.SpringApplication;

public class TestProspectsoulBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(ProspectsoulBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
