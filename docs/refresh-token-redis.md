# Refresh Token Redis 저장 정책

## 목적

Refresh Token은 Redis에 저장한다.

서버는 클라이언트가 보낸 refresh token이 해당 계정의 최신 유효 토큰인지 Redis에 저장된 값과 비교해서 판단한다.

초기 버전에서는 단일 세션 정책을 사용한다. 하나의 subject는 한 번에 하나의 refresh token만 가질 수 있다.

## Key 형식

```text
refresh-token:{subject}
```

예시:

```text
refresh-token:1
refresh-token:admin-1
```

subject가 다르면 Redis key도 다르기 때문에 서로 덮어쓰지 않는다.

```text
refresh-token:1 -> 1번 사용자 refresh token
refresh-token:2 -> 2번 사용자 refresh token
```

덮어쓰기는 같은 subject가 다시 로그인할 때만 발생한다.

```text
refresh-token:1 -> 기존 토큰
refresh-token:1 -> 새 토큰
```

이 경우 기존 refresh token은 더 이상 Redis에 저장된 값과 일치하지 않으므로 무효화된다.

## TTL

Redis TTL은 기존 JWT 설정값을 그대로 사용한다.

```yaml
jwt:
  refresh-token-validity-in-seconds: 1209600
```

refresh token을 저장할 때 JWT refresh token 만료 시간과 같은 TTL을 Redis에 설정한다.

## 구현 컴포넌트

`RefreshTokenRepository`가 Redis 접근을 담당한다.

현재 제공하는 기능:

- `save(subject, refreshToken)`: refresh token을 TTL과 함께 저장한다.
- `findBySubject(subject)`: subject 기준으로 저장된 refresh token을 조회한다.
- `matches(subject, refreshToken)`: 요청 token과 Redis에 저장된 token이 같은지 확인한다.
- `delete(subject)`: subject 기준으로 저장된 refresh token을 삭제한다.

## 로그인 흐름

예정된 로그인 흐름:

```text
로그인 성공
-> access token 생성
-> refresh token 생성
-> refresh-token:{subject} key로 Redis에 refresh token 저장
-> access token과 refresh token 응답
```

## 재발급 흐름

예정된 token 재발급 흐름:

```text
재발급 요청
-> refresh token 서명과 만료 시간 검증
-> refresh token에서 subject 추출
-> Redis의 refresh-token:{subject} 값과 요청 refresh token 비교
-> 일치하면 새 access token 발급
```

## 로그아웃 흐름

예정된 로그아웃 흐름:

```text
로그아웃 요청
-> subject 식별
-> refresh-token:{subject} 삭제
```

## 현재 범위

구현됨:

- Redis key 정책
- Redis TTL 저장
- refresh token 조회
- refresh token 일치 여부 확인
- refresh token 삭제
- 실제 Redis 서버 없이 동작하는 단위 테스트

아직 미구현:

- 로그인 API 연동
- refresh API 연동
- 로그아웃 API 연동
- 다중 기기 세션 지원

다중 기기 로그인이 필요해지면 key 형식을 확장한다.

```text
refresh-token:{subject}:{deviceId}
```
