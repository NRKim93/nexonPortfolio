# Agent Working Standard

이 문서는 Codex/AI Agent가 이 프로젝트에서 작업할 때 따라야 하는 단일 기준 문서다.

다른 agent 문서를 추가로 열어보지 않아도 되도록 `agent/agents.md`의 핵심 원칙과 `agent/newAgents.md`의 프로젝트 기준을 통합했다. 작업자는 이 문서를 우선 기준으로 삼고, 실제 구현 판단은 항상 현재 코드, `README.md`, Notion API 사양 순서로 검증한다.

가장 중요한 원칙은 다음이다.

```text
구현보다 설명 가능한 구조를 우선한다.
```

---

## 1. 기준 우선순위

작업 판단 기준은 아래 순서를 따른다.

1. 현재 코드
2. `README.md`
3. 이 문서
4. Notion API 사양과 ERRORCODE
5. 사용자 최신 요청

단, 사용자 최신 요청이 기존 문서와 충돌하면 사용자 요청을 우선하되, 충돌 내용을 최종 응답에 명확히 남긴다.

Notion 기준 문서는 다음이다.

```text
https://www.notion.so/API-32d1f4df93178066bb84fe71b9139d60
```

Notion에서 확인할 대상은 다음이다.

- 프로젝트 개요
- ER 관계도
- 시퀀스 다이어그램
- API 사양서 데이터베이스
- ERRORCODE 데이터베이스

인증/인가와 테이블 구조는 2026-05-24에 정리된 Notion ERD의 `api_clients` 기반 구조를 우선한다. README나 과거 문서에 남아 있는 OAuth2/JWT 중심 표현은 구버전 문구일 수 있으므로 그대로 구현하지 않는다.

---

## 2. 프로젝트 목표

이 프로젝트는 게임 서비스에서 반복적으로 필요한 인증, 계정, 빌링, 운영 기능을 공통 API로 분리한 Spring Boot 기반 백엔드 서버다.

핵심 목표는 다음이다.

- 외부 게임 서비스 또는 연동 시스템을 API Client로 등록한다.
- Client ID + API Key 기반으로 호출 주체를 식별한다.
- 인증/인가는 API Client 검증 결과와 Client Role을 기준으로 처리한다.
- 일반 API 권한과 관리자 권한을 역할 기반으로 분리한다.
- 계정 상태 관리, 상품 조회, 구매, 청약철회, 감사 로그 조회를 공통 API로 제공한다.
- API 응답, 예외, 에러코드, 인증 흐름을 일관된 구조로 유지한다.

---

## 3. 기술 기준

현재 프로젝트의 기본 기술 기준은 다음이다.

- Java 21 LTS
- Spring Boot
- Spring Data JPA
- Spring Security
- MySQL
- Redis
- OpenAPI / Swagger UI
- Docker Compose
- GitHub Actions
- AWS EC2 / RDS

사용자가 명시적으로 요청하기 전까지 아래 항목은 선행 구현하지 않는다.

- Kafka
- ELK 로그 수집
- 공지 관리
- 이벤트 관리
- 결제 PG 실제 연동
- OAuth2 client 기반 로그인
- OAuth 제공자별 실제 외부 API 연동
- OAuth 로그인 화면
- 프론트엔드

---

## 4. 작업 시작 절차

작업을 시작하기 전에 반드시 아래 순서로 확인한다.

1. 요청 범위가 어느 도메인인지 파악한다.
2. `README.md`에서 현재 진행 상태와 실행 방법을 확인한다.
3. 관련 패키지, 테스트, 공통 응답, 예외 구조를 확인한다.
4. API URL, Method, Request, Response, ErrorCode가 필요하면 Notion 사양을 확인한다.
5. 변경 범위를 최소 단위로 쪼갠다.
6. 구현 후 가능한 테스트 또는 빌드를 실행한다.
7. 문서 갱신 필요 여부를 판단한다.

