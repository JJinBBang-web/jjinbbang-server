/**
 * 어드민 도메인. {@code administrator}, {@code verification}, {@code moderation}, {@code dashboard}.
 *
 * <p>어드민 웹은 {@code jjinbbang-server-admin}(Authentik 로그인 · BFF)을 거쳐 이 패키지의 API를 호출한다.
 * 권한 재검증과 감사 기록({@code action_history})은 BFF가 아니라 <b>이 서버의 책임</b>이다.
 *
 * <p>구성은 {@code domain}과 같다 — {@code controller · service · repository · entity · dto}.
 * 어드민 전용 테이블({@code admins}, {@code prohibited_words}, {@code action_history} 등)의 엔티티가 여기 들어온다.
 *
 * <p><b>경계 규칙</b>
 * <ul>
 *   <li>조회는 {@code domain}의 리포지토리를 직접 써도 된다 — 어드민 목록은 삭제된 것까지 보여야 하고 필터가 많다.</li>
 *   <li>상태를 바꾸는 조작은 {@code domain}의 서비스 계층을 거친다 — 집계 갱신·감사 기록이 빠지지 않게.</li>
 * </ul>
 *
 * <p>2026-07-26 결정 기준 <b>인증·권한은 아직 붙이지 않는다.</b> SSO(Authentik) 연동 시점에 추가한다.
 */
package com.jjinbbang.server.admin;
