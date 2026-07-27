package com.jjinbbang.server.domain.content.entity;

import com.jjinbbang.server.domain.content.id.ContentLikeId;
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
@Table(name = "contents_likes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentLike {

	@EmbeddedId
	private ContentLikeId id;

	@MapsId("contentId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "contents_id", nullable = false)
	private Content content;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
}
