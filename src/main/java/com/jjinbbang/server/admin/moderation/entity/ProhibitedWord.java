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

	/**
	 * 금칙어를 등록한다. 등록 직후에는 활성 상태다.
	 *
	 * <p>{@code admin}은 넣지 않는다 — {@code admin_id}가 nullable이고, 등록자를 알아내려면
	 * 인증이 붙어야 한다. SSO 연동 때 등록자를 함께 받도록 이 팩터리를 늘린다.
	 */
	public static ProhibitedWord create(String word) {
		ProhibitedWord prohibitedWord = new ProhibitedWord();
		prohibitedWord.word = word;
		prohibitedWord.enabled = true;
		return prohibitedWord;
	}

	public void changeEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
