# API Client 인증 보안 보완 및 후속 처리 정리

## 1. 배경

현재 API Client 인증 구현은 `Client ID + API Key` 기반의 최소 인증 흐름이다.

- `api_clients` 테이블에는 API Key 원문이 아니라 `api_key_hash`만 저장한다.
- 요청 시 전달된 API Key를 서버에서 동일한 방식으로 해시한 뒤 저장된 해시와 비교한다.
- local profile에서는 `LocalApiClientInitializer`가 테스트용 client와 API Key를 직접 등록한다.
- 아직 Notion 사양에 있는 API Client 등록, API Key 재발급, API Key 기반 토큰 발급 API는 구현되지 않았다.

따라서 현재 인증부만 보면 고객이 어떤 값을 받아서 어떤 값을 넣어야 하는지 흐름이 생략되어 보일 수 있다.
정상적인 최종 흐름에서는 고객에게 `api_key_hash`를 제공하지 않고, 발급 시 생성한 원문 API Key를 한 번만 제공해야 한다.

## 2. 기준 인증 흐름

### 2.1 API Client 등록 시

1. 관리자 권한으로 API Client 등록 API를 호출한다.
2. 서버가 `clientId`와 랜덤 API Key를 생성한다.
3. 서버는 API Key 원문을 저장하지 않고 SHA-256 해시만 저장한다.
4. 응답에는 고객이 사용할 API Key 원문을 한 번만 포함한다.

```text
customer receives: clientId, apiKey
server stores: clientId, apiKeyHash
```

### 2.2 인증 요청 시

1. 고객은 플랫폼에서 제공받은 `clientId`, `apiKey`를 전달한다.
2. 서버는 `clientId`로 API Client를 조회한다.
3. API Client 상태가 `ACTIVE`인지 확인한다.
4. 요청 API Key를 서버 기준 방식으로 SHA-256 해시 처리한다.
5. 저장된 `apiKeyHash`와 constant-time 비교한다.

```text
hash(request.apiKey) == stored.apiKeyHash
```

## 3. 현재 인증부 보완 필요 사항

### 3.1 API Key 발급/등록 API 구현

Notion API 사양서에는 다음 항목이 존재한다.

- `POST /api/v1/admin/api-clients`
  - API Client 등록
  - 응답에 `clientId`, `apiKey` 포함
- `POST /api/v1/admin/api-clients/{clientId}/api-key/rotate`
  - API Key 재발급
  - 응답에 새 `apiKey` 포함
- `POST /api/v1/auth/token`
  - `clientId + apiKey` 검증 후 Access Token 발급

현재 구현은 위 사양 중 DB 기반 API Key 검증 흐름만 먼저 구현된 상태다.
따라서 다음 구현 단위에서는 등록, 재발급, 토큰 발급 중 우선순위를 정해야 한다.

권장 순서는 다음과 같다.

1. API Client 등록
2. API Key 재발급
3. API Key 기반 Access Token 발급
4. Access Token을 사용하는 보호 API 연동

### 3.2 API Key 원문 노출 범위 제한

API Key 원문은 발급 또는 재발급 응답에서 한 번만 노출해야 한다.

- DB 저장 금지
- 로그 출력 금지
- 예외 메시지 포함 금지
- 응답 재조회 API에서 재노출 금지

API Client 조회 API에서는 API Key 전체 대신 다음 중 하나만 제공하는 편이 안전하다.

```text
apiKeyLast4: "a1b2"
apiKeyIssuedAt: "2026-06-06 22:41:35"
apiKeyRotatedAt: "2026-06-06 22:41:35"
```

### 3.3 SHA-256 유지 기준

현재 `ApiKeyHasher`는 API Key를 SHA-256으로 해시한다.
현재 API Key는 `SecureRandom`으로 생성한 32바이트 랜덤 값을 URL-safe Base64 문자열로 제공하는 구조다.
API Key 자체의 엔트로피가 충분하므로 현 단계에서는 SHA-256 해시 저장 방식을 유지한다.

