/**
 * 서비스 도메인. 2026-05-28 확정 기준으로 {@code building}, {@code review}, {@code agency},
 * {@code map}, {@code user}, {@code content}, {@code common}이 여기 들어온다.
 *
 * <p>도메인 하나는 {@code controller · service · repository · entity · dto} 한 세트로 구성한다.
 * 최상위 블록이 {@code domain}이므로 엔티티 폴더는 Ver.1처럼 {@code entity}로 둔다
 * ({@code domain/review/domain}이 되는 것을 피한다).
 *
 * <p><b>의존 방향</b>: {@code admin} → {@code domain} 한 방향만 허용한다.
 * 이 패키지가 {@code admin}을 참조하면 안 된다.
 */
package com.jjinbbang.server.domain;
