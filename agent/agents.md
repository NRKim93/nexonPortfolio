# AGENTS.md

이 문서는 AI Agent 또는 Codex가 `게임 플랫폼 공통 서비스 API 서버` 프로젝트를 작업할 때 따라야 하는 작업 기준 문서입니다.

프로젝트의 상세 설명, 기술 스택, 인프라 구조, API 목록은 `README.md`를 기준으로 합니다.
이 문서에서는 중복 설명을 최소화하고, Agent가 실제 코드를 수정하거나 생성할 때 지켜야 할 규칙만 정리합니다.

---

## 1. Agent 작업 원칙

* 한 번에 너무 많은 기능을 구현하지 않는다.
* 현재 요청 범위 밖의 기능을 임의로 추가하지 않는다.
* 공통 구조를 먼저 만들고, 이후 도메인 기능을 순차적으로 구현한다.
* Controller, Service, Repository, DTO, Entity의 역할을 명확히 분리한다.
* 포트폴리오 프로젝트이므로 코드 의도가 드러나는 명확한 이름을 사용한다.
* 구현보다 설명 가능한 구조를 우선한다.
* 이미 존재하는 코드 스타일과 패키지 구조를 우선 존중한다.
* 작업 전 `README.md`와 현재 코드 구조를 먼저 확인한다.
* 작업 후에는 변경 파일, 변경 이유, 테스트 여부를 요약한다.

---

## 2. 기준 문서

작업 전 다음 문서를 우선 확인한다.

```text
README.md
AGENTS.md
```

`README.md`에는 다음 내용을 정리한다.

* 프로젝트 개요
* 기술 스택
* 인프라 구조
* 배포 플로우
* 주요 도메인
* API 목록
* 에러 코드
* 실행 방법
* 현재 진행 상태

`AGENTS.md`에서는 위 내용을 반복하지 않고, 실제 작업 기준만 관리한다.

---

## 3. 구현 우선순위

현재 구현은 아래 순서를 따른다.

1. 기본 패키지 구조 생성
2. build.gradle 의존성 정리
3. application.yml 기본 설정
4. 공통 응답 DTO 작성
5. 공통 에러코드 enum 작성
6. 공통 예외 작성
7. GlobalExceptionHandler 작성
8. Entity / Repository 작성
9. Security / API Key 인증 기본 구조 작성
10. Client ID / API Key 검증 구조 구현
11. 관리자 로그인 API 구현
12. 계정 조회 / 유저 닉네임 수정
13. 계정 상태 변경
14. 캐시 아이템 조회
15. 구매 요청
16. 구매 이력 조회
17. 청약철회
18. 관리자 결제 내역 조회
19. 감사 로그 조회

현재 우선순위에 없는 기능은 먼저 구현하지 않는다.

---

## 4. 패키지 구조 기준

기본 패키지는 실제 프로젝트의 base package를 우선한다.
아래 구조는 예시이며, 실제 프로젝트의 패키지명을 기준으로 조정한다.

```text
com.example.gameplatform
├─ global
│  ├─ common
│  │  ├─ response
│  │  └─ error
│  ├─ exception
│  └─ security
│     ├─ apikey
│     └─ config
│
├─ domain
│  ├─ auth
│  │  ├─ controller
│  │  ├─ service
│  │  └─ dto
│  ├─ account
│  │  ├─ controller
│  │  ├─ service
│  │  ├─ repository
│  │  ├─ entity
│  │  └─ dto
│  ├─ product
│  ├─ purchase
│  └─ audit
│
└─ GamePlatformApplication
```

패키지명을 새로 만들 때는 다음 기준을 따른다.

* 전역 공통 기능은 `global` 하위에 둔다.
* 도메인별 기능은 `domain.{도메인명}` 하위에 둔다.
* 인증/인가 공통 처리는 `global.security`에 둔다.
* API Key 검증, 인증 토큰 발급, 관리자 로그인처럼 인증 흐름에 가까운 기능은 `domain.auth`에 둔다.
* 테스트 패키지는 운영 코드 패키지 구조와 최대한 동일하게 맞춘다.

---

## 5. 코드 작성 규칙

### DTO