권장 방향:

```text
apiKeyHash = SHA-256(apiKey)
```

주의할 점:

- API Key는 반드시 충분히 긴 랜덤 값으로 생성한다.
- 고객이 직접 정한 짧은 문자열을 API Key로 허용하지 않는다.
- API Key 원문은 DB, Redis, 로그, 예외 메시지에 남기지 않는다.
- API Key 비교는 현재처럼 constant-time 비교를 유지한다.

### 3.4 인증 실패 응답 정책 정리

현재는 인증 실패를 공통 `AUTHENTICATION_FAILED`로 처리한다.
보안 관점에서는 client 존재 여부, key 불일치 여부를 외부에 자세히 노출하지 않는 방식이 안전하다.

다만 Notion 사양에는 다음처럼 세분화된 client 오류가 있다.

- `CLIENT-001`: CLIENT NOT FOUND, 404
- `CLIENT-002`: INVALID API KEY, 401
- `CLIENT-003`: CLIENT DISABLED, 403

외부 공개 API에서는 공격자가 client 존재 여부를 추론할 수 있으므로 다음 중 하나로 결정해야 한다.

| 방향 | 장점 | 단점 |
| --- | --- | --- |
| 공통 401 유지 | client enumeration 방어에 유리 | Notion 사양과 차이 발생 |
| Notion 사양대로 세분화 | API 문서와 일치 | client 존재 여부 추론 가능 |

권장안은 외부 인증 API에서는 공통 401을 유지하고, 내부 감사 로그에는 실패 원인을 상세 기록하는 방식이다.

### 3.5 실패 횟수 제한 및 잠금 정책

API Key 인증 실패가 반복되는 경우 brute force 또는 credential stuffing 시도로 볼 수 있다.

추가 검토 항목:

- `clientId` 기준 실패 횟수 제한
- source IP 기준 실패 횟수 제한
- 짧은 TTL 기반 일시 차단
- 관리자 조회용 실패 이력 또는 감사 로그 저장

이 기능은 Redis 도입 이후 구현하는 것이 적합하다.

### 3.6 IP allowlist 적용

Notion의 API Client 등록 사양에는 `allowedIps`가 있다.
현재 entity와 인증 서비스에는 IP allowlist 검증이 포함되어 있지 않다.

추가 구현 시 고려할 점:

- `allowedIps` 저장 구조
- CIDR 허용 여부
- proxy 또는 load balancer 환경에서 client IP 추출 방식
- `X-Forwarded-For` 신뢰 구간 설정
- IP 불일치 시 401 또는 403 중 어떤 응답을 줄지 결정

## 4. Redis 도입 시 후속 처리 방향

현재 프로젝트에는 Redis dependency와 설정 흔적이 있지만 API Client 인증 캐시로는 아직 사용하지 않는다.
Redis는 API Key 원문을 저장하는 용도가 아니라 조회 성능, 실패 횟수 제한, 임시 차단 처리에 사용하는 편이 적합하다.

### 4.1 API Client 인증 캐시

DB 조회를 매 요청마다 수행하지 않기 위해 Redis에 API Client 인증 정보를 캐시할 수 있다.

권장 key:

```text
api-client:{clientId}
```

권장 value:

```json
{
  "clientId": "game-service-a-001",
  "apiKeyHash": "hmac-or-hash-value",
  "role": "USER",
  "status": "ACTIVE",
  "allowedIps": ["203.0.113.10"],
  "allowedIps": ["203.0.113.10"]
}
```

주의:

- API Key 원문은 Redis에도 저장하지 않는다.
- Redis cache miss 시 DB에서 조회하고 Redis에 적재한다.
- API Client 상태 변경, 재발급, 삭제 시 Redis key를 즉시 삭제하거나 갱신한다.

### 4.2 API Key 재발급 시 캐시 무효화

API Key 재발급 API가 구현되면 다음 처리가 필요하다.

