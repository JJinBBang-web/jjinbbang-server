package com.jjinbbang.server.domain.user.repository;

import java.util.Optional;

import com.jjinbbang.server.domain.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

	/** 같은 사용자의 서로 다른 증명서가 동시에 처리되지 않도록 사용자 행을 잠근다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM User u WHERE u.id = :userId")
	Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}
