package com.jjinbbang.server.admin.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.jjinbbang.server.admin.review.controller.ReviewController;
import com.jjinbbang.server.admin.review.dto.response.ReviewListResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewResponse;
import com.jjinbbang.server.admin.review.type.ReviewSortType;
import com.jjinbbang.server.global.error.GlobalExceptionHandler;

/**
 * 목록 정렬은 Criteria 서브쿼리({@code ORDER BY})를 사용하므로 Mockito로는 실제 순서를 검증할 수 없다.
 * 서비스부터 MySQL까지 그대로 통과시켜서, 신고 건수 정렬이 count 쿼리·페이징과 함께 동작하는지까지 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("ReviewService 목록 정렬 (MySQL)")
class ReviewServiceSortTest {

	@Container
	@ServiceConnection
	static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	@Autowired
	DataSource dataSource;

	@Autowired
	ReviewService reviewService;

	private JdbcTemplate jdbcTemplate;

	private Long seoulUniversityId;
	private Long busanUniversityId;

	@BeforeEach
	void setUp() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		// FK가 걸린 자식 테이블부터 지운다.
		jdbcTemplate.update("DELETE FROM reports");
		jdbcTemplate.update("DELETE FROM reviews");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM universities");

		seoulUniversityId = insertUniversity("경상국립대학교");
		busanUniversityId = insertUniversity("부산대학교");
	}

	@ParameterizedTest
	@CsvSource({
		"LATEST,최신,중간,오래됨",
		"REPORT,중간,오래됨,최신",
		"RATING_HIGH,오래됨,최신,중간",
		"RATING_LOW,중간,최신,오래됨"
	})
	@DisplayName("프론트의 네 가지 정렬 요청은 MySQL 결과 순서를 바꾸고 삭제 리뷰를 제외한 페이지와 건수를 반환한다")
	void 프론트_요청부터_페이지_응답까지_검증한다(String sort, String first, String second, String third) throws Exception {
		Long userId = insertUser(seoulUniversityId);
		LocalDateTime now = LocalDateTime.now();
		Long older = insertReview(userId, "오래됨", 5, now.minusDays(2));
		Long middle = insertReview(userId, "중간", 1, now.minusDays(1));
		insertReview(userId, "최신", 3, now);
		Long deleted = insertReview(userId, "삭제 리뷰", 5, now.plusMinutes(1));
		insertReport(older, "PENDING");
		for (int i = 0; i < 3; i++) {
			insertReport(middle, "PENDING");
			insertReport(deleted, "PENDING");
		}
		reviewService.delete(deleted);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewService))
			.setControllerAdvice(new GlobalExceptionHandler()).build();

		mockMvc.perform(get("/api/admin/reviews")
			.param("hasBadWordOnly", "false").param("sort", sort).param("page", "0").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.reviewList.length()").value(2))
			.andExpect(jsonPath("$.data.reviewList[0].content").value(first))
			.andExpect(jsonPath("$.data.reviewList[1].content").value(second))
			.andExpect(jsonPath("$.data.pageInfo.totalElements").value(3))
			.andExpect(jsonPath("$.data.pageInfo.totalPages").value(2));
		mockMvc.perform(get("/api/admin/reviews")
			.param("hasBadWordOnly", "false").param("sort", sort).param("page", "1").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.reviewList.length()").value(1))
			.andExpect(jsonPath("$.data.reviewList[0].content").value(third))
			.andExpect(jsonPath("$.data.pageInfo.totalElements").value(3));
	}

	@Test
	@DisplayName("별점순은 별점을 방향대로 정렬한다")
	void 별점순으로_정렬한다() {
		// given
		Long userId = insertUser(seoulUniversityId);
		insertReview(userId, "별점 3", 3, LocalDateTime.now());
		insertReview(userId, "별점 5", 5, LocalDateTime.now());
		insertReview(userId, "별점 1", 1, LocalDateTime.now());

		// when & then
		assertThat(contents(findAll(ReviewSortType.RATING_HIGH))).containsExactly("별점 5", "별점 3", "별점 1");
		assertThat(contents(findAll(ReviewSortType.RATING_LOW))).containsExactly("별점 1", "별점 3", "별점 5");
	}

	@Test
	@DisplayName("별점이 같으면 방향과 무관하게 최신 리뷰가 먼저 나온다")
	void 별점_동률은_최신순이다() {
		// given
		Long userId = insertUser(seoulUniversityId);
		insertReview(userId, "오래된 4점", 4, LocalDateTime.now().minusDays(2));
		insertReview(userId, "최신 4점", 4, LocalDateTime.now());
		insertReview(userId, "중간 4점", 4, LocalDateTime.now().minusDays(1));

		// when & then
		assertThat(contents(findAll(ReviewSortType.RATING_HIGH)))
			.containsExactly("최신 4점", "중간 4점", "오래된 4점");
		assertThat(contents(findAll(ReviewSortType.RATING_LOW)))
			.containsExactly("최신 4점", "중간 4점", "오래된 4점");
	}

	@Test
	@DisplayName("최신순은 작성일 내림차순으로 정렬한다")
	void 최신순으로_정렬한다() {
		// given
		Long userId = insertUser(seoulUniversityId);
		insertReview(userId, "중간", 3, LocalDateTime.now().minusDays(1));
		insertReview(userId, "최신", 3, LocalDateTime.now());
		insertReview(userId, "오래됨", 3, LocalDateTime.now().minusDays(2));

		// when & then
		assertThat(contents(findAll(ReviewSortType.LATEST))).containsExactly("최신", "중간", "오래됨");
	}

	@Test
	@DisplayName("신고순은 신고 건수 내림차순으로 정렬하고, 응답의 reportCount 와 같은 값을 기준으로 한다")
	void 신고순으로_정렬한다() {
		// given
		Long userId = insertUser(seoulUniversityId);
		insertReview(userId, "신고 없음", 3, LocalDateTime.now());
		Long many = insertReview(userId, "신고 3건", 3, LocalDateTime.now());
		Long one = insertReview(userId, "신고 1건", 3, LocalDateTime.now());
		// 처리 상태와 무관하게 센다 — 목록의 reportCount 가 모든 상태를 세기 때문이다.
		insertReport(many, "PENDING");
		insertReport(many, "REJECT");
		insertReport(many, "APPROVE");
		insertReport(one, "REJECT");

		// when
		ReviewListResponse descending = findAll(ReviewSortType.REPORT);

		// then
		assertThat(contents(descending)).containsExactly("신고 3건", "신고 1건", "신고 없음");
		assertThat(reportCounts(descending)).containsExactly(3L, 1L, 0L);
	}

	@Test
	@DisplayName("신고 건수가 같으면 최신 리뷰가 먼저 나온다")
	void 신고_동률은_최신순이다() {
		// given
		Long userId = insertUser(seoulUniversityId);
		Long older = insertReview(userId, "오래된 신고 1건", 3, LocalDateTime.now().minusDays(1));
		Long newer = insertReview(userId, "최신 신고 1건", 3, LocalDateTime.now());
		insertReport(older, "PENDING");
		insertReport(newer, "PENDING");

		// when & then
		assertThat(contents(findAll(ReviewSortType.REPORT)))
			.containsExactly("최신 신고 1건", "오래된 신고 1건");
	}

	@Test
	@DisplayName("신고순은 페이지를 넘겨도 중복·누락 없이 이어지고 전체 건수가 맞다")
	void 신고순은_페이지_경계에서도_순서가_유지된다() {
		// given — 신고 건수가 같은 리뷰가 페이지 경계에 걸리도록 일부러 동률을 섞는다.
		Long userId = insertUser(seoulUniversityId);
		int[] reportCounts = {2, 0, 2, 1, 2, 0, 1};
		for (int i = 0; i < reportCounts.length; i++) {
			Long reviewId = insertReview(userId, "리뷰" + i, 3, LocalDateTime.now().minusMinutes(i));
			for (int count = 0; count < reportCounts[i]; count++) {
				insertReport(reviewId, "PENDING");
			}
		}

		// when
		List<String> collected = new ArrayList<>();
		ReviewListResponse first = findAll(ReviewSortType.REPORT, 0, 3);
		collected.addAll(contents(first));
		collected.addAll(contents(findAll(ReviewSortType.REPORT, 1, 3)));
		collected.addAll(contents(findAll(ReviewSortType.REPORT, 2, 3)));

		// then — 신고 많은 순, 동률은 최신(minutes 가 작은) 순
		assertThat(first.pageInfo().totalElements()).isEqualTo(7);
		assertThat(first.pageInfo().totalPages()).isEqualTo(3);
		assertThat(collected).containsExactly("리뷰0", "리뷰2", "리뷰4", "리뷰3", "리뷰6", "리뷰1", "리뷰5");
	}

	@Test
	@DisplayName("신고순은 학교 필터와 함께 써도 필터된 건수만 센다")
	void 신고순은_필터와_함께_동작한다() {
		// given
		Long seoulUserId = insertUser(seoulUniversityId);
		Long busanUserId = insertUser(busanUniversityId);
		Long seoulReview = insertReview(seoulUserId, "경상대 신고 5건", 3, LocalDateTime.now());
		Long busanFew = insertReview(busanUserId, "부산대 신고 1건", 3, LocalDateTime.now());
		Long busanMany = insertReview(busanUserId, "부산대 신고 2건", 3, LocalDateTime.now());
		for (int i = 0; i < 5; i++) {
			insertReport(seoulReview, "PENDING");
		}
		insertReport(busanFew, "PENDING");
		insertReport(busanMany, "PENDING");
		insertReport(busanMany, "PENDING");

		// when
		ReviewListResponse response = reviewService.findAll(
			null, List.of("부산대학교"), null, null, false,
			ReviewSortType.REPORT, PageRequest.of(0, 10)
		);

		// then
		assertThat(contents(response)).containsExactly("부산대 신고 2건", "부산대 신고 1건");
		assertThat(response.pageInfo().totalElements()).isEqualTo(2);
	}

	private ReviewListResponse findAll(ReviewSortType sortType) {
		return findAll(sortType, 0, 10);
	}

	private ReviewListResponse findAll(ReviewSortType sortType, int page, int size) {
		return reviewService.findAll(null, null, null, null, false, sortType, PageRequest.of(page, size));
	}

	private List<String> contents(ReviewListResponse response) {
		return response.reviewList().stream().map(ReviewResponse::content).toList();
	}

	private List<Long> reportCounts(ReviewListResponse response) {
		return response.reviewList().stream().map(ReviewResponse::reportCount).toList();
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

	private Long insertReview(Long userId, String content, int rating, LocalDateTime createdAt) {
		jdbcTemplate.update("""
			INSERT INTO reviews (user_id, status, type, rating, content, like_count, created_at)
			VALUES (?, 'PUBLIC', 'GENERAL', ?, ?, 0, ?)
			""", userId, rating, content, createdAt);
		return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	private void insertReport(Long reviewId, String status) {
		jdbcTemplate.update(
			"INSERT INTO reports (review_id, reason, status) VALUES (?, 'SPAM', ?)",
			reviewId, status
		);
	}
}
