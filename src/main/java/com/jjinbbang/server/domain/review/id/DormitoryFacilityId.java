package com.jjinbbang.server.domain.review.id;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DormitoryFacilityId implements Serializable {

	@Column(name = "dorm_review_id")
	private Long dormReviewId;

	@Column(name = "facility_id")
	private Long facilityId;
}