* DTO는 `class` 또는 `record` 중 프로젝트 기준에 맞는 방식을 선택한다.
* 한 프로젝트 안에서는 DTO 작성 방식을 최대한 일관되게 유지한다.
* `class` 기반 DTO를 사용할 경우 Lombok의 `@Getter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` 등을 사용할 수 있다.
* `record` 기반 DTO를 사용할 경우 불변 응답 객체나 단순 요청 객체에 우선 적용한다.
* Request DTO에는 필요한 경우 validation annotation을 사용한다.
* Entity를 API 응답으로 직접 반환하지 않는다.
* DTO에는 불필요한 비즈니스 로직을 넣지 않는다.

### Controller

* Controller는 요청과 응답 처리만 담당한다.
* 비즈니스 로직은 Service로 위임한다.
* 모든 정상 응답은 가능한 한 공통 응답 포맷을 사용한다.
* URL은 `/api/v1` 기준 REST 구조를 따른다.
* Controller에서 try-catch를 남발하지 않는다.

### Service

* Service는 비즈니스 로직을 담당한다.
* 상태 검증, 예외 발생, 도메인 흐름 제어를 담당한다.
* 외부 연동이 필요한 경우 별도 Client 또는 Component로 분리한다.
* 트랜잭션이 필요한 경우 `@Transactional` 적용 여부를 명확히 판단한다.

### Repository

* Spring Data JPA Repository를 사용한다.
* 복잡한 조회가 필요해지기 전까지는 query method를 우선한다.
* 직접 작성한 쿼리가 추가될 경우 테스트 또는 사용 근거를 남긴다.

### Entity

* Entity는 DB 테이블 매핑과 도메인 상태를 표현한다.
* JPA는 field access를 기준으로 사용하므로 모든 필드에 getter/setter를 무조건 열 필요는 없다.
* setter는 최대한 사용하지 않는다.
* 상태 변경은 `changeUsername()`, `suspend()`, `requestRefund()`처럼 의미 있는 메서드로 표현한다.
* 외부에서 읽어야 하는 값에 대해서만 필요한 getter를 제공한다.
* Lombok을 사용할 경우 Entity 전체에 `@Data`, `@Setter`를 붙이지 않는다.
* `@Getter`도 무조건 class 단위로 붙이기보다 필요한 범위만 공개하는 방식을 우선 고려한다.
* API 응답 변환 로직을 Entity에 넣지 않는다.

---

## 6. 공통 응답 / 예외 처리 규칙

* 성공 응답은 `CommonResponse`를 사용한다.
* 에러 코드는 `ErrorCode` enum에서 관리한다.
* 비즈니스 예외는 `BusinessException`으로 처리한다.
* Controller에서 try-catch를 남발하지 않는다.
* 예외 응답은 `GlobalExceptionHandler`에서 일괄 처리한다.
* API마다 다른 에러 포맷을 만들지 않는다.
* validation 실패 응답도 공통 에러 응답 포맷과 최대한 일관되게 처리한다.

기본 응답 구조와 에러 코드 목록은 `README.md`를 기준으로 한다.

---

## 7. 보안 구현 기준

* 화면단이 없는 API 서버 성격을 고려하여 OAuth 로그인은 우선 구현 범위에서 제외한다.
* 외부 서비스 또는 클라이언트 서버는 Client ID / API Key로 식별한다.
* API Key는 평문 저장을 피하고, 해시 또는 암호화 저장 방식을 고려한다.
* API Key는 요청 Header로 전달하는 방식을 우선 고려한다.
* 예시 Header는 `X-Client-Id`, `X-Api-Key`를 사용한다.
* 필요한 경우 Client ID / API Key 검증 후 내부 Access Token을 발급하는 구조를 사용할 수 있다.
* USER와 ADMIN 권한을 분리한다.
* 관리자 로그인은 별도 username/password 기반 API로 분리한다.
* 인증 실패는 인증 실패 에러 코드로 처리한다.
* 권한 부족은 권한 부족 에러 코드로 처리한다.
* API Key, JWT Secret, DB Password 등 민감 정보는 코드에 직접 작성하지 않는다.
* 보안 관련 설정은 실제 운영 설정을 추측해서 작성하지 않는다.

