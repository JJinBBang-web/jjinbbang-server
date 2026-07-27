package com.jjinbbang.server.admin.administrator.entity;

import com.jjinbbang.server.admin.moderation.entity.ProhibitedWordFlag;
import com.jjinbbang.server.admin.moderation.entity.Report;
import com.jjinbbang.server.global.persistence.CreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "action_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionHistory extends CreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "prohibited_word_flag_id")
	private ProhibitedWordFlag prohibitedWordFlag;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "report_id")
	private Report report;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "admin_id", nullable = false)
	private Admin admin;

	@Column(nullable = false, length = 50)
	private String reason;

	@Lob
	@Column(name = "detail_reason", columnDefinition = "TEXT")
	private String detailReason;
}
