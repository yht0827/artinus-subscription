package com.artinus.subscription.infrastructure;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MySQLContainer;

public final class TestcontainersConfiguration {

	private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

	private TestcontainersConfiguration() {
	}

	public static void startMysql(DynamicPropertyRegistry registry) {
		MYSQL.start();
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
	}
}
