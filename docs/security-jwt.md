# Security / JWT 설계 메모

## JWT 필터 처리 기준

`JwtAuthenticationFilter`는 일반 API 요청에서 access token만 검증한다.

처리 기준은 다음과 같다.

1. access token이 없으면 익명 요청으로 다음 필터에 위임한다.
2. access token이 유효하면 `SecurityContext`에 인증 정보를 설정한다.
3. access token이 유효하지 않으면 `SecurityContext`를 비우고 다음 필터에 위임한다.
4. 보호 API에서 인증 정보가 없으면 Spring Security의 `AuthenticationEntryPoint`를 통해 `AUTH-001` 응답을 반환한다.

## Refresh Token 처리 방향

refresh token은 일반 API 요청 필터에서 자동 검증하지 않는다.

access token이 만료되었거나 유효하지 않은 경우 일반 API는 `AUTH-001`을 반환한다. 클라이언트는 별도 토큰 재발급 API를 호출해야 한다.

예상 흐름은 다음과 같다.

1. 일반 API 요청은 access token만 사용한다.
2. access token 만료 또는 검증 실패 시 `AUTH-001`을 반환한다.
3. 클라이언트는 `/api/v1/auth/refresh` 같은 재발급 API를 호출한다.
4. 재발급 API에서 refresh token을 검증한다.
5. refresh token이 유효하면 새 access token을 발급한다.

이 기준을 두는 이유는 access token 인증과 refresh token 재발급 책임을 분리하기 위해서다. refresh token은 수명이 길고 폐기, 회전, 저장소 검증 정책이 붙을 수 있으므로 일반 API 필터에서 매 요청마다 처리하지 않는다.

## 수정 방향

기존 필터 조건은 토큰 없음과 유효하지 않은 토큰을 하나의 조건문에서 함께 다뤘다.

```java
if (token != null && jwtTokenProvider.validateToken(token)) {
    Authentication authentication = jwtTokenProvider.getAuthentication(token);
    SecurityContextHolder.getContext().setAuthentication(authentication);
}
```

이 구조는 다음 두 케이스를 구분하기 어렵다.

1. 토큰이 없는 요청
2. 토큰은 있지만 유효하지 않은 요청

따라서 필터는 아래 흐름으로 분기한다.

```text
토큰 없음 -> 다음 필터로 위임
토큰 있음 + 유효하지 않음 -> SecurityContext 비움, 다음 필터로 위임
토큰 있음 + 유효함 -> 인증 정보 설정, 다음 필터로 위임
```
