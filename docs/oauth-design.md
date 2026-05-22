# OAuth 설계 메모

## 목적

OAuth는 외부 플랫폼 계정을 이용한 로그인 수단으로 사용한다.

서비스 내부 인증은 기존 JWT 구조를 유지한다. Google, Kakao, Naver 같은 OAuth provider의 access token을 서비스 API 인증에 직접 사용하지 않고, OAuth 로그인 성공 후 우리 서버가 자체 access token과 refresh token을 발급한다.

## 기본 방향

OAuth의 책임은 외부 사용자 신원 확인까지로 제한한다.

```text
OAuth provider 로그인
-> provider 사용자 정보 조회
-> 서비스 회원 조회 또는 생성
-> 자체 JWT 발급
-> 이후 API 요청은 자체 JWT로 인증
```

이 방향을 선택하는 이유는 다음과 같다.

- 기존 `JwtAuthenticationFilter`, `JwtTokenProvider`, refresh token 저장 구조를 그대로 재사용할 수 있다.
- provider별 토큰 만료, 재발급, scope 차이를 일반 API 인증 흐름에 섞지 않을 수 있다.
- provider가 추가되어도 서비스 인증 정책은 변하지 않는다.

## Provider 우선순위

초기 구현은 provider 하나만 붙이는 것을 기준으로 한다.


1. Google

Google을 첫 provider로 두는 이유는 Spring Security OAuth2 Client와 문서, 예제가 가장 안정적이기 때문이다. 

provider 확장을 고려해 enum 또는 유사한 타입을 둔다.

```java
public enum OAuthProvider {
    GOOGLE
}
```

## 테이블 기준

테이블 구성은 `docs/tbl_script/tbl`의 DBML 정의를 기준으로 한다.

현재 OAuth 로그인과 직접 관련된 테이블은 다음과 같다.

```text
accounts
- id
- username
- email
- status
- tier
- created_at
- updated_at

oauth_accounts
- id
- account_id
- provider
- provider_user_id
- created_at
- updated_at
```

`oauth_accounts.account_id`는 `accounts.id`를 참조한다.

`accounts`에 `googleId`, `kakaoId`, `naverId` 컬럼을 직접 추가하지 않는다. provider가 늘어날 때마다 회원 테이블이 변경되고, 한 계정이 여러 OAuth 계정을 연결하는 구조로 확장하기 어렵기 때문이다.

`oauth_accounts`에는 email 컬럼이 없으므로 provider에서 받은 email은 계정 생성 시 `accounts.email`의 초기값으로만 사용한다. OAuth 계정의 고유 식별 기준은 email이 아니라 `provider + provider_user_id`이다.

## 로그인 흐름

Spring Security OAuth2 Client 기본 엔드포인트를 우선 사용한다.

```text
GET /oauth2/authorization/{provider}
GET /login/oauth2/code/{provider}
```

예상 흐름:

```text
사용자 OAuth 로그인 요청
-> Spring Security가 provider 인증 페이지로 redirect
-> provider가 authorization code를 callback으로 전달
-> Spring Security가 provider access token 획득
-> provider 사용자 정보 조회
-> oauth_accounts 기준으로 계정 조회
-> 없으면 신규 accounts 및 oauth_accounts 생성
-> 기존 JwtTokenProvider로 access token, refresh token 발급
-> refresh token 저장
-> 클라이언트로 인증 결과 전달
```

## 토큰 전달 정책

최종 방향은 refresh token을 HttpOnly cookie로 전달하는 방식이 좋다.

```text
accessToken: 응답 body 또는 별도 조회 API
refreshToken: HttpOnly, Secure cookie
```

다만 초기 백엔드 구현과 테스트 편의를 위해 아래 방식 중 하나를 임시로 사용할 수 있다.

```text
1. OAuth 성공 후 JSON 응답
2. OAuth 성공 후 프론트 callback URL로 redirect
3. 개발 단계에서만 query parameter로 access token 전달
```

query parameter로 token을 전달하는 방식은 로그, 브라우저 히스토리, referer에 노출될 수 있으므로 운영 정책으로는 사용하지 않는다.

## API 서버 단독 테스트 방향

현재 프로젝트는 별도 화면단 없이 API 서버만 존재한다. 따라서 OAuth 전체 흐름을 순수 API 호출만으로 테스트하기는 어렵다.

Google 로그인과 사용자 동의 화면은 브라우저 기반 redirect 흐름이 필요하다. 대신 OAuth 로그인 성공 이후 발급되는 자체 JWT는 Postman, Swagger, curl 같은 API 클라이언트로 검증한다.

로컬 개발 테스트 흐름은 다음과 같이 둔다.