작업 중 확실하지 않은 내용이 있으면 추측으로 API나 테이블을 만들지 않는다. 현재 코드와 문서에서 근거를 찾고, 그래도 없으면 사용자에게 확인한다.

---

## 5. 구현 원칙

기본 구현 원칙은 다음이다.

- 한 번에 너무 많은 기능을 구현하지 않는다.
- 현재 요청 범위 밖의 기능을 임의로 추가하지 않는다.
- 공통 구조를 먼저 만들고 도메인 기능을 순차적으로 구현한다.
- 기존 패키지 구조와 코드 스타일을 우선 존중한다.
- Controller, Service, Repository, DTO, Entity의 역할을 명확히 분리한다.
- 포트폴리오 프로젝트이므로 코드 의도가 드러나는 이름을 사용한다.
- 설명 가능한 단순한 구조를 과도한 추상화보다 우선한다.
- 사용하지 않는 공통 클래스나 미래 대비용 구조를 먼저 만들지 않는다.
- 작업 후 변경 파일, 변경 이유, 테스트 여부를 요약한다.

Controller에는 요청 매핑, 요청 검증 진입점, 응답 반환만 둔다. 비즈니스 로직은 Service로 보낸다.

Service에는 비즈니스 규칙, 상태 검증, 예외 발생, 트랜잭션 경계를 둔다.

Repository는 Spring Data JPA Repository를 우선 사용한다. 단순 조회는 query method를 우선하고, 직접 쿼리는 필요한 이유가 있을 때만 사용한다.

DTO는 API 입출력 형식을 담당한다. Entity를 API 응답으로 직접 반환하지 않는다. DTO에 도메인 상태 변경 로직이나 비즈니스 로직을 넣지 않는다.

Entity는 테이블 매핑과 도메인 상태를 표현한다. Entity 전체에 `@Data`, `@Setter`를 붙이지 않는다. 상태 변경은 `changeStatus()`, `changeUsername()`, `requestRefund()`처럼 의도가 드러나는 메서드로 처리한다.

---

## 6. 패키지 기준

현재 base package는 다음이다.

```text
com.portfolio.nexon
```

기존 구현에서 우선 확인할 공통 구조는 다음이다.

- `global.common.response.CommonResponse`
- `global.common.error.ErrorCode`
- `global.exception.BusinessException`
- `global.exception.GlobalExceptionHandler`
- `global.security.config.SecurityConfig`
- `global.security.jwt.*`
- `global.security.handler.*`

JWT 관련 구현이 존재하더라도 현재 인증 기준으로 확정하지 않는다. 기본 인증 방향은 Client ID + API Key 구조이며, 기존 JWT 코드는 전환 또는 정리 대상일 수 있다.

---

## 7. 도메인 기준

Notion ERD 기준 주요 테이블은 다음이다.

- `api_clients`: API 호출 주체를 저장한다. Client ID / API Key 기반 인증의 기준 테이블이다.
- `accounts`: API Client에 속한 서비스 사용자 계정을 저장한다.
- `admin_credentials`: 관리자 로그인 정보 저장 후보 테이블이다.
- `products`: 캐시 아이템 또는 상품 정보를 저장한다.
- `purchases`: 사용자 상품 구매 이력을 저장한다.
- `audit_logs`: 관리자 작업과 주요 운영 이벤트를 기록한다.

`oauth_accounts` 중심 구조는 현재 우선 구현 대상이 아니다. OAuth 제공자 연동, OAuth 계정 매핑, OAuth 로그인 플로우는 사용자 요청 전까지 구현하지 않는다.

### API Client

- `client_id`는 외부 게임 서비스 또는 연동 시스템을 식별하는 고유 값이다.
- `api_key_hash`에는 API Key 원문이 아니라 해시 값을 저장한다.
- API Key 원문은 최초 발급 또는 재발급 응답에서만 반환한다.
- DB에는 API Key 원문을 저장하지 않는다.
- Redis는 Client 인증 검증 결과 캐시로 사용할 수 있다.
- DB는 Redis 캐시 미스 시 조회하는 원본 저장소다.