---

## 8. 테스트 코드 작성 기준

테스트 코드는 기능 구현 이후에 몰아서 작성하지 않고, 공통 구조와 주요 도메인 기능을 구현할 때 함께 작성한다.

### 테스트 우선순위

1. 공통 응답 DTO 테스트
2. ErrorCode enum 테스트
3. BusinessException 테스트
4. GlobalExceptionHandler 테스트
5. 관리자 로그인 API 테스트
6. Client ID / API Key 인증 테스트
7. 계정 조회 / 닉네임 수정 테스트
8. 계정 상태 변경 테스트
9. 상품 조회 테스트
10. 구매 요청 테스트
11. 구매 이력 조회 테스트
12. 청약철회 테스트
13. 감사 로그 조회 테스트

### 테스트 범위

* 단순 getter/setter만 검증하는 테스트는 작성하지 않는다.
* 비즈니스 규칙, 예외 발생 조건, 응답 포맷, 권한 분리를 우선 검증한다.
* Controller 테스트에서는 HTTP Method, URL, Request Body, Response Body, Status Code를 검증한다.
* Service 테스트에서는 정상 흐름, 예외 흐름, 상태 변경 결과를 검증한다.
* Repository 테스트는 복잡한 쿼리나 직접 작성한 조회 조건이 있을 때 우선 작성한다.

### 테스트 작성 방식

* Controller 계층은 `@WebMvcTest` 또는 `@SpringBootTest + @AutoConfigureMockMvc`를 사용한다.
* Service 계층은 JUnit5와 Mockito 기반 단위 테스트를 우선한다.
* Repository 계층은 필요한 경우 `@DataJpaTest`를 사용한다.
* 인증/인가 테스트는 USER / ADMIN 권한 분리와 인증 실패 케이스를 포함한다.
* 공통 예외 응답은 `GlobalExceptionHandler`를 통해 동일한 포맷으로 내려가는지 확인한다.

### 테스트에서 우선 검증할 케이스

* 정상 요청 시 `SUCCESS` 응답이 내려가는지
* 잘못된 요청 시 `ERR-001` 응답이 내려가는지
* 인증 실패 시 `AUTH-001` 응답이 내려가는지
* 권한 부족 시 `AUTH-002` 응답이 내려가는지
* 존재하지 않는 계정 조회 시 `ACCOUNT-001` 응답이 내려가는지
* 이미 등록된 Client ID / API Key이면 `ACCOUNT-002` 응답이 내려가는지
* 판매 중지 상품 구매 시 `BILLING-002` 응답이 내려가는지
* 이미 환불된 구매 건이면 `REFUND-002` 응답이 내려가는지
* 관리자 권한이 필요한 API에 USER 권한으로 접근할 수 없는지
* Entity 상태 변경 메서드가 의도한 상태만 변경하는지

### 테스트 코드 작성 시 주의사항

* 테스트명은 어떤 상황에서 어떤 결과를 기대하는지 드러나게 작성한다.
* given / when / then 흐름을 유지한다.
* 실제 결제 PG, 실제 Redis, 실제 AWS에 의존하는 테스트는 기본 단위 테스트에서 제외한다.
* 외부 연동은 Mock 또는 Fake 객체로 대체한다.
* 테스트를 위해 운영 코드를 과도하게 열어두지 않는다.
* 포트폴리오에서 설명 가능한 핵심 테스트를 우선한다.

---

## 9. 작업 후 문서화 기준

Agent는 코드 작업을 완료한 뒤, 작업 내용을 문서로 남기는 것을 기본 원칙으로 한다.

### 문서화 대상

작업 후 변경 내용에 따라 아래 문서를 갱신한다.

```text
README.md
AGENTS.md
API 명세서
ERD 문서
CHANGELOG.md 또는 작업 로그 문서
```

단, 모든 작업마다 모든 문서를 수정하지는 않는다.
변경 내용과 관련 있는 문서만 갱신한다.

### README.md 갱신 기준

다음 변경이 있을 경우 `README.md`를 갱신한다.

