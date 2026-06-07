# API Client 인증 다음 작업 순번

## 1. 현재 기준 상태

현재 인증 구현은 `Client ID + API Key`로 access token을 발급하고, 이후 `Authorization: Bearer <token>`으로 보호 API에 접근하는 최소 흐름까지 구현된 상태다.

- `POST /api/v1/auth/token`에서 `clientId`, `apiKey`를 검증한다.
- 검증에 성공하면 JWT access token을 발급한다.
- 발급된 token은 `Bearer` 방식으로 전달한다.
- JWT 검증에 성공하면 Spring Security `Authentication`이 설정된다.
- `/api/v1/admin/**`는 `ROLE_ADMIN` 권한을 요구한다.
- `POST /api/v1/admin/api-clients`에서 ADMIN 권한으로 API Client를 등록할 수 있다.
- `POST /api/v1/admin/api-clients/{clientId}/api-key/rotate`에서 ADMIN 권한으로 API Key를 재발급할 수 있다.
- API Key 원문은 등록/재발급 응답에서만 1회 반환하고, DB에는 `apiKeyHash`만 저장한다.
- 현재 전체 테스트는 통과한다.

이 문서는 위 상태 이후 실제 구현을 어떤 순서로 진행할지 정리한다.

## 2. 완료된 작업

### 2.1 토큰 발급 API 통합 테스트

`POST /api/v1/auth/token`의 통합 테스트를 작성했다.

검증 범위:

- 정상 `clientId`, `apiKey` 요청 시 `Bearer` access token 발급
- 응답에 `accessToken`, `tokenType`, `expiresIn`, `clientId`, `role` 포함
- 잘못된 API Key 요청 시 인증 실패
- 존재하지 않는 client 요청 시 인증 실패
- `DISABLED` client 요청 시 인증 실패
- validation 실패 요청 시 공통 에러 응답

완료 기준:

- Controller, Service, Repository, Security 예외 흐름이 함께 검증된다.
- 테스트는 `test` profile과 H2 DB 기준으로 통과한다.

### 2.2 Bearer 토큰 기반 보호 API 통합 테스트

토큰 발급 후 실제 보호 API 접근까지 이어지는 테스트를 작성했다.

검증 범위:

- 토큰 없이 보호 API 접근 시 401
- 유효한 `ROLE_USER` token으로 일반 보호 API 접근 성공
- 유효한 `ROLE_USER` token으로 `/api/v1/admin/**` 접근 시 403
- 유효한 `ROLE_ADMIN` token으로 `/api/v1/admin/**` 접근 성공
- 변조된 token 또는 만료된 token 요청 시 인증 실패

완료 기준:

- access token이 실제 인증 수단으로 동작한다.
- 인증 실패와 권한 부족이 분리되어 검증된다.

### 2.3 README 인증 섹션 정리

현재 README에는 JWT/OAuth 중심의 과거 설명이 남아 있다.
현재 구현 기준에 맞춰 인증 흐름을 다음 형태로 정리한다.

```text
Client ID + API Key -> Access Token 발급 -> Bearer Token 인증
```

정리 범위:

- 인증/인가 설명
- API 목록
- 실행 후 테스트 호출 예시
- OAuth, refresh token 관련 표현의 현재 범위 명시

### 2.4 local/test client 생성 방식 정리

로컬에서 바로 인증 흐름을 확인할 수 있도록 테스트 client 기준을 문서화한다.

정리 범위:

- local profile에서 생성되는 clientId
- local profile에서 사용할 apiKey
- USER client와 ADMIN client 구분
- 직접 curl 또는 HTTP client로 확인 가능한 예시

주의:

- 실제 운영 API Key 원문은 DB, Redis, 로그에 남기지 않는 정책을 유지한다.
- local/dev 확인용 key와 운영 정책을 문서에서 명확히 분리한다.

### 2.5 API Client 등록 API

토큰 인증 흐름 검증 후 API Client 등록 API를 구현했다.

검증 범위:

- 관리자 권한으로 client 생성
- `clientId` 생성 또는 입력 정책 결정
- API Key 원문 1회 응답
- DB에는 `apiKeyHash`만 저장
- 중복 clientId 방지

### 2.6 API Key 재발급 API

등록 API 이후 API Key 재발급 API를 구현했다.

검증 범위:

- 관리자 권한 확인
- 새 API Key 생성
- 기존 `apiKeyHash` 교체
- 새 API Key 원문 1회 응답
- 기존 API Key로 인증 실패
- 새 API Key로 인증 성공
- USER token으로 재발급 API 호출 시 403
- `@PathVariable("clientId")`로 path variable 이름을 명시해 runtime parameter name 미보존 환경에서도 정상 동작

## 3. 바로 다음 작업

### 3.1 인증 실패 응답 및 감사 로그 정책 정리

API Client 등록과 API Key 재발급 API가 붙었으므로 다음 단계에서는 인증 실패 응답 정책과 내부 기록 범위를 정리한다.

검토 범위:

- 외부 응답은 client 존재 여부와 API Key 불일치 여부를 과도하게 노출하지 않는 방향 유지
- 내부 로그 또는 감사 로그에는 실패 원인을 추적 가능한 수준으로 기록
- API Key 원문은 로그, 예외 메시지, DB, Redis에 남기지 않음
- 추후 Redis 실패 횟수 제한과 연결 가능한 이벤트 기준 정리

## 4. 보안 후속 작업

아래 항목은 access token 인증 흐름과 API Client 관리 API가 안정화된 뒤 진행한다.

1. 인증 실패 응답 정책 확정
2. 감사 로그 또는 실패 원인 내부 기록
3. Redis 기반 API Client 인증 캐시
4. API Key 재발급 및 상태 변경 시 Redis cache invalidation
5. Redis 기반 실패 횟수 제한
6. IP allowlist 검증
7. refresh token 도입 여부 재검토

## 5. 최종 순번 요약

1. 토큰 발급 API 통합 테스트 완료
2. Bearer 토큰 기반 보호 API 통합 테스트 완료
3. README 인증 섹션 정리 진행
4. local/test client 생성 방식 문서화 진행
5. API Client 등록 API 구현 완료
6. API Key 재발급 API 구현 완료
7. 인증 실패 응답 및 감사 로그 정책 정리
8. Redis 인증 캐시 도입
9. Redis 기반 실패 횟수 제한
10. IP allowlist 검증
11. refresh token 필요성 재검토

현재 바로 착수할 작업은 7번 `인증 실패 응답 및 감사 로그 정책 정리`다.
