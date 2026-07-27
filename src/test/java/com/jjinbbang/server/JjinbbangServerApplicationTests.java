package com.jjinbbang.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.sql.DataSource;

import com.jjinbbang.server.domain.content.entity.Content;
import com.jjinbbang.server.domain.content.type.ContentCategory;
import com.jjinbbang.server.domain.review.entity.GeneralReview;
import com.jjinbbang.server.domain.review.type.ContractType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import jakarta.persistence.EntityManager;
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

	@Autowired
	EntityManager entityManager;

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
		assertThat(entityManagerFactory.getMetamodel().getEntities())
			.allSatisfy(entity -> assertThat(entity.getJavaType().getPackageName()).endsWith(".entity"));
		assertThat(entityManagerFactory.getMetamodel().getEmbeddables())
			.hasSize(6)
			.allSatisfy(embeddable -> assertThat(embeddable.getJavaType().getPackageName()).endsWith(".id"));
	}

	@Test
	@Transactional
	@DisplayName("커스텀 문자열 enum 값을 converter 없이 읽고 쓴다")
	void customEnumValueRoundTrip() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		jdbcTemplate.update("""
			INSERT INTO contents (
			    thumbnail_image,
			    category,
			    title,
			    content,
			    share_count,
			    like_count,
			    view_count
			) VALUES (?, ?, ?, ?, ?, ?, ?)
			""", "https://example.com/thumbnail.png", "부동산", "enum mapping", "content", 0, 0, 0);

		Long contentId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
		entityManager.clear();

		Content content = entityManager.find(Content.class, contentId);
		assertThat(content.getCategory()).isEqualTo(ContentCategory.REAL_ESTATE);

		ReflectionTestUtils.setField(content, "category", ContentCategory.MOVING);
		entityManager.flush();

		String storedCategory = jdbcTemplate.queryForObject(
			"SELECT category FROM contents WHERE id = ?",
			String.class,
			contentId
		);
		assertThat(storedCategory).isEqualTo("이사관련");

		jdbcTemplate.update(
			"INSERT INTO universities (name, logo) VALUES (?, ?)",
			"enum mapping university",
			"https://example.com/logo.png"
		);
		Long universityId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

		jdbcTemplate.update("""
			INSERT INTO users (university_id, provider, provider_id, verification_status)
			VALUES (?, ?, ?, ?)
			""", universityId, "GOOGLE", "enum-mapping-user", "UNVERIFIED");
		Long userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

		jdbcTemplate.update("""
			INSERT INTO reviews (user_id, status, type, rating, content, like_count)
			VALUES (?, ?, ?, ?, ?, ?)
			""", userId, "PUBLIC", "GENERAL", 5, "enum mapping review", 0);
		Long reviewId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

		jdbcTemplate.update("""
			INSERT INTO general_reviews (id, floor, area, contract_type)
			VALUES (?, ?, ?, ?)
			""", reviewId, "3", 20.0, "월세");
		entityManager.clear();

		GeneralReview generalReview = entityManager.find(GeneralReview.class, reviewId);
		assertThat(generalReview.getContractType()).isEqualTo(ContractType.MONTHLY_RENT);

		ReflectionTestUtils.setField(generalReview, "contractType", ContractType.DEPOSIT_RENT);
		entityManager.flush();

		String storedContractType = jdbcTemplate.queryForObject(
			"SELECT contract_type FROM general_reviews WHERE id = ?",
			String.class,
			reviewId
		);
		assertThat(storedContractType).isEqualTo("전세");
	}
}
