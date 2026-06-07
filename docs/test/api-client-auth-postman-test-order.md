# API Client 인증 Postman 테스트 순서

## 1. 서버 실행

local profile 기준으로 서버를 실행한다.

```bash
.\gradlew bootRun
```

기본 URL:

```text
http://localhost:9080
```

local profile에서는 테스트용 API Client가 자동 생성된다.

| 용도 | clientId | apiKey |
| --- | --- | --- |
| 일반 client | `test-user-client` | `test-user-api-key` |
| 관리자 client | `test-admin-client` | `test-admin-api-key` |
| 비활성 client | `test-disabled-client` | `test-disabled-api-key` |

## 2. USER 토큰 발급

```text
POST http://localhost:9080/api/v1/auth/token
```

Headers:

```text
Content-Type: application/json
```

Body:

```json
{
  "clientId": "test-user-client",
  "apiKey": "test-user-api-key"
}
```

기대 결과:

```text
200 OK
data.tokenType = Bearer
data.clientId = test-user-client
data.role = USER
```

Postman 변수로 저장:

```text
userAccessToken = data.accessToken
```

## 3. USER 토큰으로 일반 보호 API 호출

```text
GET http://localhost:9080/api/v1/api-clients/auth-check
```

Headers:

```text
Authorization: Bearer {{userAccessToken}}
```

기대 결과:

```text
200 OK
data.clientId = test-user-client
data.authorities[0] = ROLE_USER
```

## 4. USER 토큰으로 관리자 보호 API 호출

```text
GET http://localhost:9080/api/v1/admin/api-clients/auth-check
```

Headers:

```text
Authorization: Bearer {{userAccessToken}}
```

기대 결과:

```text
403 FORBIDDEN
code = AUTH-002
message = FORBIDDEN
```

## 5. ADMIN 토큰 발급

```text
POST http://localhost:9080/api/v1/auth/token
```

Headers:

```text
Content-Type: application/json
```

Body:

```json
{
  "clientId": "test-admin-client",
  "apiKey": "test-admin-api-key"
}
```

기대 결과:

```text
200 OK
data.tokenType = Bearer
data.clientId = test-admin-client
data.role = ADMIN
```

Postman 변수로 저장:

```text
adminAccessToken = data.accessToken
```

## 6. ADMIN 토큰으로 관리자 보호 API 호출

```text
GET http://localhost:9080/api/v1/admin/api-clients/auth-check
```

Headers:

```text
Authorization: Bearer {{adminAccessToken}}
```

기대 결과:

```text
200 OK
data.clientId = test-admin-client
data.authorities[0] = ROLE_ADMIN
```

## 7. 실패 케이스 확인

### 7.1 잘못된 API Key

```text
POST http://localhost:9080/api/v1/auth/token
```

Body:

```json
{
  "clientId": "test-user-client",
  "apiKey": "wrong-api-key"
}
```

기대 결과:

```text
401 UNAUTHORIZED
code = AUTH-001
message = AUTHENTICATION FAILED
```

### 7.2 비활성 client

```text
POST http://localhost:9080/api/v1/auth/token
```

Body:

```json
{
  "clientId": "test-disabled-client",
  "apiKey": "test-disabled-api-key"
}
```

기대 결과:

```text
401 UNAUTHORIZED
code = AUTH-001
message = AUTHENTICATION FAILED
```

### 7.3 토큰 없이 보호 API 호출

```text
GET http://localhost:9080/api/v1/api-clients/auth-check
```

Headers:

```text
Authorization 헤더 없음
```

기대 결과:

```text
401 UNAUTHORIZED
code = AUTH-001
message = AUTHENTICATION FAILED
```

### 7.4 변조된 토큰으로 보호 API 호출

```text
GET http://localhost:9080/api/v1/api-clients/auth-check
```

Headers:

```text
Authorization: Bearer {마지막 문자를 바꾼 accessToken}
```

기대 결과:

```text
401 UNAUTHORIZED
code = AUTH-001
message = AUTHENTICATION FAILED
```

## 8. 권장 실행 순서 요약

1. USER 토큰 발급
2. USER 토큰으로 일반 보호 API 성공 확인
3. USER 토큰으로 관리자 보호 API 403 확인
4. ADMIN 토큰 발급
5. ADMIN 토큰으로 관리자 보호 API 성공 확인
6. 잘못된 API Key 401 확인
7. 비활성 client 401 확인
8. 토큰 없이 보호 API 401 확인
9. 변조된 토큰 401 확인
