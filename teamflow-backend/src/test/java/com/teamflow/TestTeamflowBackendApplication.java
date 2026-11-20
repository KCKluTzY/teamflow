package com.teamflow;

import org.springframework.boot.SpringApplication;

public class TestTeamflowBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(TeamflowBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