API Client 상태 기준은 다음이다.

- `ACTIVE`
- `DISABLED`
- `DELETED`

### Account

- `api_client_id`는 계정이 어느 API Client에 속하는지 나타낸다.
- `external_user_id`는 API Client 서비스에서 사용하는 사용자 식별 ID다.
- `api_client_id`와 `external_user_id` 조합은 유니크하게 관리한다.
- 같은 `external_user_id`라도 API Client가 다르면 서로 다른 사용자로 처리할 수 있다.
- `username`은 사용자 닉네임 또는 표시명 성격의 값이다.
- `email`은 선택 값으로 본다.

계정 상태 기준은 다음이다.

- `ACTIVE`
- `DORMANT`
- `SUSPENDED`
- `DELETED`

사용자 등급 기준은 다음이다.

- `NORMAL`
- `PREMIUM`
- `VIP`

### Product

상품 타입 기준은 다음이다.

- `SINGLE`
- `PACKAGE`

상품 상태 기준은 다음이다.

- `ON_SALE`
- `STOPPED`

### Purchase

구매 상태 기준은 다음이다.

- `PENDING`
- `PAID`
- `FAILED`
- `CANCELED`
- `REFUND_REQUESTED`
- `REFUNDED`

### Audit Log

감사 로그는 강한 FK보다 `actor_type/actor_id`, `target_type/target_id` 조합을 사용하는 유연한 로그 구조를 기본으로 본다.

actor type 기준은 다음이다.

- `ACCOUNT`
- `ADMIN`
- `SYSTEM`
- `CLIENT`

target type 기준은 다음이다.

- `ACCOUNT`
- `PRODUCT`
- `PURCHASE`
- `REPORT`
- `SANCTION`
- `CLIENT`

log type 기준은 다음이다.

- `ACCOUNT`
- `BILLING`
- `SECURITY`
- `REPORT`
- `SANCTION`
- `CLIENT`

result type 기준은 다음이다.

- `SUCCESS`
- `FAIL`
- `DETECTED`
- `REQUESTED`
- `PROCESSED`

action type 기준은 다음이다.

- `LOGIN`
- `LOGOUT`
- `STATUS_CHANGED`
- `PURCHASE_CREATED`
- `PAYMENT_SUCCESS`
- `PAYMENT_FAIL`
- `REFUND_REQUESTED`
- `REFUND_COMPLETED`
- `ABNORMAL_DETECTED`
- `REPORT_CREATED`
- `SANCTION_REQUESTED`
- `SANCTION_APPROVED`
- `SANCTION_REJECTED`
- `CLIENT_CREATED`
- `CLIENT_STATUS_CHANGED`
- `API_KEY_ROTATED`

---

## 8. API 기준

API URL은 `/api/v1` 기준 REST 구조를 따른다.

README와 Notion API 사양을 기준으로 우선 고려할 API는 다음이다.

| 기능 | Method | URL |
| --- | --- | --- |
| API Key 토큰 발급 | TBD | Notion API 사양 확인 필요 |
| API Client 등록 | TBD | Notion API 사양 확인 필요 |
| API Client 조회 | TBD | Notion API 사양 확인 필요 |
| API Client 상태 변경 | TBD | Notion API 사양 확인 필요 |
| API Key 재발급 | TBD | Notion API 사양 확인 필요 |
| 계정 조회 | GET | `/api/v1/admin/users/{userId}` |
| 닉네임 수정 | PATCH | `/api/v1/users/{userId}/username` |
| 상품 조회 | GET | `/api/v1/products` |
| 구매 요청 | POST | `/api/v1/purchases` |
| 구매 이력 조회 | GET | `/api/v1/purchases` |
| 청약철회 요청 | POST | `/api/v1/purchases/{purchaseId}/refund-requests` |
| 계정 상태 변경 | PATCH | `/api/v1/admin/users/{userId}/status` |
| 관리자 결제 이력 조회 | GET | `/api/v1/admin/purchases` |
| 감사 로그 조회 | GET | `/api/v1/admin/audit-logs` |