* 주요 기능 추가
* 실행 방법 변경
* 기술 스택 변경
* 패키지 구조 변경
* 배포 플로우 변경
* 환경 변수 추가
* Docker Compose 실행 방식 변경
* 현재 진행 상태 변경

### API 문서 갱신 기준

다음 변경이 있을 경우 API 문서 또는 Swagger/OpenAPI 설명을 갱신한다.

* API URL 추가 또는 변경
* Request Body 변경
* Response Body 변경
* Query Parameter 변경
* Path Variable 변경
* 인증/권한 조건 변경
* 에러 코드 추가 또는 변경

### ERD 문서 갱신 기준

다음 변경이 있을 경우 ERD 또는 테이블 정의 문서를 갱신한다.

* Entity 추가
* 컬럼 추가/삭제/변경
* 연관관계 변경
* 상태 코드 변경
* 인덱스 또는 제약조건 추가

### 작업 로그 작성 기준

기능 단위 작업이 끝나면 작업 로그를 남긴다.

작업 로그에는 최소한 아래 내용을 포함한다.

```text
- 작업 일자
- 작업 범위
- 추가/수정한 파일
- 주요 구현 내용
- 테스트 여부
- 남은 작업
- 특이사항 또는 의사결정 내용
```

예시:

```text
## 2026-05-10 작업 로그

### 작업 범위
- 공통 응답 DTO 추가
- ErrorCode enum 추가
- BusinessException 추가
- GlobalExceptionHandler 추가

### 주요 구현 내용
- 모든 API 응답을 CommonResponse 형식으로 통일
- 비즈니스 예외 발생 시 ErrorCode 기반 에러 응답 반환
- GlobalExceptionHandler에서 공통 예외 처리 수행

### 테스트
- CommonResponse 생성 테스트 완료
- BusinessException 에러코드 매핑 테스트 완료
- GlobalExceptionHandler 응답 포맷 테스트 예정

### 남은 작업
- 관리자 로그인 API 기본 구조 구현
```

### 문서화 시 주의사항

* 실제 구현되지 않은 내용을 완료된 것처럼 작성하지 않는다.
* 문서와 코드가 서로 다르면 코드를 기준으로 문서를 수정한다.
* 임시 구현, Mock 구현, 추후 구현 예정 기능은 명확히 구분한다.
* 포트폴리오 설명에 필요한 의사결정은 간단히 근거를 남긴다.
* 문서 수정도 커밋에 포함한다.

---

## 10. 실행 및 검증 명령

Agent는 코드 수정 후 아래 명령 중 현재 작업에 맞는 명령을 실행한다.

### 기본 빌드

```bash
./gradlew clean build
```

### 테스트만 실행

```bash
./gradlew test
```

### 테스트 환경 문제로 전체 빌드가 어려운 경우

```bash
./gradlew clean build -x test
```

### Docker Compose 실행

```bash
docker compose up -d
```

### Docker Compose 종료

```bash
docker compose down
```

### 검증 기준

* 테스트가 통과하면 통과한 명령을 작업 결과에 기록한다.
* 테스트가 실패하면 임의로 대규모 수정하지 않고 실패 원인을 먼저 요약한다.
* 테스트 환경 미구성으로 실패한 경우, 코드 문제인지 환경 문제인지 구분해서 보고한다.
* 빌드 또는 테스트 명령을 실행하지 못한 경우, 실행하지 못한 이유를 명확히 남긴다.

---

## 11. Git 작업 기준

Agent는 작업 전 현재 변경 상태를 확인한다.

```bash
git status
```

작업 완료 후 변경 내용을 확인한다.

```bash
git diff
```

작업 결과에는 아래 내용을 요약한다.

```text
- 변경한 파일
- 변경 이유
- 실행한 테스트 명령
- 테스트 결과
- 남은 작업
```

### Git 작업 시 주의사항

* 사용자가 요청하지 않는 한 직접 커밋하지 않는다.
* 사용자가 요청하지 않는 한 브랜치를 새로 만들지 않는다.
* 기존 사용자의 미커밋 변경사항을 임의로 되돌리지 않는다.
* 충돌이 발생하면 임의로 해결하지 않고 충돌 파일과 원인을 보고한다.
* 작업 범위 밖의 파일 변경이 발생하면 반드시 이유를 설명한다.

