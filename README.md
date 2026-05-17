# 게임 플랫폼 공통 서비스 API 서버

게임 서비스에서 반복적으로 필요한 인증, 계정, 빌링, 운영 기능을 공통 API로 분리한 Spring Boot 기반 백엔드 포트폴리오 프로젝트입니다.

본 프로젝트는 일반 유저 기능과 관리자 기능을 역할에 따라 분리하고, OAuth 로그인, 관리자 로그인, 계정 상태 관리, 상품 조회, 구매, 청약철회, 감사 로그 조회 기능을 하나의 공통 서비스 API 서버로 구성하는 것을 목표로 합니다.

---

## 1. 프로젝트 개요

### 프로젝트 목적

* 게임 서비스에서 공통적으로 필요한 인증, 계정, 빌링, 운영 기능을 API 서버로 분리
* 일반 유저와 관리자 기능을 역할 기반으로 분리
* JWT 기반 인증/인가 구조 구현
* OAuth 계정 연동과 관리자 계정 인증 구조 분리
* 구매, 청약철회, 감사 로그 등 운영 관점의 핵심 기능 구현
* AWS 기반 배포 구조와 CI/CD 흐름 구성

### 주요 기능

* 유저 OAuth 로그인
* 관리자 폼 로그인
* JWT 기반 인증
* USER / ADMIN 역할 기반 권한 분리
* 유저 계정 조회 및 닉네임 수정
* 외부 OAuth 계정 추가 연동
* 관리자 계정 상태 변경
* 캐시 아이템 조회
* 캐시 아이템 구매 요청
* 아이템 구매 이력 조회
* 청약철회 요청
* 관리자용 결제 내역 조회
* 감사 로그 조회

---

## 2. 기술 스택

| 구분        | 기술                                                   |
| --------- | ---------------------------------------------------- |
| Language  | Java 21 LTS                                          |
| Framework | Spring Boot                                          |
| Database  | MySQL                                                |
| Cache     | Redis                                                |
| ORM       | Spring Data JPA                                      |
| Security  | Spring Security, OAuth2, JWT                         |
| API Docs  | OpenAPI / Swagger UI                                 |
| Infra     | AWS EC2, AWS RDS, VPC, Public Subnet, Private Subnet |
| DevOps    | GitHub Actions, Docker Compose, Terraform            |

---

## 3. 인프라 구조

```text
AWS VPC
├─ Public Subnet
│  └─ EC2
│     ├─ Spring Boot Application
│     └─ Redis
│
└─ Private Subnet
   └─ RDS MySQL
```

### 구성 방향

* AWS VPC 내부에 Public Subnet과 Private Subnet 구성
* Public Subnet에는 EC2 배치
* EC2 내부에서 Docker Compose로 Spring Boot 애플리케이션과 Redis 실행
* RDS는 Private Subnet에 배치하여 외부 직접 접근 차단
* Spring Boot 애플리케이션만 RDS에 접근하도록 구성

---

## 4. 배포 플로우

```text
Local PC → Git Push → GitHub Actions → Docker Compose 실행 → Spring Boot 구동 완료
```

### CI/CD 흐름

1. Local PC에서 개발 후 GitHub Repository에 push
2. GitHub Actions Workflow 실행
3. 빌드 및 테스트 수행
4. 빌드/테스트 성공 시 AWS EC2 배포 진행
5. EC2에서 Docker Compose 실행
6. Spring Boot 애플리케이션 구동 완료

---

## 5. 도메인 구성

### 인증 / 인가

* OAuth 기반 유저 로그인
* 관리자 username/password 기반 폼 로그인
* JWT Access Token / Refresh Token 발급
* USER / ADMIN 역할 기반 권한 분리

### 계정

* 유저 계정 조회
* 유저 닉네임 수정
* 외부 OAuth 계정 추가 연동
* 관리자 계정 상태 변경

계정 상태는 다음과 같이 관리합니다.

| 코드 | 상태        |
| -- | --------- |
| 0  | ACTIVE    |
| 1  | DORMANT   |
| 2  | SUSPENDED |
| 3  | DELETED   |

### 빌링

* 캐시 아이템 조회
* 캐시 아이템 구매 요청
* 구매 이력 조회
* 청약철회 요청

구매 상태는 다음과 같이 관리합니다.

| 상태               | 설명      |
| ---------------- | ------- |
| PENDING          | 결제 대기   |
| PAID             | 결제 완료   |
| FAILED           | 결제 실패   |
| CANCELED         | 결제 취소   |
| REFUND_REQUESTED | 청약철회 요청 |
| REFUNDED         | 환불 완료   |

### 운영

* 관리자용 계정 상태 변경
* 관리자용 결제 내역 조회
* 감사 로그 조회

---

## 6. 주요 테이블

| 테이블               | 설명                        |
| ----------------- | ------------------------- |
| accounts          | 서비스 내부 유저 계정 정보           |
| oauth_accounts    | 외부 OAuth 제공자와 내부 계정 연결 정보 |
| admin_credentials | 관리자 로그인 정보                |
| products          | 캐시 아이템 상품 정보              |
| purchases         | 유저 상품 구매 이력               |
| audit_logs        | 관리자 작업 및 주요 운영 이벤트 기록     |

### audit_logs 설계 방향

`audit_logs`는 다양한 도메인의 운영 이벤트를 기록하기 위해 강한 FK 대신 `actor_type / actor_id`, `target_type / target_id` 조합을 사용하는 다형성 로그 구조로 설계합니다.

---

## 7. API 목록