API를 구현하거나 수정할 때는 Notion API 사양서 데이터베이스를 확인한다. 사양이 `TBD`인 API는 임의로 URL과 Method를 확정하지 않는다.

---

## 9. 응답과 예외

공통 응답과 예외 처리 기준은 다음이다.

- 성공 응답은 `CommonResponse`를 사용한다.
- 에러 코드는 `ErrorCode` enum에서 관리한다.
- 비즈니스 예외는 `BusinessException`으로 처리한다.
- 예외 응답은 `GlobalExceptionHandler`에서 일괄 처리한다.
- API마다 별도 응답 포맷을 임의로 만들지 않는다.
- Notion ERRORCODE 데이터베이스를 에러코드 원본 기준으로 확인한다.

인증 실패와 권한 부족은 서로 다른 에러로 처리한다.

---

## 10. 보안 기준

보안 기준은 다음이다.

- USER와 ADMIN 권한을 명확히 분리한다.
- 관리자 API 접근은 우선 Client Role이 ADMIN인지 확인하는 방식으로 처리한다.
- Client ID / API Key 인증 실패와 권한 부족은 구분한다.
- API Key, DB Password 등 민감 정보는 코드에 직접 작성하지 않는다.
- 민감 설정은 환경 변수 이름 또는 예시 값만 문서화한다.
- Client ID / API Key가 필요한 API는 Header 기반 전달을 우선 고려한다.
- 예시 Header는 `X-Client-Id`, `X-Api-Key`를 사용한다.
- API Key 원문은 DB에 저장하지 않는다.
- API Key 비교는 요청 API Key를 동일한 방식으로 해시한 뒤 `api_key_hash`와 비교한다.

---

## 11. 주요 흐름 기준

### Client ID / API Key 인증

기본 흐름은 다음이다.

1. 클라이언트가 `X-Client-Id`, `X-Api-Key` Header를 전달한다.
2. 서버는 Client ID로 API Client를 조회한다.
3. 서버는 API Key를 검증한다.
4. 검증된 Client의 Role을 인증/인가 기준으로 사용한다.
5. Client 상태가 `ACTIVE`가 아니면 인증 실패로 처리한다.
6. Redis 캐시가 있으면 우선 사용하고, 캐시 미스 시 DB를 조회한다.
7. 인증 실패와 권한 부족은 서로 다른 에러로 반환한다.

### 관리자 로그인

`admin_credentials`는 ERD에 존재하지만 별도 관리자 로그인 API가 반드시 필요한지는 구현 전 확인한다. 현재 우선 접근 방식은 Client Role 기반 관리자 권한 검증이다.

### 상품 구매

기본 흐름은 다음이다.

1. 판매 중인 상품 목록을 조회한다.
2. 구매 요청 시 상품 존재 여부와 판매 상태를 검증한다.
3. 판매 중이면 구매 이력을 생성한다.
4. 판매 중이 아니면 구매 실패 응답을 반환한다.

### 청약철회

기본 흐름은 다음이다.

1. 구매 이력 존재 여부를 확인한다.
2. 이미 철회된 구매인지 확인한다.
3. 청약철회 가능 기간과 상품 사용 여부를 확인한다.
4. 가능하면 구매 상태를 청약철회 요청 상태로 변경한다.

### 계정 상태 변경

기본 흐름은 다음이다.

1. 요청자가 ADMIN 권한인지 확인한다.
2. 대상 계정 존재 여부를 확인한다.
3. 상태 변경 가능 여부를 검증한다.
4. 상태를 변경한다.
5. 감사 로그를 기록한다.

---

## 12. 테스트 기준

테스트는 변경 위험도에 맞춰 작성한다.

우선 검증 대상은 다음이다.

