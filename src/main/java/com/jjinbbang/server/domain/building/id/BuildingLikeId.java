package com.jjinbbang.server.domain.building.id;

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
public class BuildingLikeId implements Serializable {

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "building_id")
	private Long buildingId;
}
