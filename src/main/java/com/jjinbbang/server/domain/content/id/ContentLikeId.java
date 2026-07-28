package com.jjinbbang.server.domain.content.id;

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
public class ContentLikeId implements Serializable {

	@Column(name = "contents_id")
	private Long contentId;

	@Column(name = "user_id")
	private Long userId;
}
