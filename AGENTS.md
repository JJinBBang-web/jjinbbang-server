# JJinBBang Server 작업 규칙

이 파일은 사람과 자동화 에이전트가 함께 지켜야 하는 저장소의 유일한 작업 규칙
진입점이다.

## 규칙 원본과 도구 연동

- 프로젝트 컨벤션과 작업 규칙의 단일 원본은 루트 `AGENTS.md`다.
- `README.md`는 사람이 빠르게 확인할 수 있는 요약이며, 내용이 다르면
  `AGENTS.md`를 우선한다.
- Codex는 루트 `AGENTS.md`를 직접 사용한다.
- Claude Code와 Gemini CLI를 사용할 때도 별도 규칙 파일을 만들지 않고,
  작업을 시작하기 전에 루트 `AGENTS.md`를 직접 읽고 따른다.
- 브랜치, 커밋, Issue, PR, 아키텍처, Flyway, 테스트 컨벤션을 변경할 때는
  `AGENTS.md`를 먼저 수정하고 필요한 README 요약을 같은 변경에서 갱신한다.

## 최우선 규칙

- 모든 작업에서 `README.md`의 브랜치·커밋 컨벤션을 항상 준수한다.
- 모든 Issue와 PR은 `.github`의 해당 템플릿을 사용하고 요구 항목을 빠짐없이 작성한다.
- 컨벤션이나 템플릿을 지킬 수 없는 예외 상황에서는 임의로 생략하지 않고,
  작업 전에 사유와 대안을 보고한 뒤 승인을 받는다.
- 이 규칙은 사람과 자동화 에이전트 모두에게 동일하게 적용된다.

## 작업 시작

- 기본 브랜치는 `develop`, 운영 브랜치는 `main`이다.
- 작업 전 현재 브랜치와 dirty worktree를 확인하고 기존 변경을 보존한다.
- 읽기 전용 Git 명령은 필요한 범위에서 사용할 수 있다.
- Git 이력이나 원격 상태를 변경하는 작업은 아래 승인 게이트를 반드시 따른다.

## Git 변경 승인 게이트

- `git commit`, `git push`, `git merge`, `git rebase`, `git tag`, force push,
  release 생성처럼 Git 이력이나 원격 상태를 변경하는 작업은 항상 사용자의
  사전 검토와 명시적 승인을 받은 뒤에만 실행한다.
- 구현 요청, QA·QC 요청, 작업 계속 요청을 Git 변경 승인으로 해석하지 않는다.
  사용자가 승인 대상 작업을 명확하게 확인한 경우에만 승인으로 인정한다.
- 커밋 전에는 변경 범위, 검증 결과, 커밋 분리 방식, 커밋 메시지 후보,
  대상 브랜치, 리스크와 롤백 방법을 포함한 결재안을 먼저 공유한다.
- 커밋 승인은 푸시 승인을 포함하지 않는다. 커밋 후에는 커밋 해시와 최종 검증
  결과, 푸시 대상 브랜치를 다시 공유하고 별도의 명시적 푸시 승인을 받는다.
- 승인받은 이후 변경 범위, 커밋 수, 메시지 또는 대상 브랜치가 달라지면 작업을
  중단하고 변경된 결재안으로 다시 승인받는다.
- merge, rebase, tag, force push처럼 영향이 다른 작업은 정확한 명령과 대상을
  별도로 설명하고 각각 승인받는다.

## 브랜치

```text
<type>/<topic>-<issue-number>
```

- 허용 type: `feat`, `fix`, `refactor`, `chore`, `test`, `docs`, `hotfix`
- 예시: `feat/authentik-login-12`
- 저장소 초기 구축만 `init/<topic>`을 허용한다.
- 기본 승격 흐름: `작업 브랜치 → develop → main`
- `main` 직접 hotfix는 일반 승격으로 대응할 수 없는 운영 장애에만 제안하고
  사유를 설명한 뒤 승인받는다.

## 커밋

```text
<emoji> <Type>[#<issue-number>]: <한글 설명>
```

- `✨ Feat`
- `🐛 Fix`
- `♻️ Refactor`
- `🔧 Chore`
- `✅ Test`
- `📝 Docs`
- `🎨 Style`

커밋은 하나의 논리적 변경 단위로 나눈다. 이슈 없는 저장소 초기 구축은
`[#번호]`를 생략할 수 있다.

## 아키텍처

- 루트 패키지는 `com.jjinbbang.server`다.
- `global`은 횡단 관심사, `domain`은 서비스 도메인, `admin`은 관리자
  도메인이다.
- 의존 방향은 `admin → domain`만 허용하며 `domain`은 `admin`을 참조하지 않는다.
- 도메인 내부는 필요한 범위에서 `controller`, `service`, `repository`, `dto`,
  `entity`, `type`, `id`, `converter`로 구성한다.
- `entity`에는 `@Entity`, `type`에는 도메인 enum, `id`에는 `@Embeddable`
  복합키, `converter`에는 `AttributeConverter`만 둔다. 사용하지 않는 빈
  패키지는 만들지 않는다.
- 관리자 웹은 이 서버의 관리자 API를 직접 호출한다.

## JPA와 Flyway

- Flyway가 스키마의 유일한 변경 주체다.
- Hibernate는 `ddl-auto: validate`만 사용한다.
- 적용된 migration 파일은 수정하지 않고 다음 버전 파일을 추가한다.
- DB의 단일 문자열과 enum 상수를 매핑할 때는 Jakarta Persistence 3.2의
  `@EnumeratedValue`를 우선한다. 복잡한 영속성 변환에만 `AttributeConverter`를
  사용하며 DB 값 변환을 서비스나 DTO 계층으로 넘기지 않는다.
- API 요청·응답 DTO와 도메인 타입의 변환은 DTO mapper 또는 팩토리가 담당하며,
  JPA 영속성 매핑과 혼합하지 않는다.
- 엔티티 연관관계는 기본적으로 LAZY 단방향으로 매핑하고, 실제 탐색 요구가
  확인된 경우에만 양방향을 추가한다.
- datasource 비밀번호와 홈랩 접속 정보는 환경 변수로 주입하며 저장소에
  커밋하지 않는다.

## 테스트와 검증

- 기본 검증 명령은 `./gradlew test`와 `./gradlew build`다.
- DB 스키마 변경은 Testcontainers MySQL에서 Flyway 적용과 Hibernate
  validation을 모두 확인한다.
- 테스트나 명령으로 확인한 결과와 코드만 보고 추론한 내용을 구분해서 보고한다.
- 실제 GitHub Actions를 실행하지 않았다면 CI 통과를 단정하지 않는다.

## 이슈와 PR

- 기능·버그 작업은 Issue를 만들고 브랜치, 커밋, PR에 번호를 연결한다.
- PR 대상은 일반적으로 `develop`이다.
- PR 템플릿의 리뷰 포인트, 테스트 결과, DB migration 항목을 빠짐없이 작성한다.
- 스크린샷이 필요 없는 백엔드 변경은 해당 항목에 `해당 없음`을 명시한다.
