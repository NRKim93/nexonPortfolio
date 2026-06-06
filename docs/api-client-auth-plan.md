# API Client Authentication Work Plan

이 문서는 다음 개발 단계에서 진행할 `Client ID + API Key` 인증 기반 작업 계획을 정리한다.

현재 프로젝트는 공통 응답, 공통 예외, JWT 검증용 기본 구조, 테스트용 H2 설정까지 정리된 상태다. 다음 단계에서는 API 서버의 호출 주체를 식별하기 위한 `api_clients` 도메인과 API Key 인증 골격을 먼저 구현한다.

---

## 1. 작업 목표

다음 작업의 목표는 외부 게임 서비스 또는 연동 시스템을 `API Client`로 식별하고, `Client ID + API Key` Header를 통해 요청 주체를 인증할 수 있는 최소 골격을 만드는 것이다.

JWT는 현재 단계에서 refresh token 흐름 없이 검증용 access token 구조만 유지한다. OAuth 로그인과 refresh token 저장소는 현재 구현 범위에 포함하지 않는다.

---

## 2. 우선 구현 범위

### 2.1 API Client 도메인

다음 패키지를 기준으로 구현한다.

```text
com.portfolio.nexon.domain.apiclient
```

우선 생성할 대상은 다음이다.

- `ApiClient` Entity
- `ApiClientStatus` enum
- `ApiClientRole` enum
- `ApiClientRepository`
- `ApiClientService`

상태 값은 다음을 기본으로 한다.

```text
ACTIVE
DISABLED
DELETED
```

역할 값은 다음을 기본으로 한다.

```text
USER
ADMIN
```

`ApiClient`는 최소한 다음 값을 가진다.

- id
- clientId
- apiKeyHash
- name
- role
- status
- createdAt
- updatedAt

`clientId`는 unique 제약을 가진다. API Key 원문은 DB에 저장하지 않고 `apiKeyHash`만 저장한다.

### 2.2 API Key 해시 컴포넌트

API Key 처리를 위한 별도 컴포넌트를 만든다.

예상 위치는 다음이다.

```text
com.portfolio.nexon.global.security.apikey
```

우선 생성할 대상은 다음이다.

- `ApiKeyGenerator`
- `ApiKeyHasher`

기준은 다음이다.

- API Key 원문은 최초 발급 또는 재발급 응답에서만 반환한다.
- DB에는 API Key 원문을 저장하지 않는다.
- 검증 시 요청 API Key를 동일한 방식으로 해시해 `apiKeyHash`와 비교한다.
- 해시 방식은 우선 설명 가능한 단순 구조로 시작한다.

### 2.3 API Key 인증 필터

Header 기반 인증 필터를 만든다.

사용할 Header는 다음이다.

```text
X-Client-Id
X-Api-Key
```

기본 흐름은 다음이다.

1. 요청 Header에서 `X-Client-Id`, `X-Api-Key`를 읽는다.
2. Header가 없으면 다음 필터로 넘긴다.
3. Client ID로 `ApiClient`를 조회한다.
4. Client 상태가 `ACTIVE`인지 확인한다.
5. API Key 해시를 비교한다.
6. 성공 시 Spring Security `Authentication`에 client id와 role을 넣는다.
7. 실패 시 인증 실패 에러로 처리한다.

인증 실패와 권한 부족은 서로 다른 에러로 분리한다.

### 2.4 SecurityConfig 정리

현재 `SecurityConfig`는 JWT 필터 중심이다. 다음 단계에서는 API Key 필터를 기본 인증 필터로 추가하고, JWT 필터는 보조 검증용으로 유지할지 별도 판단한다.

우선순위는 다음이다.

1. API Key 인증 필터
2. JWT 검증 필터
3. 인증 실패/권한 부족 핸들러

API 서버 기본 인증 주체는 API Client로 본다.

---

## 3. 다음 단계 API 후보

인증 골격 이후 구현할 API 후보는 다음이다.

| 기능 | Method | URL |
| --- | --- | --- |
| API Client 등록 | TBD | Notion API 사양 확인 필요 |
| API Client 조회 | TBD | Notion API 사양 확인 필요 |
| API Client 상태 변경 | TBD | Notion API 사양 확인 필요 |
| API Key 재발급 | TBD | Notion API 사양 확인 필요 |

URL과 Method가 Notion에서 확정되지 않은 경우 임의로 확정하지 않는다.

---

## 4. 제외 범위

다음 작업은 이번 단계에서 제외한다.

- OAuth 로그인
- OAuth provider 연동
- refresh token 발급/저장/재발급
- 관리자 username/password 로그인
- 계정, 상품, 구매, 청약철회 API 구현
- Redis 기반 API Client 인증 캐시
- 운영용 API Key rotation 정책 고도화
- 결제 PG 연동

Redis 인증 캐시는 DB 기반 검증이 먼저 안정화된 뒤 추가한다.

---

## 5. 테스트 기준

우선 작성할 테스트는 다음이다.

- `ApiClient` 상태 변경 메서드 테스트
- `ApiClientRepository` clientId 조회 테스트
- `ApiKeyGenerator` 생성 값 형식 테스트
- `ApiKeyHasher` 동일 입력 동일 해시 테스트
- `ApiKeyHasher` 다른 입력 다른 해시 테스트
- API Key 인증 성공 테스트
- API Key 누락 시 인증 미설정 테스트
- Client ID 없음 테스트
- API Key 불일치 테스트
- 비활성 Client 인증 실패 테스트
- ADMIN role 기반 권한 테스트

테스트는 기본적으로 `test` 프로필과 H2 DB를 사용한다.

```bash
./gradlew test
```

---

## 6. 완료 기준

이번 작업은 다음 조건을 만족하면 완료로 본다.

- `api_clients` 도메인 기본 Entity/Repository/Service가 존재한다.
- API Key 원문이 DB에 저장되지 않는다.
- `X-Client-Id`, `X-Api-Key` Header 기반 인증 흐름이 동작한다.
- 인증 실패와 권한 부족이 구분된다.
- `./gradlew test`가 통과한다.
- README 또는 관련 문서 갱신 필요 여부가 검토된다.
