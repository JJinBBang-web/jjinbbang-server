# jjinbbang-server

찐빵(JJinBBang) **V2 백엔드**. 서비스 API와 어드민 API를 한 저장소에서 관리한다.

- Spring Boot 4.1.0 / Gradle 9.5.1 / Java 21
- 루트 패키지 `com.jjinbbang.server`

## 실행

```bash
DB_URL='jdbc:mysql://localhost:3306/jjinbbang?serverTimezone=UTC&characterEncoding=UTF-8' \
DB_USERNAME='jjinbbang' \
DB_PASSWORD='jjinbbang' \
./gradlew bootRun

./gradlew test
./gradlew build
```

## 데이터베이스와 Flyway

- MySQL 8.4 이상을 사용한다.
- 애플리케이션 시작 시 Flyway가 `src/main/resources/db/migration`의 미적용 SQL을 순서대로 실행한다.
- Hibernate는 `ddl-auto: validate`만 사용하며 테이블을 생성하거나 변경하지 않는다.
- 운영 DB 연결 정보는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경 변수로 주입한다.
- 이미 적용된 마이그레이션 파일은 수정하지 않고 다음 버전 파일을 추가한다.

Flyway 실행을 위한 별도 Docker Compose는 필요하지 않다. 로컬·CI 통합 테스트는
Testcontainers가 임시 MySQL을 생성하고, 홈랩에서는 배포된 MySQL을 데이터소스로 사용한다.

## 패키지 구조

```
com.jjinbbang.server
├── global/       횡단 관심사 (응답·예외)
├── domain/       서비스 도메인 (building, review, agency, map, user, content, common)
└── admin/        어드민 도메인 (administrator, verification, moderation, dashboard)
```

도메인 하나는 `controller · service · repository · entity · dto` 한 세트다.
의존 방향은 `admin` → `domain` 한 방향만 허용한다.

## Git 컨벤션

> **필수 규칙:** 모든 기여자는 브랜치·커밋 컨벤션과 Issue·PR 템플릿을
> 항상 준수해야 한다. 일부 항목을 생략하거나 다른 형식을 사용해야 한다면
> 작업 전에 사유와 대안을 공유하고 승인을 받는다.
>
> 자동화 에이전트의 저장소 작업 규칙은 루트 [`AGENTS.md`](AGENTS.md)를
> 유일한 진입점으로 사용한다.

### AI 도구별 규칙 참조

프로젝트 컨벤션의 단일 원본은 [`AGENTS.md`](AGENTS.md)다. Codex뿐 아니라
Claude Code와 Gemini CLI를 사용할 때도 작업 시작 전에 루트 `AGENTS.md`를
직접 읽고 따른다.

| 도구 | 참조 규칙 |
| --- | --- |
| Codex | 루트 [`AGENTS.md`](AGENTS.md)를 직접 사용 |
| Claude Code | 작업 시작 전에 루트 [`AGENTS.md`](AGENTS.md)를 직접 읽고 준수 |
| Gemini CLI | 작업 시작 전에 루트 [`AGENTS.md`](AGENTS.md)를 직접 읽고 준수 |

컨벤션을 변경할 때는 `AGENTS.md`를 먼저 수정하고, 사람이 확인할 README 요약도
같은 변경에서 갱신한다. 도구별 별도 규칙 파일은 만들지 않는다.

### 브랜치

기본 승격 흐름은 `작업 브랜치 → develop → main`이다. 운영 장애처럼 일반 승격
흐름을 기다릴 수 없는 경우가 아니라면 `main`에 직접 반영하지 않는다.

```text
<type>/<topic>-#<issue-number>
```

| Type | 용도 | 예시 |
| --- | --- | --- |
| `feat` | 기능 추가 | `feat/authentik-login-#12` |
| `fix` | 버그 수정 | `fix/review-status-#34` |
| `refactor` | 기능 변경 없는 구조 개선 | `refactor/entity-relation-#56` |
| `chore` | 빌드·설정·의존성 작업 | `chore/flyway-#78` |
| `test` | 테스트 추가·수정 | `test/schema-validation-#90` |
| `docs` | 문서 변경 | `docs/git-convention-#91` |
| `hotfix` | 승인된 운영 긴급 수정 | `hotfix/login-failure-#92` |

`init/<topic>`은 저장소 초기 구축 작업에서만 이슈 번호 없이 사용할 수 있다.

### 커밋

기존 찐빵 백엔드의 이슈 연결 방식과 이모지 스타일을 유지한다.

```text
<emoji> <Type>[#<issue-number>]: <한글 설명>
```

| Type | Emoji | 예시 |
| --- | --- | --- |
| `Feat` | ✨ | `✨ Feat[#12]: Authentik 로그인 엔드포인트 추가` |
| `Fix` | 🐛 | `🐛 Fix[#34]: 리뷰 상태 변경 오류 수정` |
| `Refactor` | ♻️ | `♻️ Refactor[#56]: 엔티티 연관관계 매핑 정리` |
| `Chore` | 🔧 | `🔧 Chore[#78]: Flyway 의존성 추가` |
| `Test` | ✅ | `✅ Test[#90]: MySQL 스키마 검증 테스트 추가` |
| `Docs` | 📝 | `📝 Docs[#91]: Git 컨벤션 문서화` |
| `Style` | 🎨 | `🎨 Style[#93]: 코드 포맷 정리` |

- 제목은 무엇을 바꿨는지 한 문장으로 작성한다.
- 하나의 커밋에는 하나의 논리적 변경만 담는다.
- 이슈가 없는 저장소 초기화 작업은 `[#번호]`를 생략할 수 있다.
- merge commit과 자동 생성 커밋에는 이 형식을 강제하지 않는다.

### 이슈와 PR

- 기능과 버그는 작업 전에 GitHub Issue를 생성하고 브랜치·커밋·PR에 번호를 연결한다.
- PR 대상은 일반적으로 `develop`이며, `main` 승격은 별도 PR로 진행한다.
- PR에는 변경 내용, 리뷰 포인트, 테스트 결과와 Flyway 영향 여부를 작성한다.
- 템플릿은 [`.github/ISSUE_TEMPLATE`](.github/ISSUE_TEMPLATE)과
  [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md)를 사용한다.
- 커밋·푸시·머지처럼 Git 이력이나 원격 상태를 변경하는 작업은 항상 사용자의
  사전 검토와 명시적 승인을 받은 뒤에만 실행한다.
- 커밋 승인과 푸시 승인은 별개다. 커밋 후 해시와 검증 결과를 다시 공유하고
  별도의 푸시 승인을 받는다.
- 구현·QA·QC·작업 계속 요청은 커밋 또는 푸시 승인으로 간주하지 않는다.

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

컨벤션 전체와 자동화 에이전트 작업 규칙은 [`AGENTS.md`](AGENTS.md)를 본다.

## 현재 상태

`global` 응답·예외 처리, 전체 초기 엔티티, MySQL datasource, Flyway V1이 구성되어 있다.
Testcontainers 통합 테스트에서 Flyway 마이그레이션과 Hibernate 스키마 검증을 함께 수행한다.
