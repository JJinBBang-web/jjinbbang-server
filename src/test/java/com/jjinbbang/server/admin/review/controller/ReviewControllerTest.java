package com.jjinbbang.server.admin.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.jjinbbang.server.admin.review.dto.response.ReviewListResponse;
import com.jjinbbang.server.admin.review.dto.response.ReviewResponse;
import com.jjinbbang.server.admin.review.service.ReviewService;
import com.jjinbbang.server.admin.review.type.ReviewPeriodType;
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
			.build();
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
