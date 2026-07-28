package com.jjinbbang.server.global.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class UpdatedAtEntity extends CreatedAtEntity {

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void initializeUpdatedAt() {
		if (updatedAt == null) {
			updatedAt = LocalDateTime.now();
		}
	}

	@PreUpdate
	protected void updateTimestamp() {
		updatedAt = LocalDateTime.now();
	}
}
