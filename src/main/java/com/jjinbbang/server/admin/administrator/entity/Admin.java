package com.jjinbbang.server.admin.administrator.entity;

import java.time.LocalDateTime;

import com.jjinbbang.server.admin.administrator.type.AdminStatus;
import com.jjinbbang.server.global.persistence.UpdatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "admins",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_admins_oidc_identity",
		columnNames = {"oidc_issuer", "oidc_subject"}
	)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin extends UpdatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "oidc_issuer", nullable = false)
	private String oidcIssuer;

	@Column(name = "oidc_subject", nullable = false)
	private String oidcSubject;

	private String email;

	@Column(length = 100)
	private String username;

	@Column(name = "display_name", length = 100)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AdminStatus status;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;
}
