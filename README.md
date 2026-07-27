# jjinbbang-server

찐빵(JJinBBang) **V2 백엔드**. 서비스 API와 어드민 API를 한 저장소에서 관리한다.

- Spring Boot 4.1.0 / Gradle 9.5.1 / Java 21
- 루트 패키지 `com.jjinbbang.server`

## 실행

```bash
./gradlew bootRun        # 로컬 실행 (기본 프로파일: local)
./gradlew test           # 전체 테스트
./gradlew build          # 컴파일 + 테스트 + jar
./gradlew build -x test  # 테스트 제외 빌드
```

## 패키지 구조

```
com.jjinbbang.server
├── global/       횡단 관심사 (응답·예외)
├── domain/       서비스 도메인 (building, review, agency, map, user, content, common)
└── admin/        어드민 도메인 (administrator, verification, moderation, dashboard)
```

도메인 하나는 `controller · service · repository · entity · dto` 한 세트다.
의존 방향은 `admin` → `domain` 한 방향만 허용한다.

## 응답 규약

성공은 `ResTemplate<T>`로 감싼다.

```json
{ "code": 200, "message": "리뷰 불러오기 성공", "data": {} }
```

실패는 `GlobalExceptionHandler`가 한 곳에서 만든다.

```json
{ "code": 404, "errorCode": "REVIEW_NOT_FOUND", "message": "해당 리뷰 정보가 존재하지 않습니다." }
```

도메인은 예외 클래스를 만들지 않고 `ErrorCode` enum 파일 하나만 둔다.

```java
throw ReviewErrorCode.REVIEW_NOT_FOUND.exception();
```

컨벤션 전체는 [`.gemini/styleguide.md`](.gemini/styleguide.md), 작업 맥락은 [`CLAUDE.md`](CLAUDE.md)를 본다.

## 현재 상태

기초 세팅 단계다. `global`(응답·예외)과 패키지 골격만 있고 도메인 코드는 없다.
엔티티·Flyway·datasource·Docker는 다음 담당자가 붙인다 — `CLAUDE.md`의 "인수인계" 참조.