```text
브라우저에서 OAuth 로그인 시작
GET http://localhost:8080/oauth2/authorization/google

-> Google 로그인/동의
-> Google이 백엔드 callback 호출
GET /login/oauth2/code/google

-> OAuth success handler 실행
-> accounts / oauth_accounts 조회 또는 생성
-> 자체 JWT accessToken 발급
-> 개발용 응답으로 accessToken 반환
```

초기 개발 단계에서는 OAuth 성공 시 access token을 확인 가능한 응답으로 내려준다.

예상 응답:

```json
{
  "grantType": "Bearer",
  "accessToken": "...",
  "refreshToken": "...",
  "accessTokenExpiresIn": 1800
}
```

이 응답에서 받은 access token을 API 클라이언트에 복사해서 보호 API를 호출한다.

```http
Authorization: Bearer {accessToken}
```

검증 기준은 다음과 같다.

```text
access token 없음 -> AUTH-001
잘못된 access token -> AUTH-001
정상 access token -> 보호 API 접근 성공
```

refresh token 저장 여부는 Redis에서 확인한다.

```text
refresh-token:{accountId}
```

확인 항목:

```text
Redis에 refresh token key가 생성되는지
저장된 refresh token이 응답 token과 일치하는지
TTL이 jwt.refresh-token-validity-in-seconds와 맞는지
```

정리하면, 로컬 개발에서는 브라우저로 OAuth 접속 요청을 먼저 수행하고, 성공 후 백엔드가 자체 access token을 응답으로 반환한다. 이후 토큰 유지는 API 클라이언트와 Redis 상태로 확인한다.

운영 단계에서는 refresh token을 응답 body에 노출하지 않고 HttpOnly Secure cookie로 전환하는 방향을 검토한다.

## Success Handler 책임

OAuth 로그인 성공 후 별도 success handler에서 서비스 토큰 발급을 담당한다.

예상 책임:

```text
OAuth2AuthenticationSuccessHandler
-> OAuth2User에서 provider 사용자 정보 추출
-> oauth_accounts 조회 또는 생성
-> accounts 조회 또는 생성
-> 서비스 principal 생성
-> JwtTokenProvider로 access token 생성
-> JwtTokenProvider로 refresh token 생성
-> RefreshTokenRepository에 refresh token 저장
-> 응답 또는 redirect 처리
```

중요한 점은 OAuth용 JWT 발급 로직을 새로 만들지 않는 것이다. 기존 JWT 발급 정책을 재사용해서 로그인 방식만 달라지고 인증 결과는 동일하게 만든다.

## 사용자 식별 기준

OAuth 계정 식별자는 아래 조합으로 판단한다.

```text
provider + provider_user_id
```

email은 provider 설정, 사용자 동의, 계정 상태에 따라 없거나 바뀔 수 있으므로 단독 식별자로 사용하지 않는다.

email은 계정 생성 시 기본값으로 사용할 수 있지만, 계정 연결의 고유 기준은 `provider + provider_user_id`로 둔다.

## 예외 처리 방향

OAuth 로그인 실패는 일반 JWT 인증 실패와 분리해서 다룬다.

예상 케이스:

```text
provider 인증 실패
provider 사용자 정보 조회 실패
필수 사용자 정보 누락
이미 다른 회원에 연결된 OAuth 계정
비활성 회원 로그인 시도
```

API 인증 중 access token이 없거나 유효하지 않은 케이스는 기존 `AUTH-001` 흐름을 유지한다.

## 환경변수

provider별 client 설정은 환경변수로 관리한다.

예상 값:

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GOOGLE_REDIRECT_URI

KAKAO_CLIENT_ID
KAKAO_CLIENT_SECRET
KAKAO_REDIRECT_URI

NAVER_CLIENT_ID
NAVER_CLIENT_SECRET
NAVER_REDIRECT_URI
```

초기에는 실제로 사용할 provider의 값만 필수로 둔다.

## 현재 결정할 것

구현 전에 아래 항목을 먼저 확정한다.

- 첫 OAuth provider는 Google로 확정한다.
- OAuth 성공 후 토큰을 어떤 방식으로 클라이언트에 전달할지
- refresh token을 cookie로 보낼지, 응답 body로 보낼지
- 회원 도메인을 먼저 만들지, OAuth 구현 과정에서 같이 만들지
- OAuth 로그인만 허용할지, 일반 이메일 로그인도 같이 고려할지

## 초기 구현 범위

첫 구현 범위는 다음 정도로 제한한다.

- provider 1개 연동
- OAuth 로그인 성공 처리
- oauth_accounts 기반 accounts 조회 또는 생성
- 기존 JWT 발급 로직 재사용
- refresh token 저장
- OAuth 실패 응답 또는 redirect 처리

아직 구현하지 않을 범위:

- 여러 provider 동시 연동
- 기존 회원과 OAuth 계정 수동 연결
- OAuth 계정 연결 해제
- provider access token 장기 저장
- provider API 대리 호출
