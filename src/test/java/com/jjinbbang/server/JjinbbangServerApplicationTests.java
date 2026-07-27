package com.jjinbbang.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 컨텍스트 로딩 확인. 엔티티·Flyway 가 붙으면 이 테스트가 <b>스키마-엔티티 정합성 검사</b>가 된다
 * ({@code ddl-auto: validate}).
 *
 * <p>테스트는 {@code test} 프로파일로만 돈다. 운영 프로파일을 활성화하거나 실제 DB 를 건드리는
 * 테스트를 만들지 않는다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("애플리케이션 컨텍스트")
class JjinbbangServerApplicationTests {

	@Container
	@ServiceConnection
	static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	@Test
	@DisplayName("MySQL에서 컨텍스트가 로딩된다")
	void contextLoads() {
	}
}