1. 새 API Key 생성
2. 새 API Key hash 저장
3. 기존 Redis cache 삭제
4. 새 API Key 원문을 응답으로 한 번만 반환

```text
DEL api-client:{clientId}
```

이후 첫 인증 요청에서 DB 기준 최신 hash를 다시 Redis에 적재한다.

### 4.3 Client 상태 변경 시 캐시 무효화

API Client 상태가 `DISABLED`, `DELETED`로 변경되는 경우 캐시된 `ACTIVE` 상태가 남아 있으면 안 된다.

필수 처리:

```text
DEL api-client:{clientId}
```

또는 상태 변경 직후 Redis value를 최신 상태로 갱신한다.
단순성과 일관성을 우선하면 삭제 후 재적재 방식이 낫다.

### 4.4 실패 횟수 제한

Redis를 사용하면 인증 실패 횟수 제한을 TTL 기반으로 간단히 구현할 수 있다.

예시 key:

```text
api-client-auth-fail:client:{clientId}
api-client-auth-fail:ip:{ip}
api-client-auth-block:client:{clientId}
api-client-auth-block:ip:{ip}
```

예시 정책:

```text
5분 동안 실패 10회 이상이면 10분 차단
```

주의:

- 존재하지 않는 clientId에 대해서도 동일한 비용과 유사한 응답을 유지한다.
- 실패 카운터로 client 존재 여부가 노출되지 않도록 응답 메시지는 단순하게 유지한다.

## 5. 추가 데이터 모델 검토

현재 `ApiClient` 최소 필드는 다음에 가깝다.

```text
clientId
apiKeyHash
name
role
status
createdAt
updatedAt
```

Notion 사양과 보안 요구를 반영하면 다음 필드 추가를 검토할 수 있다.

```text
description
allowedIps
apiKeyLast4
apiKeyIssuedAt
apiKeyRotatedAt
lastAuthenticatedAt
```

`allowedIps`는 별도 테이블로 분리하거나 JSON 컬럼으로 둘 수 있다.
검색과 변경 이력이 중요하면 별도 테이블이 낫고, 단순 검증 목적이면 JSON 또는 element collection도 가능하다.

## 6. 권장 구현 순서

1. API Client 등록 API 구현
   - API Key 생성
   - hash 저장
   - 원문 API Key 1회 응답

2. API Key 재발급 API 구현
   - 기존 hash 교체
   - 원문 새 API Key 1회 응답
   - 추후 Redis 도입 시 cache invalidation 포함

3. 인증 실패 응답 정책 결정
   - 공통 401 유지 또는 Notion 사양 세분화 중 선택
   - 외부 응답과 내부 감사 로그 정책 분리 권장

4. SHA-256 기반 API Key 해시 정책 명시
   - API Key 생성 길이 유지
   - 원문 저장 금지
   - constant-time 비교 유지

5. Redis 인증 캐시 도입
   - API Client 조회 캐시
   - 상태 변경 및 재발급 시 무효화

6. Redis 기반 실패 횟수 제한 도입
   - clientId/IP 기준 카운터
   - TTL 기반 임시 차단

7. IP allowlist 검증 추가
   - Notion 등록 사양의 `allowedIps` 반영
   - proxy 환경의 실제 client IP 추출 정책 확정

## 7. 결론

현재 인증 구현의 큰 방향은 유지해도 된다.
다만 최종 사양을 완성하려면 API Client 등록과 API Key 재발급 API가 먼저 붙어야 한다.

Redis는 API Key 원문 복호화 저장소로 쓰기보다 다음 용도로 도입하는 것이 적합하다.

- API Client 인증 정보 캐시
- API Key 재발급/상태 변경 시 캐시 무효화
- 인증 실패 횟수 제한
- 임시 차단 처리

API Key는 고객에게 원문 형태의 불투명한 문자열로 한 번만 제공하고, 서버와 Redis에는 SHA-256 해시만 저장하는 정책을 기준으로 삼는다.
