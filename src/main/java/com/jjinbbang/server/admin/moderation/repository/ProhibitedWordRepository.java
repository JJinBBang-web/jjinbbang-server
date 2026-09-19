package com.jjinbbang.server.admin.moderation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jjinbbang.server.admin.moderation.entity.ProhibitedWord;

/**
 * {@code prohibited_words}는 소프트 삭제 테이블이라 조회 메서드마다 {@code deleted_at IS NULL}이 붙는다.
 *
 * <p>{@code JpaRepository}가 물려주는 {@code findById}·{@code findAll}은 삭제된 행까지 가져오므로
 * 서비스에서 쓰지 않는다.
 */
public interface ProhibitedWordRepository extends JpaRepository<ProhibitedWord, Long> {

	Optional<ProhibitedWord> findByIdAndDeletedAtIsNull(Long id);

	/** {@code word}에 UNIQUE 제약이 없어 중복 검사를 조회로 한다. */
	boolean existsByWordAndDeletedAtIsNull(String word);

	Page<ProhibitedWord> findAllByDeletedAtIsNull(Pageable pageable);

	Page<ProhibitedWord> findAllByEnabledAndDeletedAtIsNull(Boolean enabled, Pageable pageable);

	/** 리뷰 본문 마스킹은 활성 금칙어만 페이징 없이 한 번에 대조한다. */
	List<ProhibitedWord> findAllByEnabledTrueAndDeletedAtIsNull();
}
