# stock-reading

초보자를 위한 주식 학습 앱. 뉴스·공시·차트를 **읽는 법**을 알려준다.
종목 추천이나 시세 예측은 하지 않는다.

- 저장소: https://github.com/binibean/stock-reading
- Figma 프로토타입: https://www.figma.com/design/mt5iL3O3ugl0A3tP0W2dAb/

## 스택

| | |
|---|---|
| 언어 · 런타임 | Java 21 |
| 프레임워크 | Spring Boot 4.1.1 (Web MVC) |
| 빌드 | Gradle 9.7.1 |
| DB | PostgreSQL 16 + Flyway |
| 캐시 | Redis 7 |
| 문서 | springdoc-openapi 3.1.0 (Swagger UI) |
| 로컬 인프라 | Docker Compose |

> Spring Boot 4 는 starter 이름이 바뀌었다 (`spring-boot-starter-web` → `webmvc`).
> springdoc 도 **v3** 를 써야 한다. v2 는 Boot 3 전용.

## 패키지 구조 — 도메인 우선

```
com.stockreading
├── glossary/          용어사전
│   ├── domain/        엔티티 · 도메인 규칙
│   ├── application/   유스케이스 (Service)
│   ├── infra/         Repository · 외부 API 클라이언트
│   └── api/           Controller · DTO
├── guide/             설명 글
├── calendar/          일정
├── news/              뉴스           (MVP 2)
├── simulation/        과거 리플레이  (MVP 3)
└── common/            공통 응답 · 에러
```

**왜 계층이 아니라 도메인을 먼저 두는가**
기능 하나를 고칠 때 폴더 하나만 열면 된다. 도메인 경계가 코드 구조에 그대로 드러나고,
나중에 분리하기도 쉽다. 계층 우선은 도메인이 늘수록 각 폴더가 비대해진다.

## 공통 응답 — 성공·실패 모두 봉투

```json
// 성공
{ "success": true,  "data": { ... }, "error": null }

// 실패
{ "success": false, "data": null, "error": { "code": "NOT_FOUND", "message": "..." } }
```

클라이언트가 분기 없이 한 가지 방식으로 처리할 수 있다.
컨트롤러는 예외만 던지고, `GlobalExceptionHandler` 가 봉투로 변환한다.

## 브랜치 전략

```
main                배포된 상태 (보호: PR 필수 + CI 통과 필수)
  ↑ MVP 완료 시
develop             통합 브랜치
  ↑ 이슈 완료 시 PR
feature/<이슈번호>-<설명>     예) feature/12-calendar-event-entity
```

- 커밋: Conventional Commits — `feat` `fix` `chore` `docs` `test` `refactor`
- PR 본문에 **왜 그렇게 했는지**를 반드시 남긴다. `Closes #N` 으로 이슈를 연결한다
- 머지는 Squash merge

## 설계 원칙

1. **추천·예측하지 않는다.** "사세요", "오를 것" 같은 문구는 콘텐츠·LLM 응답 어디에도 넣지 않는다.
   이 제약은 대부분의 이슈에 인수 조건으로 들어가 있다.
2. **스키마는 Flyway 가 관리한다.** JPA 는 `ddl-auto: validate` 로 검증만 한다.
3. **뉴스 본문은 저장하지 않는다.** 제목 · 링크 · 요약만 다룬다.
4. **외부 API 는 배치로만 호출한다.** 조회 API 는 DB 만 읽는다.
5. **비밀값은 `.env` 에만.** 커밋하지 않는다. 항목은 `.env.example` 로 공유한다.
   GitHub 웹에서 파일을 만들면 `.gitignore` 가 적용되지 않으니 주의.
6. **시뮬레이션은 `as_of` 이후 데이터를 볼 수 없다.** (#42) 조회 계층에서 강제하고
   ArchUnit 으로 우회를 막는다.

## 로컬 실행

```bash
docker compose up -d      # PostgreSQL + Redis
./gradlew bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

`.env` 가 먼저 있어야 한다. `.env.example` 을 복사해 값을 채운다.
`DB_PASSWORD` 는 직접 정하는 값이고, `docker-compose.yml` 과 같은 값을 써야 한다.
나머지 키는 각 서비스에서 발급받는다 (DART · FRED · FMP).
