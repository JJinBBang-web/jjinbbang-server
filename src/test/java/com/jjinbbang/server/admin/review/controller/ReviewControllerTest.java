package com.jjinbbang.server.admin.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.jjinbbang.server.admin.administrator.security.AdminOidcUser;
import com.jjinbbang.server.admin.review.dto.response.ReviewDetailResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewListResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewMaskPreviewResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewResponse;
import com.jjinbbang.server.admin.review.service.ReviewService;
import com.jjinbbang.server.admin.review.type.ReviewMaskReason;
import com.jjinbbang.server.admin.review.type.ReviewPeriodType;
import com.jjinbbang.server.domain.review.exception.ReviewErrorCode;
import com.jjinbbang.server.domain.review.type.ReviewStatus;
import com.jjinbbang.server.global.error.GlobalExceptionHandler;
import com.jjinbbang.server.global.paging.PageInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewController")
class ReviewControllerTest {

	@Mock
	private ReviewService reviewService;

	@InjectMocks
	private ReviewController reviewController;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
			.setControllerAdvice(new GlobalExceptionHandler())
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("목록 조회는 reviewList 와 pageInfo 로 나간다")
	void 목록_응답_형태() throws Exception {
		// given
		given(reviewService.findAll(any(), any(), any(), any(), anyBoolean(), any())).willReturn(listResponse());

		// when & then
		mockMvc.perform(get("/api/admin/reviews"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.message").value("리뷰 목록 조회 성공"))
			.andExpect(jsonPath("$.data.reviewList[0].id").value(1))
			.andExpect(jsonPath("$.data.reviewList[0].schoolName").value("찐빵대학교"))
			.andExpect(jsonPath("$.data.reviewList[0].hasBadWord").value(false))
			.andExpect(jsonPath("$.data.pageInfo.totalElements").value(1));
	}

	@Test
	@DisplayName("명세서에 있던 title 은 응답에 없다")
	void 응답에_title은_없다() throws Exception {
		// given
		given(reviewService.findAll(any(), any(), any(), any(), anyBoolean(), any())).willReturn(listResponse());

		// when & then
		mockMvc.perform(get("/api/admin/reviews"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.reviewList[0].title").doesNotExist());
	}

	@Test
	@DisplayName("파라미터를 하나도 안 주면 기본값(빈 필터·0페이지·20건·최신순)으로 조회한다")
	void 파라미터가_없으면_기본값으로_조회한다() throws Exception {
		// given
		given(reviewService.findAll(any(), any(), any(), any(), anyBoolean(), any())).willReturn(listResponse());

		// when
		mockMvc.perform(get("/api/admin/reviews")).andExpect(status().isOk());

		// then
		then(reviewService).should().findAll(
			isNull(), isNull(), isNull(), isNull(), eq(false), any(Pageable.class)
		);
	}

	@Test
	@DisplayName("schoolNames 를 여러 번 주면 목록으로 바인딩된다")
	void schoolNames_는_목록으로_바인딩된다() throws Exception {
		// given
		given(reviewService.findAll(any(), any(), any(), any(), anyBoolean(), any())).willReturn(listResponse());

		// when
		mockMvc.perform(get("/api/admin/reviews")
			.param("schoolNames", "경상국립대학교", "부산대학교"))
			.andExpect(status().isOk());

		// then
		then(reviewService).should().findAll(
			isNull(), eq(List.of("경상국립대학교", "부산대학교")), isNull(), isNull(), eq(false), any(Pageable.class)
		);
	}

	@Test
	@DisplayName("periodType 이 enum 밖의 값이면 400 INVALID_TYPE")
	void 알_수_없는_periodType은_400() throws Exception {
		mockMvc.perform(get("/api/admin/reviews").param("periodType", "LAST_100_DAYS"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_TYPE"))
			.andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다. (periodType)"));
	}

	@Test
	@DisplayName("status 가 enum 밖의 값이면 400 INVALID_TYPE")
	void 알_수_없는_status는_400() throws Exception {
		mockMvc.perform(get("/api/admin/reviews").param("status", "DELETED"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_TYPE"))
			.andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다. (status)"));
	}

	@Test
	@DisplayName("hasBadWordOnly=true 를 주면 그대로 서비스에 전달된다")
	void hasBadWordOnly가_그대로_전달된다() throws Exception {
		// given
		given(reviewService.findAll(any(), any(), any(), any(), anyBoolean(), any())).willReturn(listResponse());

		// when
		mockMvc.perform(get("/api/admin/reviews")
			.param("periodType", "LAST_7_DAYS")
			.param("status", "PUBLIC")
			.param("hasBadWordOnly", "true"))
			.andExpect(status().isOk());

		// then
		then(reviewService).should().findAll(
			isNull(), isNull(), eq(ReviewPeriodType.LAST_7_DAYS), eq(ReviewStatus.PUBLIC), eq(true), any(Pageable.class)
		);
	}

	@Test
	@DisplayName("상세 조회는 리뷰 상세 필드를 그대로 응답한다")
	void 상세_응답_형태() throws Exception {
		// given
		given(reviewService.findById(1L)).willReturn(detailResponse());

		// when & then
		mockMvc.perform(get("/api/admin/reviews/{reviewId}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.message").value("리뷰 상세 조회 성공"))
			.andExpect(jsonPath("$.data.reviewId").value(1))
			.andExpect(jsonPath("$.data.schoolName").value("찐빵대학교"))
			.andExpect(jsonPath("$.data.hasBadWordFlag").value(true))
			.andExpect(jsonPath("$.data.email").value("kim***@pusan.ac.kr"))
			.andExpect(jsonPath("$.data.title").doesNotExist())
			.andExpect(jsonPath("$.data.reportList[0].reportReason").value("욕설·비방"))
			.andExpect(jsonPath("$.data.reportList[0].reporterId").value(5))
			.andExpect(jsonPath("$.data.historyList[0].actionNames[0]").value("신고 기각"))
			.andExpect(jsonPath("$.data.historyList[0].handler").value("ddochi"));
	}

	@Test
	@DisplayName("존재하지 않는 리뷰를 상세 조회하면 404 REVIEW_NOT_FOUND")
	void 존재하지_않는_리뷰_상세는_404() throws Exception {
		// given
		given(reviewService.findById(999L)).willThrow(ReviewErrorCode.REVIEW_NOT_FOUND.exception());

		// when & then
		mockMvc.perform(get("/api/admin/reviews/{reviewId}", 999L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("REVIEW_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("해당 리뷰 정보가 존재하지 않습니다."));
	}

	@Test
	@DisplayName("reviewId 가 숫자가 아니면 400 INVALID_TYPE")
	void 숫자가_아닌_reviewId는_400() throws Exception {
		mockMvc.perform(get("/api/admin/reviews/{reviewId}", "abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_TYPE"));
	}

	@Test
	@DisplayName("삭제는 data 없이 성공 메시지만 응답한다")
	void 삭제_응답_형태() throws Exception {
		// when & then
		mockMvc.perform(delete("/api/admin/reviews/{reviewId}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.message").value("리뷰 삭제 성공"))
			.andExpect(jsonPath("$.data").value(nullValue()));

		then(reviewService).should().delete(1L);
	}

	@Test
	@DisplayName("존재하지 않는 리뷰를 삭제하면 404 REVIEW_NOT_FOUND")
	void 존재하지_않는_리뷰_삭제는_404() throws Exception {
		// given
		willThrow(ReviewErrorCode.REVIEW_NOT_FOUND.exception()).given(reviewService).delete(999L);

		// when & then
		mockMvc.perform(delete("/api/admin/reviews/{reviewId}", 999L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("REVIEW_NOT_FOUND"));
	}

	@Test
	@DisplayName("이미 삭제된 리뷰를 다시 삭제하면 409 REVIEW_ALREADY_DELETED")
	void 이미_삭제된_리뷰_삭제는_409() throws Exception {
		// given
		willThrow(ReviewErrorCode.REVIEW_ALREADY_DELETED.exception()).given(reviewService).delete(1L);

		// when & then
		mockMvc.perform(delete("/api/admin/reviews/{reviewId}", 1L))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errorCode").value("REVIEW_ALREADY_DELETED"));
	}

	@Test
	@DisplayName("상태를 변경하면 200 이고 data 는 비어 있다")
	void 상태_변경은_200() throws Exception {
		// when & then
		mockMvc.perform(patch("/api/admin/reviews/{reviewId}/status", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"PRIVATE\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("리뷰 상태 변경 성공"))
			.andExpect(jsonPath("$.data").value(nullValue()));

		then(reviewService).should().updateStatus(1L, ReviewStatus.PRIVATE);
	}

	@Test
	@DisplayName("존재하지 않는 리뷰는 상태를 변경하면 404 REVIEW_NOT_FOUND")
	void 존재하지_않는_리뷰_상태_변경은_404() throws Exception {
		// given
		willThrow(ReviewErrorCode.REVIEW_NOT_FOUND.exception())
			.given(reviewService).updateStatus(999L, ReviewStatus.PRIVATE);

		// when & then
		mockMvc.perform(patch("/api/admin/reviews/{reviewId}/status", 999L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"PRIVATE\"}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("REVIEW_NOT_FOUND"));
	}

	@Test
	@DisplayName("status 를 빼면 400 이다 — 빠진 값이 조용히 특정 상태로 정해지지 않는다")
	void status_누락은_400() throws Exception {
		mockMvc.perform(patch("/api/admin/reviews/{reviewId}/status", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.errors[0].field").value("status"));
	}

	@Test
	@DisplayName("status 가 PUBLIC/PRIVATE 밖의 값이면 400 이다")
	void status가_enum_밖의_값이면_400() throws Exception {
		mockMvc.perform(patch("/api/admin/reviews/{reviewId}/status", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"BANANA\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
	}

	@Test
	@DisplayName("마스킹 미리보기는 금칙어가 치환된 본문을 응답한다")
	void 마스킹_미리보기_응답_형태() throws Exception {
		// given
		given(reviewService.previewMask(1L)).willReturn(ReviewMaskPreviewResponse.from("집주인이 ** 진짜 별로예요"));

		// when & then
		mockMvc.perform(get("/api/admin/reviews/{reviewId}/mask", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.message").value("리뷰 마스킹 미리보기 조회 성공"))
			.andExpect(jsonPath("$.data.maskedContent").value("집주인이 ** 진짜 별로예요"));
	}

	@Test
	@DisplayName("존재하지 않는 리뷰를 마스킹 미리보기 조회하면 404 REVIEW_NOT_FOUND")
	void 존재하지_않는_리뷰_마스킹_미리보기는_404() throws Exception {
		// given
		given(reviewService.previewMask(999L)).willThrow(ReviewErrorCode.REVIEW_NOT_FOUND.exception());

		// when & then
		mockMvc.perform(get("/api/admin/reviews/{reviewId}/mask", 999L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("REVIEW_NOT_FOUND"));
	}

	@Test
	@DisplayName("마스킹을 확정하면 200 이고 data 는 비어 있다")
	void 마스킹_확정은_200() throws Exception {
		// when & then
		mockMvc.perform(post("/api/admin/reviews/{reviewId}/mask", 1L)
				.with(admin(9L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reasons\":[\"BAD_WORD\"],\"detailReason\":\"직접 입력한 사유\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("리뷰 마스킹 성공"))
			.andExpect(jsonPath("$.data").value(nullValue()));

		then(reviewService).should().mask(1L, 9L, List.of(ReviewMaskReason.BAD_WORD), "직접 입력한 사유");
	}

	@Test
	@DisplayName("사유를 여러 개 선택하면 그대로 서비스에 전달된다")
	void 여러_사유를_선택하면_그대로_전달된다() throws Exception {
		// when & then
		mockMvc.perform(post("/api/admin/reviews/{reviewId}/mask", 1L)
				.with(admin(9L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reasons\":[\"BAD_WORD\",\"PRIVACY_EXPOSURE\"]}"))
			.andExpect(status().isOk());

		then(reviewService).should()
			.mask(1L, 9L, List.of(ReviewMaskReason.BAD_WORD, ReviewMaskReason.PRIVACY_EXPOSURE), null);
	}

	@Test
	@DisplayName("reasons 를 빼면 400 이다 — 빠진 값이 조용히 특정 사유로 정해지지 않는다")
	void reasons_누락은_400() throws Exception {
		mockMvc.perform(post("/api/admin/reviews/{reviewId}/mask", 1L)
				.with(admin(9L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.errors[0].field").value("reasons"));
	}

	@Test
	@DisplayName("reasons 가 빈 배열이면 400 이다")
	void reasons가_빈_배열이면_400() throws Exception {
		mockMvc.perform(post("/api/admin/reviews/{reviewId}/mask", 1L)
				.with(admin(9L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reasons\":[]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
	}

	@Test
	@DisplayName("존재하지 않는 리뷰를 마스킹 확정하면 404 REVIEW_NOT_FOUND")
	void 존재하지_않는_리뷰_마스킹_확정은_404() throws Exception {
		// given
		willThrow(ReviewErrorCode.REVIEW_NOT_FOUND.exception())
			.given(reviewService).mask(999L, 9L, List.of(ReviewMaskReason.BAD_WORD), null);

		// when & then
		mockMvc.perform(post("/api/admin/reviews/{reviewId}/mask", 999L)
				.with(admin(9L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reasons\":[\"BAD_WORD\"]}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errorCode").value("REVIEW_NOT_FOUND"));
	}

	/** {@code standaloneSetup}은 시큐리티 필터체인이 없어 {@code SecurityContextHolder}를 직접 채운다. */
	private RequestPostProcessor admin(Long adminId) {
		return request -> {
			AdminOidcUser principal = new AdminOidcUser(mock(OidcUser.class), adminId);
			SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken(principal, null));
			return request;
		};
	}

	private ReviewDetailResponse detailResponse() {
		return new ReviewDetailResponse(
			1L, ReviewStatus.PUBLIC, "찐빵대학교", true, 1,
			LocalDateTime.of(2026, 6, 2, 0, 0), 1L, "계약 연장 문의 응대 관련 후기",
			List.of("https://img/1", "https://img/2"),
			5L, "장전동거주자", "kim***@pusan.ac.kr",
			List.of(new ReviewDetailResponse.ReportItem("욕설·비방", 5L, LocalDateTime.of(2026, 6, 2, 14, 30))),
			List.of(new ReviewDetailResponse.HistoryItem(
				List.of("신고 기각"), LocalDateTime.of(2026, 6, 2, 14, 2), "ddochi", "근거 부족"
			))
		);
	}

	private ReviewListResponse listResponse() {
		return new ReviewListResponse(
			List.of(new ReviewResponse(
				1L, ReviewStatus.PUBLIC, false, "찐빵대학교", "방음 안 됨", "익명의찐빵이",
				5, 0L, LocalDateTime.of(2026, 8, 1, 10, 0)
			)),
			new PageInfo(0, 20, 1, 1)
		);
	}
}
