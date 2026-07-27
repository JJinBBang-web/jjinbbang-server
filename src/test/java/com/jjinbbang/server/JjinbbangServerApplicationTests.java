package com.jjinbbang.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import jakarta.persistence.EntityManagerFactory;

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

	private static final Set<String> EXPECTED_TABLES = Set.of(
		"action_history",
		"admins",
		"admission_certificates",
		"agencies",
		"agency_likes",
		"agency_reviews",
		"building_likes",
		"buildings",
		"campuses",
		"contents",
		"contents_likes",
		"dorm_reviews",
		"dormitory_facilities",
		"facilities",
		"general_reviews",
		"images",
		"keywords",
		"prohibited_word_flags",
		"prohibited_words",
		"reports",
		"review_keywords",
		"review_likes",
		"reviews",
		"universities",
		"users"
	);

	@Container
	@ServiceConnection
	static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	@Autowired
	DataSource dataSource;

	@Autowired
	EntityManagerFactory entityManagerFactory;

	@Test
	@DisplayName("Flyway 스키마와 전체 엔티티 매핑이 일치한다")
	void contextLoads() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		Set<String> actualTables = Set.copyOf(jdbcTemplate.queryForList("""
			SELECT table_name
			FROM information_schema.tables
			WHERE table_schema = DATABASE()
			  AND table_name <> 'flyway_schema_history'
			""", String.class));

		assertThat(actualTables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
		assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(EXPECTED_TABLES.size());
	}
}
