package com.jjinbbang.server.admin.review.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.admin.moderation.entity.Report_;
import com.jjinbbang.server.admin.review.type.ReviewSortType;
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
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
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
			predicates.add(cb.isNull(root.get(Review_.deletedAt)));

			if (keyword != null && !keyword.isBlank()) {
				predicates.add(cb.like(root.get(Review_.originalContent), "%" + keyword + "%"));
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
				predicates.add(cb.isTrue(root.get(Review_.prohibitedWordFlag)));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	/**
	 * 프론트의 정렬 선택지에 맞는 {@code ORDER BY}를 건다. 모든 기준을 여기서 처리한다.
	 *
	 * <p>{@code REPORT}는 신고 건수가 {@code reviews}의 컬럼이 아니라 {@code reports}를 세야 나와서
	 * {@link org.springframework.data.domain.Sort}로 표현할 수 없다 — 상관 서브쿼리를 {@code ORDER BY}에 쓴다.
	 * 기준마다 {@code Sort}와 Specification으로 나누면 정렬 방식이 둘이 되므로 한곳에서 건다.
	 *
	 * <p>정렬은 {@code query.orderBy}로 직접 건다. {@code Pageable}에 정렬이 있으면 Spring Data가
	 * 그 정렬로 덮어쓰므로, 이 Specification을 쓸 때는 정렬 없는 {@code Pageable}을 넘겨야 한다.
	 * count 쿼리에는 정렬이 필요 없어 건너뛴다.
	 *
	 * <p>신고 건수는 처리 상태를 가리지 않고 센다 — 목록에 내려주는 {@code reportCount}와 같은 기준이다.
	 *
	 * <p>동률일 때 페이지 경계에서 행이 중복되거나 빠지지 않도록 {@code id}까지 정렬 키에 넣는다.
	 * 모든 정렬의 동률은 최신순({@code createdAt DESC, id DESC})으로 푼다.
	 */
	public static Specification<Review> orderBy(ReviewSortType sortType) {
		return (root, query, cb) -> {
			if (Long.class == query.getResultType()) {
				return null;
			}

			Path<?> createdAt = root.get(Review_.createdAt);
			Path<?> id = root.get(Review_.id);

			List<Order> orders = switch (sortType) {
				case LATEST -> List.of(
					cb.desc(createdAt),
					cb.desc(id)
				);
				case RATING_HIGH, RATING_LOW -> List.of(
					sortType == ReviewSortType.RATING_LOW ? cb.asc(root.get(Review_.rating)) : cb.desc(root.get(Review_.rating)),
					cb.desc(createdAt),
					cb.desc(id)
				);
				case REPORT -> {
					Subquery<Long> reportCount = query.subquery(Long.class);
					Root<Report> report = reportCount.from(Report.class);
					reportCount.select(cb.count(report)).where(cb.equal(report.get(Report_.review), root));

					yield List.of(cb.desc(reportCount), cb.desc(createdAt), cb.desc(id));
				}
			};
			query.orderBy(orders);

			return null;
		};
	}
}
