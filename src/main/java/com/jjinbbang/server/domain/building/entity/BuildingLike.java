package com.jjinbbang.server.domain.building.entity;

import com.jjinbbang.server.domain.building.id.BuildingLikeId;
import com.jjinbbang.server.domain.user.entity.User;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
@Table(name = "building_likes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuildingLike {

	@EmbeddedId
	private BuildingLikeId id;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@MapsId("buildingId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "building_id", nullable = false)
	private Building building;
}