| 기능          | Method | URL                                              | 설명                |
| ----------- | ------ | ------------------------------------------------ | ----------------- |
| OAuth 로그인   | POST   | `/api/v1/auth/oauth/login`                       | 유저 OAuth 로그인      |
| 관리자 로그인     | POST   | `/api/v1/auth/admin/login`                       | 관리자 폼 로그인         |
| 계정 조회       | GET    | `/api/v1/admin/users/{userId}`                   | 관리자용 유저 정보 조회     |
| 유저 정보 수정    | PUT    | `/api/v1/users/{userId}`                         | 유저 닉네임 수정         |
| OAuth 계정 연동 | POST   | `/api/v1/users/{userId}/oauth-accounts`          | 외부 OAuth 계정 추가 연동 |
| 캐시 아이템 조회   | GET    | `/api/v1/products`                               | 캐시 아이템 목록 조회      |
| 구매 요청       | POST   | `/api/v1/purchases`                              | 캐시 아이템 구매 요청      |
| 구매 이력 조회    | GET    | `/api/v1/purchases`                              | 유저 구매 이력 조회       |
| 청약철회 요청     | POST   | `/api/v1/purchases/{purchaseId}/refund-requests` | 구매 건 청약철회 요청      |
| 계정 상태 변경    | PATCH  | `/api/v1/admin/users/{userId}/status`            | 관리자용 계정 상태 변경     |
| 결제 내역 조회    | GET    | `/api/v1/admin/purchases`                        | 관리자용 결제 내역 조회     |
| 감사 로그 조회    | GET    | `/api/v1/admin/audit-logs`                       | 관리자용 감사 로그 조회     |

---

## 8. 공통 응답 포맷

### 성공 응답

```json
{
  "code": "SUCCESS",
  "message": "OK",
  "httpStatus": 200,
  "data": {}
}
```

### 에러 응답

```json
{
  "code": "ERR-001",
  "message": "BAD REQUEST",
  "httpStatus": 400
}
```

---

## 9. 에러 코드

| Code        | HTTP Status | Message                      |
| ----------- | ----------: | ---------------------------- |
| ERR-001     |         400 | BAD REQUEST                  |
| ERR-003     |         500 | INTERNAL SERVER ERROR        |
| ERR-004     |         999 | UNKNOWN ERROR                |
| AUTH-001    |         401 | AUTHENTICATION FAILED        |
| AUTH-002    |         403 | FORBIDDEN                    |
| ACCOUNT-001 |         404 | ACCOUNT NOT FOUND            |
| ACCOUNT-002 |         400 | OAUTH ACCOUNT ALREADY LINKED |
| BILLING-001 |         404 | NO ITEM                      |
| BILLING-002 |         400 | PRODUCT NOT AVAILABLE        |
| REFUND-001  |         404 | NO PURCHASE HISTORY          |
| REFUND-002  |         400 | ALREADY REFUNDED             |
| REFUND-003  |         400 | REFUND PERIOD EXPIRED        |
| REFUND-004  |         400 | USED ITEM                    |
| ADMIN-001   |         404 | NO TARGET USER               |
| ADMIN-002   |         400 | CAN'T CHANGE ACCOUNT STATUS  |

---

## 10. 패키지 구조

```text
com.example.gameplatform
├─ global
│  ├─ common
│  │  ├─ response
│  │  └─ error
│  ├─ exception
│  └─ security
│     ├─ jwt
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

---

## 11. 실행 방법

### 사전 준비

* Java 21
* Docker
* Docker Compose
* MySQL
* Redis

### 로컬 실행

```bash
./gradlew bootRun
```

### 테스트 실행

```bash
./gradlew test
```

### 빌드

```bash
./gradlew clean build
```

### Docker Compose 실행

```bash
docker compose up -d
```

---

## 12. 구현 우선순위

1. Spring Boot 프로젝트 생성
2. 기본 패키지 구조 생성
3. 공통 응답 DTO 작성
4. 공통 에러코드 enum 작성
5. 공통 예외 / 예외 핸들러 작성
6. Entity / Repository 작성
7. Security / JWT 기본 구조 작성
8. 관리자 로그인 구현
9. OAuth 로그인 구현
10. 계정 조회 / 유저 닉네임 수정
11. OAuth 계정 연동
12. 계정 상태 변경
13. 캐시 아이템 조회
14. 구매 요청
15. 구매 이력 조회
16. 청약철회
17. 관리자 결제 내역 조회
18. 감사 로그 조회

---

## 13. 커밋 메시지 규칙

| 태그             | 설명                             |
| -------------- | ------------------------------ |
| `[feature :]`  | 기능 개발                          |
| `[fix :]`      | 버그 수정                          |
| `[refactor :]` | 리팩토링                           |
| `[docs :]`     | 문서 수정                          |
| `[test :]`     | 테스트 코드                         |
| `[style :]`    | 코드 포맷팅, 오타, 세미콜론 등 로직 변화 없는 수정 |
| `[chore :]`    | 기타 작업                          |

### 예시

```bash
git commit -m "[feature :] 프로젝트 초기 설정 및 공통 응답 구조 추가"
git commit -m "[feature :] 관리자 로그인 API 기본 구조 구현"
```

---

## 14. 현재 진행 상태

* [x] 프로젝트 방향 정리
* [x] ERD 주요 테이블 정리
* [x] API 목록 정리
* [x] 에러 코드 초안 정리
* [x] Spring Boot 프로젝트 생성
* [x] Git 원격 저장소 연결
* [x] `git pull origin main` 완료
* [ ] 기본 패키지 구조 생성
* [ ] 공통 응답 DTO 작성
* [ ] 공통 에러코드 enum 작성
* [ ] 공통 예외 / 예외 핸들러 작성
* [ ] 관리자 로그인 API 기본 구조 구현

---

## 15. 후순위 기능

* 공지 관리
* 이벤트 관리
* 알림 기능
* ELK 로그 수집
* Kafka 메시지 브로커
