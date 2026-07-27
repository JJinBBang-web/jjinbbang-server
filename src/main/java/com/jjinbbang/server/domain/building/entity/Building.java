package com.jjinbbang.server.domain.building.entity;

import com.jjinbbang.server.domain.common.entity.Campus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "buildings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Building {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campus_id", nullable = false)
	private Campus campus;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BuildingType type;

	@Column(nullable = false)
	private String address;

	@Column(nullable = false)
	private Double latitude;

	@Column(nullable = false)
	private Double longitude;

	private Double area;

	@Column(nullable = false)
	private Double rating;

	@Column(name = "review_count", nullable = false)
	private Integer reviewCount;

	@Column(name = "like_count", nullable = false)
	private Integer likeCount;

	@Column(name = "image_count", nullable = false)
	private Integer imageCount;
}
