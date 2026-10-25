package com.jjinbbang.server.admin.verification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("합격증명서 스키마")
class AdmissionCertificateSchemaTest {

	@Container
	@ServiceConnection
	static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	@Autowired
	DataSource dataSource;

	@Test
	@Transactional
	@DisplayName("반려 사유는 REJECT 상태의 합격증명서에만 저장할 수 있다")
	void rejectReasonIsAllowedOnlyForRejectedCertificate() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		jdbcTemplate.update(
			"INSERT INTO universities (name, logo) VALUES (?, ?)",
			"reject reason university",
			"https://example.com/reject-reason-logo.png"
		);
		Long universityId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

		jdbcTemplate.update("""
			INSERT INTO users (university_id, provider, provider_id, verification_status)
			VALUES (?, ?, ?, ?)
			""", universityId, "GOOGLE", "reject-reason-user", "UNVERIFIED");
		Long userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

		jdbcTemplate.update("""
			INSERT INTO admission_certificates (user_id, url, status, reject_reason)
			VALUES (?, ?, ?, ?)
			""", userId, "https://example.com/rejected.png", "REJECT", "식별 정보가 선명하지 않습니다.");

		assertThatThrownBy(() -> jdbcTemplate.update("""
			INSERT INTO admission_certificates (user_id, url, status, reject_reason)
			VALUES (?, ?, ?, ?)
			""", userId, "https://example.com/pending.png", "PENDING", "저장되면 안 되는 사유"))
			.isInstanceOf(UncategorizedSQLException.class)
			.hasMessageContaining("chk_admission_certificates_reject_reason_status");
	}
}