- `CommonResponse` 응답 포맷
- `ErrorCode` 매핑
- `BusinessException` 에러코드 보존
- `GlobalExceptionHandler` 응답 포맷
- Client ID / API Key 인증 실패와 권한 부족 처리
- Client Role 기반 관리자 API 접근 제어
- 계정 상태 변경 권한과 상태 전이
- 구매 요청 상품 상태 검증
- 청약철회 가능 여부 검증

계층별 기준은 다음이다.

- Controller: HTTP Method, URL, Request Body, Response Body, Status Code 검증
- Service: 정상 흐름, 예외 흐름, 상태 변경 결과 검증
- Repository: 복잡한 조회 조건이나 직접 쿼리가 있을 때 검증

가능한 검증 명령은 다음이다.

```bash
./gradlew test
./gradlew clean build
./gradlew clean build -x test
```

테스트를 실행하지 못한 경우 최종 응답에 이유를 명확히 남긴다.

---

## 13. 문서 갱신 기준

아래 변경이 있으면 관련 문서 갱신을 검토한다.

- API URL, Method, Request, Response 변경
- 에러코드 추가 또는 변경
- Entity, 테이블 컬럼, 상태 값 변경
- 실행 방법 변경
- 환경 변수 변경
- Docker Compose 변경
- 구현 완료 상태 변경

갱신 후보 문서는 다음이다.

```text
README.md
agent/mergedAgents.md
docs/**
Notion API 사양
Notion ERRORCODE
```

Notion 수정은 사용자가 요청한 경우에만 수행한다. 로컬 문서와 Notion이 다르면 최종 기준이 무엇인지 명확히 보고한다.

---

## 14. 금지사항

명시 요청 전까지 아래 작업은 하지 않는다.

- 요청 범위 밖 기능 선행 구현
- Kafka 구현
- ELK 로그 수집 구현
- 공지 관리 구현
- 이벤트 관리 구현
- 결제 PG 실제 연동
- OAuth2 client 기반 로그인 구현
- OAuth 제공자별 실제 외부 API 연동
- OAuth 로그인 화면 구현
- 프론트엔드 구현
- 사용하지 않는 공통 클래스 선행 생성
- 과도한 추상화
- 테스트 삭제 또는 비활성화로 통과시키기
- 민감 정보가 담긴 파일 생성 또는 수정

코드 작성 시 아래 방식도 금지한다.

- Controller에 비즈니스 로직 작성
- Entity를 API 응답으로 직접 반환
- DTO에 도메인 로직 작성
- Entity 전체 setter 공개
- API마다 제각각인 응답 포맷 작성
- 현재 구현 단계보다 앞선 기능 선행 구현

민감 파일은 명시 요청 전까지 생성, 수정, 삭제하지 않는다.

```text
.env
.env.*
application-secret.yml
application-local-secret.yml
application-prod.yml
application-prod-*.yml
*.pem
*.key
*.p12
docker-compose.prod.yml
```

예시 파일은 실제 값이 아닌 sample, dummy, example 값만 사용한다.

```text
.env.example
application-secret.example.yml
```

---

## 15. Git 기준

Git 작업 기준은 다음이다.

- 사용자가 요청하지 않으면 직접 commit하지 않는다.
- 사용자가 요청하지 않으면 새 branch를 만들지 않는다.
- 기존 사용자 변경사항을 임의로 되돌리지 않는다.
- 작업 완료 후 `git status`로 변경 범위를 확인한다.
- 필요하면 `git diff`로 변경 내용을 확인한다.
- 최종 응답에는 변경 파일, 변경 이유, 테스트 여부를 요약한다.

커밋 메시지는 README의 규칙을 따른다.

예시는 다음이다.

```bash
git commit -m "feature : Client ID API Key 인증 구조 구현"
git commit -m "docs : Agent 작업 기준 문서 갱신"
```

---

## 16. 최종 응답 기준

작업 완료 응답에는 다음을 포함한다.

- 변경한 파일
- 왜 변경했는지
- 실행한 테스트 또는 실행하지 못한 이유
- 남은 주의사항

불필요한 장황한 설명은 피하고, 사용자가 다음 행동을 바로 판단할 수 있게 작성한다.
