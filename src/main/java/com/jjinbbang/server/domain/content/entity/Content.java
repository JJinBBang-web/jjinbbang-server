package com.jjinbbang.server.domain.content.entity;

import com.jjinbbang.server.global.persistence.SoftDeleteEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends SoftDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "thumbnail_image", nullable = false, length = 2048)
	private String thumbnailImage;

	@Convert(converter = ContentCategoryConverter.class)
	@Column(nullable = false)
	private ContentCategory category;

	@Column(nullable = false)
	private String title;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "share_count", nullable = false)
	private Integer shareCount;

	@Column(name = "like_count", nullable = false)
	private Integer likeCount;

	@Column(name = "view_count", nullable = false)
	private Integer viewCount;
}
