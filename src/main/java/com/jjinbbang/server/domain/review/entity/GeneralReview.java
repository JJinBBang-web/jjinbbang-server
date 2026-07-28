package com.jjinbbang.server.domain.review.entity;

import com.jjinbbang.server.domain.review.type.ContractType;

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
@Table(name = "general_reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeneralReview {

	@Id
	private Long id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id", nullable = false)
	private Review review;

	@Column(nullable = false)
	private String floor;

	@Column(nullable = false)
	private Double area;

	@Enumerated(EnumType.STRING)
	@Column(name = "contract_type", nullable = false, length = 20)
	private ContractType contractType;

	private Integer deposit;

	private Integer price;

	@Column(name = "maintenance_cost")
	private Integer maintenanceCost;
}
