package com.jjinbbang.server.domain.agency.entity;

import com.jjinbbang.server.domain.agency.id.AgencyLikeId;
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
@Table(name = "agency_likes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgencyLike {

	@EmbeddedId
	private AgencyLikeId id;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@MapsId("agencyId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "agency_id", nullable = false)
	private Agency agency;
}
