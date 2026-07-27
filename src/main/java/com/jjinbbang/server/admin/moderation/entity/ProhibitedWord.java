package com.jjinbbang.server.admin.moderation.entity;

import com.jjinbbang.server.admin.administrator.entity.Admin;
import com.jjinbbang.server.global.persistence.SoftDeleteEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "prohibited_words")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProhibitedWord extends SoftDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "admin_id")
	private Admin admin;

	@Column(nullable = false)
	private String word;

	@Column(name = "is_enabled", nullable = false)
	private Boolean enabled;
}
