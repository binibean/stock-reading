---
name: create-migration
description: Flyway 마이그레이션 SQL 파일을 프로젝트 규칙에 맞게 만들고 검증한다. 테이블 생성·컬럼 추가 등 스키마를 바꿀 때 사용.
disable-model-invocation: true
allowed-tools: Read, Write, Bash, Grep, Glob
---

# 마이그레이션 만들기

스키마는 Flyway 가 유일한 주인이다. JPA 는 `ddl-auto: validate` 라 엔티티와 실제
테이블이 어긋나면 **서버가 아예 뜨지 않는다.** 엔티티를 고쳤으면 반드시 짝이 되는
마이그레이션을 같은 커밋에 넣는다.

## 0. 이 스킬의 진행 방식

파일을 만들기 전에 **무엇을 왜 그렇게 쓰는지 먼저 설명하고 승인을 받는다.**
스키마 설계는 이 프로젝트의 핵심 학습 대상이라 대신 처리하지 않는다.
검증 명령은 사용자가 직접 실행한다.

## 1. 다음 버전 번호 확인

```bash
ls src/main/resources/db/migration/
```

가장 큰 `V<n>` 다음 번호를 쓴다. 번호를 건너뛰어도 되지만 **중복은 안 된다.**

## 2. 파일 생성

```
src/main/resources/db/migration/V<번호>__<설명>.sql
```

- 언더바 **2개** (`V1__`). 1개면 Flyway 가 인식하지 못한다
- 설명은 snake_case, 무엇을 하는지 드러나게: `V1__create_glossary_term.sql`,
  `V3__add_summary_to_news.sql`
- `classpath:db/migration` 이 기본 위치라 경로만 맞추면 별도 설정은 필요 없다

IntelliJ 에서는 `resources` 우클릭 → New → File → 전체 경로를 그대로 입력하면
중간 폴더까지 만들어진다. `New → Directory` 에 점(`db.migration`)을 쓰면 안 된다.

## 3. 지켜야 할 규칙

- **이미 적용된 파일은 절대 수정하지 않는다.** Flyway 가 체크섬을
  `flyway_schema_history` 에 저장해 두어, 고치면 다음 기동 때 checksum mismatch 로
  막힌다. 바꾸려면 새 버전 파일을 추가한다
- **하나의 파일 = 하나의 논리적 변경.** 되돌리거나 이력을 읽을 때를 위해
- 되돌리는 스크립트는 두지 않는다. 앞으로만 간다

## 4. 검증 — 사용자가 직접 실행한다

```bash
docker compose up -d
./gradlew bootRun
```

기동에 성공하면 (= JPA validate 통과) DB 에서 확인한다:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

방금 만든 버전이 `success = true` 로 있으면 끝이다.

기동이 실패하면 로그의 `Schema-validation:` 줄을 본다. 엔티티와 SQL 중
어느 쪽이 틀렸는지 알려준다. **답을 먼저 주지 말고 어디를 볼지부터 안내한다.**

## 5. 테이블 컨벤션

첫 실제 테이블은 이슈 #2 (`glossary_term`) 에서 만든다. 거기서 정한 규칙
(PK 타입 · 타임스탬프 컬럼 · 네이밍) 을 이 절에 적어 두고, 이후 모든
마이그레이션이 따른다.
