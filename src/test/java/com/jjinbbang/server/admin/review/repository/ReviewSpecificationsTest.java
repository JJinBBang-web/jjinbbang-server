package com.jjinbbang.server.admin.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.domain.review.repository.ReviewRepository;
import com.jjinbbang.server.domain.review.type.ReviewStatus;

/**
 * {@link ReviewSpecifications}는 Criteria API로 조립하는 동적 쿼리라 Mockito로는
 * 실제 필터·조인 동작을 검증할 수 없다 — 그래서 Testcontainers MySQL에 직접 붙어서 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("ReviewSpecifications")
class ReviewSpecificationsTest {

	@Container
	@ServiceConnection
	static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	@Autowired
	DataSource dataSource;

	@Autowired
	ReviewRepository reviewRepository;

	private JdbcTemplate jdbcTemplate;

	private Long seoulUniversityId;
	private Long busanUniversityId;

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		// FK가 걸린 자식 테이블(prohibited_word_flags → reviews·prohibited_words, reviews → users, users → universities)부터 지운다.
		jdbcTemplate.update("DELETE FROM prohibited_word_flags");
		jdbcTemplate.update("DELETE FROM reviews");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM universities");
		jdbcTemplate.update("DELETE FROM prohibited_words");

		seoulUniversityId = insertUniversity("경상국립대학교");
		busanUniversityId = insertUniversity("부산대학교");
	}

	@Test
	@DisplayName("검색어는 리뷰 본문(content)에 포함된 것만 찾는다")
	void 검색어로_본문을_필터링한다() {
		// given
		Long userId = insertUser(seoulUniversityId);
		insertReview(userId, "방음이 안 돼요", LocalDateTime.now());
		insertReview(userId, "관리비가 저렴해요", LocalDateTime.now());

		// when
		Page<Review> page = reviewRepository.findAll(
			ReviewSpecifications.withFilters("방음", null, null, null, false),
			PageRequest.of(0, 10)
		);

		// then
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().getFirst().getContent()).isEqualTo("방음이 안 돼요");
	}

	@Test
	@DisplayName("schoolNames 는 소속 대학 이름으로 필터링한다")
	void schoolNames로_필터링한다() {
		// given
		Long seoulUserId = insertUser(seoulUniversityId);
		Long busanUserId = insertUser(busanUniversityId);
		insertReview(seoulUserId, "경상국립대 리뷰", LocalDateTime.now());
		insertReview(busanUserId, "부산대 리뷰", LocalDateTime.now());

		// when
		Page<Review> page = reviewRepository.findAll(
			ReviewSpecifications.withFilters(null, List.of("부산대학교"), null, null, false),
			PageRequest.of(0, 10)
		);

		// then
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().getFirst().getContent()).isEqualTo("부산대 리뷰");
	}

	@Test
	@DisplayName("status 는 공개·비공개 상태로 필터링한다")
	void status로_필터링한다() {
		// given
		Long userId = insertUser(seoulUniversityId);
		insertReview(userId, "공개 리뷰", ReviewStatus.PUBLIC, LocalDateTime.now());
		insertReview(userId, "비공개 리뷰", ReviewStatus.PRIVATE, LocalDateTime.now());

		// when
		Page<Review> page = reviewRepository.findAll(
			ReviewSpecifications.withFilters(null, null, ReviewStatus.PRIVATE, null, false),
			PageRequest.of(0, 10)
		);

		// then
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().getFirst().getContent()).isEqualTo("비공개 리뷰");
	}

	@Test
	@DisplayName("createdAfter 는 그 시각 이후에 작성된 리뷰만 찾는다")
	void 기간으로_필터링한다() {
		// given
		Long userId = insertUser(seoulUniversityId);
		insertReview(userId, "오래된 리뷰", LocalDateTime.now().minusDays(30));
		insertReview(userId, "최근 리뷰", LocalDateTime.now());

		// when
		Page<Review> page = reviewRepository.findAll(
			ReviewSpecifications.withFilters(null, null, null, LocalDateTime.now().minusDays(7), false),
			PageRequest.of(0, 10)
		);

		// then
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().getFirst().getContent()).isEqualTo("최근 리뷰");
	}

	@Test
	@DisplayName("hasBadWordOnly 는 금칙어 플래그가 있는 리뷰만 찾는다")
	void hasBadWordOnly로_필터링한다() {
		// given
		Long userId = insertUser(seoulUniversityId);
		Long flaggedReviewId = insertReview(userId, "욕설 포함 리뷰", LocalDateTime.now());
		insertReview(userId, "평범한 리뷰", LocalDateTime.now());
		flagReview(flaggedReviewId);

		// when
		Page<Review> page = reviewRepository.findAll(
			ReviewSpecifications.withFilters(null, null, null, null, true),
			PageRequest.of(0, 10)
		);

		// then
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().getFirst().getId()).isEqualTo(flaggedReviewId);
	}

	@Test
	@DisplayName("작성자·소속 대학이 fetch 로 미리 로드돼서 추가 쿼리 없이 읽을 수 있다")
	void 작성자와_소속_대학이_fetch로_로드된다() {
		// given
		Long userId = insertUser(seoulUniversityId);
		insertReview(userId, "리뷰", LocalDateTime.now());

		// when
		Page<Review> page = reviewRepository.findAll(
			ReviewSpecifications.withFilters(null, null, null, null, false),
			PageRequest.of(0, 10)
		);

		// then — LazyInitializationException 없이 바로 접근되면 fetch 가 된 것이다
		Review review = page.getContent().getFirst();
		assertThat(review.getUser().getUniversity().getName()).isEqualTo("경상국립대학교");
	}

	private Long insertUniversity(String name) {
		jdbcTemplate.update("INSERT INTO universities (name, logo) VALUES (?, ?)", name, "https://example.com/logo.png");
		return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	private Long insertUser(Long universityId) {
		jdbcTemplate.update("""
			INSERT INTO users (university_id, provider, provider_id, verification_status)
			VALUES (?, 'GOOGLE', ?, 'UNVERIFIED')
			""", universityId, "provider-" + System.nanoTime());
		return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	private Long insertReview(Long userId, String content, LocalDateTime createdAt) {
		return insertReview(userId, content, ReviewStatus.PUBLIC, createdAt);
	}

	private Long insertReview(Long userId, String content, ReviewStatus status, LocalDateTime createdAt) {
		jdbcTemplate.update("""
			INSERT INTO reviews (user_id, status, type, rating, content, like_count, created_at)
			VALUES (?, ?, 'GENERAL', 5, ?, 0, ?)
			""", userId, status.name(), content, createdAt);
		return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	private void flagReview(Long reviewId) {
		jdbcTemplate.update("INSERT INTO prohibited_words (word, is_enabled) VALUES (?, true)", "욕설-" + System.nanoTime());
		Long wordId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
		jdbcTemplate.update(
			"INSERT INTO prohibited_word_flags (review_id, prohibited_word_id) VALUES (?, ?)",
			reviewId, wordId
		);
	}
}