---

## 12. 수정 금지 파일 및 민감 정보 기준

명시 요청이 있기 전까지 아래 파일은 생성, 수정, 삭제하지 않는다.

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

민감 정보 처리 기준은 아래를 따른다.

* DB 비밀번호, JWT Secret, API Key, AWS Access Key는 코드에 직접 작성하지 않는다.
* 예시가 필요한 경우 `example`, `sample`, `dummy` 값을 사용한다.
* 실제 운영 설정을 추측해서 작성하지 않는다.
* 설정값이 필요한 경우 README.md에 환경 변수 이름만 문서화한다.
* secret 파일이 필요하면 실제 값이 아닌 예시 파일만 작성한다.

예시 파일명은 아래와 같이 작성할 수 있다.

```text
.env.example
application-secret.example.yml
```

---

## 13. 작업 금지사항

다음 작업은 명시 요청이 있기 전까지 하지 않는다.

* Kafka 구현
* ELK 로그 수집 구현
* 공지 관리 구현
* 이벤트 관리 구현
* 알림 기능 구현
* 결제 PG 실제 연동
* OAuth 제공자별 실제 외부 API 연동 세부 구현
* OAuth 로그인 구현
* 프론트엔드 구현
* 과도한 추상화
* 사용하지 않는 공통 클래스 선생성

다음 코드 작성 방식은 피한다.

* Controller에 비즈니스 로직 작성
* Entity를 API 응답으로 직접 반환
* DTO에 불필요한 도메인 로직 작성
* setter 남발
* DTO 작성 방식을 이유 없이 혼용
* API마다 제각각인 응답 포맷 작성
* 현재 구현 단계보다 앞선 기능 선구현
* 테스트 없이 핵심 비즈니스 로직만 계속 추가
* 실패한 테스트를 임의로 삭제하거나 비활성화

---

## 14. 커밋 메시지 규칙

커밋 메시지 규칙은 `README.md`의 규칙을 따른다.

예시:

```bash
git commit -m "[docs :] README 및 AGENTS 문서 추가"
git commit -m "[feature :] 프로젝트 초기 설정 및 공통 응답 구조 추가"
git commit -m "[feature :] 관리자 로그인 API 기본 구조 구현"
```

커밋 메시지는 작업 단위가 드러나게 작성한다.
여러 기능을 한 커밋에 과도하게 섞지 않는다.

---

## 15. Agent 요청 예시

Agent에게 작업을 요청할 때는 작은 단위로 요청한다.

```text
AGENTS.md 기준으로 기본 패키지 구조를 생성해줘.
```

```text
공통 응답 DTO, ErrorCode enum, BusinessException, GlobalExceptionHandler를 먼저 구현해줘.
```

```text
Client ID / API Key 인증을 위한 Request DTO, Service, 인증 Filter 기본 구조를 만들어줘.
```

```text
관리자 로그인 API의 Controller, Service, DTO 기본 구조를 만들어줘. 실제 토큰 발급 구현은 다음 단계로 미뤄줘.
```

```text
accounts, admin_credentials, products, purchases, audit_logs 엔티티 초안을 만들어줘.
```

```text
현재 프로젝트 구조를 분석하고, 구현 계획만 작성해줘. 파일은 수정하지 마.
```

```text
작업 후 ./gradlew test를 실행하고, 실패하면 원인만 요약해줘. 임의로 대규모 수정하지 마.
```

---

## 16. Agent 판단 기준

작업 중 애매한 부분이 있으면 아래 기준을 우선한다.

1. README.md에 정리된 프로젝트 방향
2. 현재 구현 우선순위
3. 기존 코드 스타일
4. 단순하고 설명 가능한 구조
5. 포트폴리오에서 설명하기 쉬운 설계

불필요하게 복잡한 구조보다, 현재 단계에서 구현 가능하고 설명 가능한 구조를 우선한다.

명확하지 않은 부분은 임의로 확정하지 않고, 가능한 선택지를 짧게 정리한 뒤 사용자의 결정을 요청한다.
