package com.jjinbbang.server.global.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class SoftDeleteEntity extends UpdatedAtEntity {

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;
}
