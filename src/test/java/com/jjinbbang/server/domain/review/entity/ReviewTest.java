package com.jjinbbang.server.domain.review.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Review 마스킹")
class ReviewTest {

	@Test
	@DisplayName("마스킹 전에는 마스킹된 상태가 아니고 getContent 는 원문을 돌려준다")
	void 마스킹_전에는_원문을_돌려준다() {
		Review review = review("집주인이 욕설 진짜 별로예요");

		assertThat(review.isMasked()).isFalse();
		assertThat(review.getContent()).isEqualTo("집주인이 욕설 진짜 별로예요");
	}

	@Test
	@DisplayName("마스킹하면 getContent 는 마스킹본을 돌려주고 원문과 플래그는 따로 유지된다")
	void 마스킹하면_마스킹본을_돌려준다() {
		Review review = review("집주인이 욕설 진짜 별로예요");

		review.mask("집주인이 ** 진짜 별로예요");

		assertThat(review.isMasked()).isTrue();
		assertThat(review.getContent()).isEqualTo("집주인이 ** 진짜 별로예요");
		assertThat(review.getOriginalContent()).isEqualTo("집주인이 욕설 진짜 별로예요");
		assertThat(review.isProhibitedWordFlag()).isTrue();
	}

	@Test
	@DisplayName("해제하면 원문이 다시 보이고 마스킹 상태와 금칙어 플래그가 풀린다")
	void 해제하면_원문이_다시_보인다() {
		Review review = review("집주인이 욕설 진짜 별로예요");
		review.mask("집주인이 ** 진짜 별로예요");

		review.unmask();

		assertThat(review.isMasked()).isFalse();
		assertThat(review.getContent()).isEqualTo("집주인이 욕설 진짜 별로예요");
		assertThat(review.isProhibitedWordFlag()).isFalse();
	}

	private Review review(String originalContent) {
		Review review = BeanUtils.instantiateClass(Review.class);
		ReflectionTestUtils.setField(review, "originalContent", originalContent);
		return review;
	}
}
