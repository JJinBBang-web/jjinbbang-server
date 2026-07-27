package com.jjinbbang.server.domain.review.entity;

import com.jjinbbang.server.domain.common.entity.Facility;
import com.jjinbbang.server.domain.review.id.DormitoryFacilityId;
import com.jjinbbang.server.domain.review.type.UsageType;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "dormitory_facilities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DormitoryFacility {

	@EmbeddedId
	private DormitoryFacilityId id;

	@MapsId("dormReviewId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id", nullable = false)
	private DormReview dormReview;

	@MapsId("facilityId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "facility_id", nullable = false)
	private Facility facility;

	@Column(nullable = false)
	private Boolean available;

	@Enumerated(EnumType.STRING)
	@Column(name = "usage_type", length = 20)
	private UsageType usageType;
}
