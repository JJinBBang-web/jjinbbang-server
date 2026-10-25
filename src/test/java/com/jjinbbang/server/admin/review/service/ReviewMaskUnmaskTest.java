package com.jjinbbang.server.admin.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.jjinbbang.server.admin.review.dto.response.ReviewDetailResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewListResponse;
import com.jjinbbang.server.admin.review.type.ReviewMaskReason;
import com.jjinbbang.server.admin.review.type.ReviewSortType;
import com.jjinbbang.server.domain.review.exception.ReviewErrorCode;
import com.jjinbbang.server.global.error.BusinessException;

/**
 * 마스킹·해제는 {@code reviews.content}(원문)와 {@code masked_content}(마스킹본)를 오가는 영속성 동작이라
 * Mockito로는 실제 컬럼에 무엇이 남는지 확인할 수 없다. Flyway V7 적용과 Hibernate validation까지
 * 한 번에 통과한 채로, 서비스부터 MySQL까지 그대로 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("리뷰 마스킹·해제 (MySQL)")
class ReviewMaskUnmaskTest {

	private static final String ORIGINAL = "집주인이 욕설 진짜 별로예요";
	private static final String MASKED = "집주인이 ** 진짜 별로예요";

	@Container
	@ServiceConnection
	static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	@Autowired
	DataSource dataSource;

	@Autowired
	ReviewService reviewService;

	private JdbcTemplate jdbcTemplate;

	private Long adminId;
	private Long reviewId;

	@BeforeEach
	void setUp() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		// FK가 걸린 자식 테이블부터 지운다.
		jdbcTemplate.update("DELETE FROM action_history");
		jdbcTemplate.update("DELETE FROM reports");
		jdbcTemplate.update("DELETE FROM reviews");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM prohibited_words");
		jdbcTemplate.update("DELETE FROM admins");
		jdbcTemplate.update("DELETE FROM universities");

		jdbcTemplate.update("INSERT INTO universities (name, logo) VALUES (?, ?)", "찐빵대학교", "https://example.com/logo.png");
		Long universityId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

		jdbcTemplate.update("""
			INSERT INTO users (university_id, provider, provider_id, verification_status)
			VALUES (?, 'GOOGLE', 'provider-1', 'UNVERIFIED')
			""", universityId);
		Long userId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

		jdbcTemplate.update("""
			INSERT INTO admins (oidc_issuer, oidc_subject, username)
			VALUES ('https://issuer.example.com', 'subject-1', 'ddochi')
			""");
		adminId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

		jdbcTemplate.update("INSERT INTO prohibited_words (word, is_enabled) VALUES ('욕설', b'1')");

		jdbcTemplate.update("""
			INSERT INTO reviews (user_id, status, type, rating, content, like_count, created_at)
			VALUES (?, 'PUBLIC', 'GENERAL', 3, ?, 0, ?)
			""", userId, ORIGINAL, LocalDateTime.now());
		reviewId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	@Test
	@DisplayName("마스킹을 확정해도 원문(content)은 그대로이고 마스킹본은 masked_content 에 저장된다")
	void 마스킹해도_원문이_유지된다() {
		// when
		reviewService.mask(reviewId, adminId, List.of(ReviewMaskReason.BAD_WORD), null);

		// then
		Map<String, Object> row = reviewRow();
		assertThat(row.get("content")).isEqualTo(ORIGINAL);
		assertThat(row.get("masked_content")).isEqualTo(MASKED);
		assertThat(row.get("prohibited_word_flag")).isEqualTo(true);
	}

	@Test
	@DisplayName("마스킹된 리뷰는 목록·상세에서 마스킹본으로 나가고, 키워드 검색은 원문으로 한다")
	void 응답은_마스킹본이고_검색은_원문이다() {
		// given
		reviewService.mask(reviewId, adminId, List.of(ReviewMaskReason.BAD_WORD), null);

		// when
		ReviewListResponse byOriginalWord = list("욕설");
		ReviewDetailResponse detail = reviewService.findById(reviewId);

		// then — 마스킹본에는 "욕설"이 없지만 원문으로 검색되고, 응답에는 마스킹본이 나간다
		assertThat(byOriginalWord.reviewList()).hasSize(1);
		assertThat(byOriginalWord.reviewList().getFirst().content()).isEqualTo(MASKED);
		assertThat(detail.content()).isEqualTo(MASKED);
	}

	@Test
	@DisplayName("마스킹을 해제하면 마스킹본이 비워지고 플래그가 내려가며 원문이 다시 응답된다")
	void 해제하면_원문이_다시_응답된다() {
		// given
		reviewService.mask(reviewId, adminId, List.of(ReviewMaskReason.BAD_WORD), null);

		// when
		reviewService.unmask(reviewId, adminId, "오탐으로 확인");

		// then — DB
		Map<String, Object> row = reviewRow();
		assertThat(row.get("content")).isEqualTo(ORIGINAL);
		assertThat(row.get("masked_content")).isNull();
		assertThat(row.get("prohibited_word_flag")).isEqualTo(false);

		// then — 응답과 조치 이력
		ReviewDetailResponse detail = reviewService.findById(reviewId);
		assertThat(detail.content()).isEqualTo(ORIGINAL);
		assertThat(detail.hasBadWordFlag()).isFalse();
		assertThat(detail.historyList()).hasSize(2);
		assertThat(detail.historyList().get(0).actionNames()).containsExactly("BAD_WORD");
		assertThat(detail.historyList().get(1).actionNames()).containsExactly("UNMASK");
		assertThat(detail.historyList().get(1).reason()).isEqualTo("오탐으로 확인");
		assertThat(detail.historyList().get(1).handler()).isEqualTo("ddochi");
	}

	@Test
	@DisplayName("해제한 뒤 다시 마스킹할 수 있고, 마스킹은 항상 원문으로 다시 계산한다")
	void 해제_후_다시_마스킹할_수_있다() {
		// given
		reviewService.mask(reviewId, adminId, List.of(ReviewMaskReason.BAD_WORD), null);
		reviewService.unmask(reviewId, adminId, null);

		// when
		reviewService.mask(reviewId, adminId, List.of(ReviewMaskReason.BAD_WORD), null);

		// then
		Map<String, Object> row = reviewRow();
		assertThat(row.get("content")).isEqualTo(ORIGINAL);
		assertThat(row.get("masked_content")).isEqualTo(MASKED);
	}

	@Test
	@DisplayName("마스킹되지 않은 리뷰를 해제하면 409 REVIEW_NOT_MASKED 이고 조치 이력이 남지 않는다")
	void 마스킹되지_않은_리뷰는_해제할_수_없다() {
		// when & then
		assertThatThrownBy(() -> reviewService.unmask(reviewId, adminId, null))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ReviewErrorCode.REVIEW_NOT_MASKED);

		Integer historyCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM action_history", Integer.class);
		assertThat(historyCount).isZero();
	}

	private ReviewListResponse list(String keyword) {
		return reviewService.findAll(
			keyword, null, null, null, false,
			ReviewSortType.LATEST, Sort.Direction.DESC, PageRequest.of(0, 10)
		);
	}

	private Map<String, Object> reviewRow() {
		return jdbcTemplate.queryForMap(
			"SELECT content, masked_content, prohibited_word_flag FROM reviews WHERE id = ?", reviewId
		);
	}
}
