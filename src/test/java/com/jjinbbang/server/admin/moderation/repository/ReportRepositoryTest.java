package com.jjinbbang.server.admin.moderation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.admin.moderation.type.ReportStatus;

/**
 * 신고 목록은 리뷰 단위로 페이징한다. {@code GROUP BY}·{@code ORDER BY MAX(...)}가 실제 MySQL에서
 * 의도대로 도는지는 Mockito로 확인할 수 없어 Testcontainers에 직접 붙는다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("ReportRepository")
class ReportRepositoryTest {

	@Container
	@ServiceConnection
	static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	@Autowired
	DataSource dataSource;

	@Autowired
	ReportRepository reportRepository;

	private JdbcTemplate jdbcTemplate;

	private Long userId;

	@BeforeEach
	void setUp() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		// FK가 걸린 자식 테이블(reports → reviews → users → universities)부터 지운다.
		jdbcTemplate.update("DELETE FROM reports");
		jdbcTemplate.update("DELETE FROM reviews");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM universities");

		userId = insertUser(insertUniversity("부산대학교"));
	}

	@Test
	@DisplayName("신고가 여러 건인 리뷰도 한 행으로 세고, 최근 신고일 순으로 정렬한다")
	void 리뷰_단위로_묶고_최근_신고일_순으로_정렬한다() {
		// given — 리뷰 A의 신고 사이에 리뷰 B의 신고가 시간상 끼어 있다
		Long reviewA = insertReview("집주인이 별로예요");
		Long reviewB = insertReview("알바 구함");
		insertReport(reviewA, ReportStatus.PENDING, at(1));
		insertReport(reviewB, ReportStatus.PENDING, at(2));
		insertReport(reviewA, ReportStatus.PENDING, at(3));

		// when
		Page<Long> page = reportRepository.findReportedReviewIds(PageRequest.of(0, 10));

		// then
		assertThat(page.getContent()).containsExactly(reviewA, reviewB);
		assertThat(page.getTotalElements()).isEqualTo(2);
	}

	@Test
	@DisplayName("페이지 크기는 신고가 아니라 리뷰 개수로 자른다")
	void 페이지는_리뷰_개수로_자른다() {
		// given — 리뷰 A에 신고 3건, B·C에 1건씩. 신고 단위로 2건씩 자르면 A가 두 페이지에 걸친다
		Long reviewA = insertReview("A");
		Long reviewB = insertReview("B");
		Long reviewC = insertReview("C");
		insertReport(reviewA, ReportStatus.PENDING, at(5));
		insertReport(reviewA, ReportStatus.PENDING, at(4));
		insertReport(reviewA, ReportStatus.PENDING, at(3));
		insertReport(reviewB, ReportStatus.PENDING, at(2));
		insertReport(reviewC, ReportStatus.PENDING, at(1));

		// when
		Page<Long> first = reportRepository.findReportedReviewIds(PageRequest.of(0, 2));
		Page<Long> second = reportRepository.findReportedReviewIds(PageRequest.of(1, 2));

		// then
		assertThat(first.getContent()).containsExactly(reviewA, reviewB);
		assertThat(second.getContent()).containsExactly(reviewC);
		assertThat(first.getTotalElements()).isEqualTo(3);
		assertThat(first.getTotalPages()).isEqualTo(2);
	}

	@Test
	@DisplayName("상태 필터가 있으면 그 상태의 신고가 하나라도 있는 리뷰만, 그 상태의 최근 신고일 순으로 고른다")
	void 상태_필터는_리뷰_선별과_정렬에_모두_걸린다() {
		// given — A의 가장 최근 신고는 기각됐다. PENDING 기준으로는 B가 더 최근이다
		Long reviewA = insertReview("A");
		Long reviewB = insertReview("B");
		Long handledOnly = insertReview("처리 끝난 리뷰");
		insertReport(reviewA, ReportStatus.PENDING, at(1));
		insertReport(reviewA, ReportStatus.REJECT, at(9));
		insertReport(reviewB, ReportStatus.PENDING, at(2));
		insertReport(handledOnly, ReportStatus.APPROVE, at(5));

		// when
		Page<Long> page = reportRepository.findReportedReviewIdsByStatus(ReportStatus.PENDING, PageRequest.of(0, 10));

		// then
		assertThat(page.getContent()).containsExactly(reviewB, reviewA);
		assertThat(page.getTotalElements()).isEqualTo(2);
	}

	@Test
	@Transactional
	@DisplayName("리뷰 ID로 신고를 모으면 오래된 순으로, 리뷰 작성자 학교까지 함께 가져온다")
	void 리뷰_ID로_신고를_모은다() {
		// given
		Long reviewA = insertReview("A");
		Long other = insertReview("다른 리뷰");
		Long later = insertReport(reviewA, ReportStatus.PENDING, at(3));
		Long earlier = insertReport(reviewA, ReportStatus.PENDING, at(1));
		insertReport(other, ReportStatus.PENDING, at(2));

		// when
		List<Report> reports = reportRepository.findAllWithReviewByReviewIdIn(List.of(reviewA));

		// then
		assertThat(reports).extracting(Report::getId).containsExactly(earlier, later);
		assertThat(reports.getFirst().getReview().getUser().getUniversity().getName()).isEqualTo("부산대학교");
	}

	@Test
	@DisplayName("상태 필터가 있으면 그 상태의 신고만 모은다")
	void 상태_필터가_있으면_그_상태의_신고만_모은다() {
		// given
		Long reviewA = insertReview("A");
		Long pending = insertReport(reviewA, ReportStatus.PENDING, at(1));
		insertReport(reviewA, ReportStatus.REJECT, at(2));

		// when
		List<Report> reports = reportRepository.findAllWithReviewByReviewIdInAndStatus(
			List.of(reviewA), ReportStatus.PENDING
		);

		// then
		assertThat(reports).extracting(Report::getId).containsExactly(pending);
	}

	private LocalDateTime at(int hour) {
		return LocalDateTime.of(2026, 6, 2, hour, 0);
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

	private Long insertReview(String content) {
		jdbcTemplate.update("""
			INSERT INTO reviews (user_id, status, type, rating, content, like_count)
			VALUES (?, 'PUBLIC', 'GENERAL', 5, ?, 0)
			""", userId, content);
		return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	private Long insertReport(Long reviewId, ReportStatus status, LocalDateTime createdAt) {
		jdbcTemplate.update("""
			INSERT INTO reports (user_id, review_id, reason, status, created_at)
			VALUES (?, ?, '욕설·비방', ?, ?)
			""", userId, reviewId, status.name(), createdAt);
		return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}
}
