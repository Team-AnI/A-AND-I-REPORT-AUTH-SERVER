1. 개요
   1.1 목적

본 서비스는 Spring Boot WebFlux 기반의 비동기 인증/인가 서비스로,
JWT 기반 Access/Refresh Token 발급 및 권한 기반 접근 제어(Role-Based Access Control)를 제공한다.

1.2 기술 스택

Framework: Spring Boot (WebFlux)

Security: Spring Security (Reactive)

DB: PostgreSQL (R2DBC)

Cache / Token State: Redis

Token: JWT (HMAC 또는 RSA)

Architecture: MSA 기반 독립 인증 서버

2. 핵심 요구사항
3. 권한 체계 (Role System)
   3.1 권한 종류
   Role	설명
   ADMIN	모든 API 접근 가능
   ORGANIZER	USER가 접근 불가능한 운영 API까지 접근 가능
   USER	일반 사용자 API만 접근 가능
   3.2 권한 계층 구조

권한은 계층형 구조를 따른다.

ADMIN ⊇ ORGANIZER ⊇ USER


즉,

USER API → USER / ORGANIZER / ADMIN 모두 접근 가능

ORGANIZER 전용 API → ORGANIZER / ADMIN 접근 가능

ADMIN 전용 API → ADMIN만 접근 가능

4. 인증 정책
   4.1 토큰 구조
   Access Token

형식: JWT

만료: 1시간

포함 정보:

userId

username

role

issuedAt (iat)

expiration (exp)

(선택) jti

용도:

API 접근 인증

권한 기반 접근 제어

Refresh Token

만료: 60일 (2개월)

용도:

Access Token 재발급

상태 관리:

Redis에 저장되는 경우 “로그아웃된 토큰”으로 간주

5. 로그아웃 정책
   5.1 동작 원리

로그아웃 시, Refresh Token을 Redis에 저장

이후 Refresh 요청 시:

Redis에 존재하면 → 거부 (401)

존재하지 않으면 → 정상 처리

5.2 Redis 키 설계

Key:

logout:refresh:{sha256(refreshToken)}


Value:

1 (또는 logout timestamp)


TTL:

refreshToken 만료 시점까지 남은 시간

6. 사용자 정책
   6.1 회원가입

회원가입 기능은 존재하지 않음

모든 계정은 ADMIN에 의해 생성됨

6.2 기본 관리자 계정

username: admin

role: ADMIN

서비스 초기화 시 자동 생성

모든 관리 책임 보유

6.3 계정 생성 정책

ADMIN은 새로운 계정을 생성할 수 있다.

생성 규칙

username 형식:

user_01
user_02
user_03
...


숫자는 1씩 증가

비밀번호:

32자 랜덤 문자열

생성 즉시 응답으로 반환

이후 오프라인 전달

6.4 사용자 정보 수정

USER는 자신의 정보 수정 가능

추가 검증 절차 없음

문제 발생 시 운영진이 수동 처리

7. 데이터 저장 구조
   7.1 PostgreSQL (영구 데이터)
   User 테이블
   필드	타입	설명
   id	UUID	PK
   username	VARCHAR	unique
   password_hash	VARCHAR	암호화 저장
   role	ENUM	ADMIN / ORGANIZER / USER
   created_at	TIMESTAMP	생성일
   updated_at	TIMESTAMP	수정일
   7.2 Username 시퀀스

동시성 안전을 위해 다음 방식 중 하나 사용:

Redis INCR user_seq (권장)

별도 시퀀스 테이블

DB auto increment 기반 계산

8. API 명세
   8.1 인증 API
   POST /auth/login

Request:

{
"username": "string",
"password": "string"
}


Response:

{
"accessToken": "string",
"refreshToken": "string",
"expiresIn": 3600,
"tokenType": "Bearer",
"user": {
"id": "uuid",
"username": "string",
"role": "USER"
}
}

POST /auth/refresh

Request:

{
"refreshToken": "string"
}


처리:

토큰 유효성 검사

Redis 블랙리스트 조회

존재하면 401

Response:

{
"accessToken": "string",
"expiresIn": 3600
}

POST /auth/logout

Request:

{
"refreshToken": "string"
}


처리:

Redis에 refreshToken hash 저장

Response:

{
"success": true
}

8.2 USER API
GET /me

인증 필요

PATCH /me

본인 정보 수정

8.3 ADMIN API
POST /admin/users

설명: 계정 생성

Response:

{
"id": "uuid",
"username": "user_07",
"password": "random32string",
"role": "USER"
}

PATCH /admin/users/{id}

설명:

사용자 정보 수정

권한 변경 가능

GET /admin/users

사용자 목록 조회

9. 인가 정책 매핑
   API	USER	ORGANIZER	ADMIN
   /auth/**	O	O	O
   /me	O	O	O
   ORGANIZER 전용 API	X	O	O
   /admin/**	X	X	O
10. 보안 정책

비밀번호는 BCrypt 해시 저장

JWT Secret은 환경변수로 관리

로그에 토큰 원문 기록 금지

Redis에는 토큰 원문 대신 해시 사용 권장

11. 비기능 요구사항

모든 API는 Non-blocking (WebFlux)

Stateless 인증 구조

확장 가능한 MSA 구조

고가용성을 고려한 Redis 사용

12. 향후 확장 고려 사항 (Optional)

Refresh Token Rotation

권한 세분화 (Permission 기반)

Audit 로그 저장

계정 활성/비활성 상태 관리

로그인 시도 제한