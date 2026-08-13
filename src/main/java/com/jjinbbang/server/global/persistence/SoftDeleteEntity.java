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

	/**
	 * 소프트 삭제. 행을 지우지 않고 {@code deleted_at}만 채운다.
	 *
	 * <p>이미 삭제된 것을 다시 지워도 최초 삭제 시각을 덮어쓰지 않는다 — 감사 기록이 뒤로 밀리면 안 된다.
	 * 조회 쪽에서 삭제된 행을 걸러내는 것은 각 리포지토리의 몫이다.
	 */
	public void delete() {
		if (deletedAt == null) {
			deletedAt = LocalDateTime.now();
		}
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}
}
