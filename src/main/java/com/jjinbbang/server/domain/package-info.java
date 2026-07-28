/**
 * 서비스 도메인. 2026-05-28 확정 기준으로 {@code building}, {@code review}, {@code agency},
 * {@code map}, {@code user}, {@code content}, {@code common}이 여기 들어온다.
 *
 * <p>도메인 하나는 필요한 범위에서
 * {@code controller · service · repository · dto · entity · type · id · converter}로 구성한다.
 * {@code entity}에는 JPA 엔티티, {@code type}에는 도메인 enum,
 * {@code id}에는 복합키, {@code converter}에는 복잡한 JPA 변환기만 둔다.
 *
 * <p><b>의존 방향</b>: {@code admin} → {@code domain} 한 방향만 허용한다.
 * 이 패키지가 {@code admin}을 참조하면 안 된다.
 */
package com.jjinbbang.server.domain;
