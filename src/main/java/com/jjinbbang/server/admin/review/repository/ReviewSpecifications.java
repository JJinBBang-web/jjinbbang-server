package com.jjinbbang.server.admin.review.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.jjinbbang.server.admin.moderation.entity.ProhibitedWordFlag;
import com.jjinbbang.server.admin.moderation.entity.ProhibitedWordFlag_;
import com.jjinbbang.server.domain.common.entity.University;
import com.jjinbbang.server.domain.common.entity.University_;
import com.jjinbbang.server.domain.review.entity.Review;
import com.jjinbbang.server.domain.review.entity.Review_;
import com.jjinbbang.server.domain.review.type.ReviewStatus;
import com.jjinbbang.server.domain.user.entity.User;
import com.jjinbbang.server.domain.user.entity.User_;

import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * 어드민 리뷰 목록의 다중 필터 조건.
 *
 * <p>선택 필터가 다섯 개(키워드·학교·기간·상태·금칙어만보기)라 {@code @Query}에
 * {@code :param IS NULL OR ...}를 다 몰아넣으면 enum 파라미터({@code status})의 타입 추론이
 * 불안정해진다 — {@code ReportRepository}가 상태 필터 하나 때문에 메서드를 둘로 나눈 이유와 같다.
 * 여기는 조합이 32가지라 메서드 분리로는 감당이 안 돼 {@link Specification}으로 조립한다.
 *
 * <p>{@code Review}는 {@code domain}에 있지만 이 클래스는 {@code admin}에 둔다 — 금칙어만 보기
 * 필터가 {@link ProhibitedWordFlag}(admin 소속)를 직접 참조해야 해서다. {@code domain}은
 * {@code admin}을 참조하면 안 되므로(AGENTS.md 아키텍처 규칙), 이 필터 조립 로직 자체를
 * {@code domain/review/repository}가 아니라 여기에 둔다.
 *
 * <p>{@code user}·{@code university} fetch는 count 쿼리에도 그대로 붙이면 안 된다.
 * {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor}가 페이징 total을
 * 구하려고 같은 {@link Specification}으로 결과 타입 {@code Long}인 count 쿼리를 별도로 만드는데,
 * 이때는 fetch 대신 predicate에 필요한 최소 join만 붙인다.
 */
public final class ReviewSpecifications {

	private ReviewSpecifications() {
	}

	public static Specification<Review> withFilters(
		String keyword,
		List<String> schoolNames,
		ReviewStatus status,
		LocalDateTime createdAfter,
		boolean hasBadWordOnly
	) {
		boolean needsUniversityJoin = schoolNames != null && !schoolNames.isEmpty();

		return (root, query, cb) -> {
			FetchParent<User, University> universityJoin = null;

			if (Long.class == query.getResultType()) {
				// count 쿼리: schoolNames 필터가 없으면 user·university까지 조인할 이유가 없다.
				if (needsUniversityJoin) {
					Join<Review, User> userJoin = root.join(Review_.user, JoinType.INNER);
					universityJoin = userJoin.join(User_.university, JoinType.INNER);
				}
			} else {
				// 데이터 쿼리: ReviewResponse가 매번 schoolName·userName을 내려줘야 해서 항상 fetch한다.
				FetchParent<Review, User> userFetch = root.fetch(Review_.user, JoinType.INNER);
				universityJoin = userFetch.fetch(User_.university, JoinType.INNER);
			}

			List<Predicate> predicates = new ArrayList<>();

			if (keyword != null && !keyword.isBlank()) {
				predicates.add(cb.like(root.get(Review_.content), "%" + keyword + "%"));
			}
			if (needsUniversityJoin) {
				predicates.add(((Join<User, University>) universityJoin).get(University_.name).in(schoolNames));
			}
			if (status != null) {
				predicates.add(cb.equal(root.get(Review_.status), status));
			}
			if (createdAfter != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get(Review_.createdAt), createdAfter));
			}
			if (hasBadWordOnly) {
				Subquery<Long> flagExists = query.subquery(Long.class);
				Root<ProhibitedWordFlag> flagRoot = flagExists.from(ProhibitedWordFlag.class);
				flagExists.select(flagRoot.get(ProhibitedWordFlag_.id))
					.where(cb.equal(flagRoot.get(ProhibitedWordFlag_.review), root));
				predicates.add(cb.exists(flagExists));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
