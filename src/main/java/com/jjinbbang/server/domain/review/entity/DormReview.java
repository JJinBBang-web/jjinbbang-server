package com.jjinbbang.server.domain.review.entity;

import com.jjinbbang.server.domain.review.type.DormFloor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "dorm_reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DormReview {

	@Id
	private Long id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id", nullable = false)
	private Review review;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private DormFloor floor;

	@Column(nullable = false)
	private Integer capacity;

	@Column(name = "dorm_fee", nullable = false)
	private Integer dormFee;

	@Column(name = "current_region")
	private String currentRegion;

	@Column(name = "current_grade")
	private Double currentGrade;
}
