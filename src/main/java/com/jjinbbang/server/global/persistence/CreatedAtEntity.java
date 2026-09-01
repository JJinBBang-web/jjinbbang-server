package com.jjinbbang.server.global.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class CreatedAtEntity {

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void initializeCreatedAt() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
