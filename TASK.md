# TASK.md

## 운영 규칙
- 이 문서는 `aandiclub.com` 인증 서비스 구현의 단일 작업 기준서다.
- 모든 작업은 이 문서의 순서대로 진행한다.
- 각 단계의 `완료 체크`를 기준으로 진행 상태를 관리한다.
- 작업 시작 시 `진행중 체크`를 `[x]`로 바꾸고, 완료 시 `완료 체크`를 `[x]`로 바꾼다.
- 단계 완료 시 `Progress Log`에 날짜(YYYY-MM-DD)와 변경 파일, 테스트 결과를 기록한다.
- 보안 관련 변경은 반드시 테스트(단위/통합 최소 1개 이상)와 함께 반영한다.

## 체크 규칙
- `완료 체크`: 최종 완료 여부 (`[ ]` / `[x]`)
- `진행중 체크`: 현재 작업 중 여부 (`[ ]` / `[x]`)
- `차단 체크`: 외부 이슈로 중단 여부 (`[ ]` / `[x]`)

## 현재 기준선
- [x] `AGENTS.md` 작성 및 한국어 번역본 추가
- [x] 테스트 스택 추가 (`Kotest`, `MockK`)
- [x] 데이터 스택 추가 (`Redis Reactive`, `PostgreSQL R2DBC`)

## 단계별 작업

### T01. 프로젝트 구조/패키지 정리
- 완료 체크: [x]
- 진행중 체크: [ ]
- 차단 체크: [ ]
- 목표:
  - 도메인 중심 패키지 구조 확정 (`auth`, `user`, `admin`, `common`, `security` 등)
  - 예외/응답 포맷 공통 규약 정의
- 산출물:
  - 패키지 스켈레톤
  - 글로벌 예외 처리기
  - 공통 응답 모델
- DoD:
  - 기본 라우팅 + 예외 응답 테스트 통과

### T02. 데이터 모델 및 저장소 구현 (R2DBC)
- 완료 체크: [x]
- 진행중 체크: [ ]
- 차단 체크: [ ]
- 목표:
  - `User` 엔티티/테이블 스키마 확정
  - Reactive Repository 및 기본 조회/저장 구현
- 산출물:
  - 마이그레이션 스크립트(DDL)
  - `User` 모델/리포지토리
- DoD:
  - 저장/조회/유니크 제약 테스트 통과

### T03. 보안 코어 구현 (비밀번호/JWT/권한)
- 완료 체크: [x]
- 진행중 체크: [ ]
- 차단 체크: [ ]
- 목표:
  - BCrypt 기반 비밀번호 인코딩
  - Access/Refresh JWT 발급/검증
  - Role 계층(ADMIN ⊇ ORGANIZER ⊇ USER) 반영
- 산출물:
  - `JwtService`, `PasswordService`, `RolePolicy`
  - JWT 환경변수 설정(`issuer`, `audience`, `secret/key`, `exp`)
- DoD:
  - 토큰 발급/만료/서명오류/권한검증 테스트 통과

### T04. Redis 토큰 상태 관리 구현
- 완료 체크: [x]
- 진행중 체크: [ ]
- 차단 체크: [ ]
- 목표:
  - 로그아웃 Refresh Token 블랙리스트 저장
  - 키 설계: `logout:refresh:{sha256(token)}`
  - TTL = Refresh 만료 잔여시간
- 산출물:
  - Redis TokenState 서비스
  - 해시 유틸리티(원문 저장 금지)
- DoD:
  - logout 후 refresh 거부 시나리오 테스트 통과

### T05. 인증 API 구현 (`/auth/login`, `/auth/refresh`, `/auth/logout`)
- 완료 체크: [x]
- 진행중 체크: [ ]
- 차단 체크: [ ]
- 목표:
  - PRD 명세대로 요청/응답 계약 구현
  - 실패 응답 표준화 (401/403/400)
- 산출물:
  - Auth Controller/Service
  - DTO 검증 로직
- DoD:
  - 정상/실패/악용 시나리오 통합 테스트 통과

### T06. 인가 및 사용자 API 구현 (`/me`, `/admin/**`)
- 완료 체크: [x]
- 진행중 체크: [ ]
- 차단 체크: [ ]
- 목표:
  - Reactive Security Filter Chain 구성
  - `/me`, `/admin/users` 관련 권한 제어 구현
- 산출물:
  - Security Config
  - USER/ADMIN API
- DoD:
  - 역할별 접근 매트릭스 테스트 통과

### T07. 관리자 계정/계정 생성 정책 구현
- 완료 체크: [x]
- 진행중 체크: [ ]
- 차단 체크: [ ]
- 목표:
  - 초기 `admin` 계정 자동 생성
  - `user_01`, `user_02` 증가 규칙 + 동시성 안전 보장
  - 랜덤 32자 비밀번호 생성 후 1회 반환
- 산출물:
  - 부트스트랩 로직
  - username sequence 전략(우선 Redis INCR)
- DoD:
  - 중복 없는 계정 생성 동시성 테스트 통과

### T08. 운영/보안 강화
- 완료 체크: [x]
- 진행중 체크: [ ]
- 차단 체크: [ ]
- 목표:
  - 로그인 시도 제한(rate limiting)
  - 보안 감사 로그(민감정보 마스킹)
  - 메트릭(로그인 실패/토큰 오류/락아웃) 노출
- 산출물:
  - 제한 정책 구현
  - 구조화 로그 및 메트릭 설정
- DoD:
  - 악의적 시나리오 부하 테스트 및 로그 검증 통과

### T09. 문서화/로컬 실행 환경/CI 게이트
- 완료 체크: [x]
- 진행중 체크: [ ]
- 차단 체크: [ ]
- 목표:
  - 로컬 개발 환경(`docker-compose`: postgres, redis) 구성
  - 실행/테스트 가이드 작성
  - CI 최소 게이트(`./gradlew test`) 확정
- 산출물:
  - `docker-compose.yml`
  - `README.md` 또는 운영 문서
  - CI 설정 파일
- DoD:
  - 신규 개발자가 문서만 보고 로컬 부팅 + 테스트 성공

## Progress Log
- 2026-02-18: 초기 `TASK.md` 생성, 단계/DoD/로그 규칙 수립.
- 2026-02-18: T01 완료. 패키지 스켈레톤(`auth/user/admin/security/common`) 추가, 공통 응답 모델/글로벌 예외 처리기 구현, 기본 라우팅(`/api/ping`) + 예외 응답 테스트 추가. 검증: `./gradlew test` 성공.
- 2026-02-18: T02 완료. `UserEntity`/`UserRole`/`UserRepository` 구현, 신규 저장용 `BeforeConvertCallback` 추가, DDL(`db/migration/V1__create_users.sql`) 작성, 저장/조회/유니크 제약 테스트(`UserRepositoryTest`) 추가. 검증: `./gradlew test` 성공.
- 2026-02-18: T03 완료. `PasswordService`(BCrypt), `JwtService`(HS256 Access/Refresh 발급·검증, issuer/audience/exp/type 검증), `RolePolicy`(ADMIN ⊇ ORGANIZER ⊇ USER) 구현 및 단위 테스트(`PasswordServiceTest`, `JwtServiceTest`, `RolePolicyTest`) 추가. JWT 환경변수 설정(`application.yaml`) 반영. 검증: `./gradlew test` 성공.
- 2026-02-18: T04 완료. `TokenHashService`(SHA-256 hex), `RefreshTokenStateService`(키: `logout:refresh:{sha256(token)}`, TTL=refresh 만료 잔여시간, 로그아웃 토큰 거부) 구현 및 테스트(`RefreshTokenStateServiceTest`) 추가. 검증: `./gradlew test` 성공.
- 2026-02-18: T05 완료. 버전 경로 `/v1/auth/*` 기준 `AuthController` + `AuthServiceImpl` 구현(`login`, `refresh`, `logout`), 요청/응답 DTO 및 검증 추가, 입력 파싱/검증 예외의 400 매핑 보강, 인증 API 서비스/컨트롤러 테스트 추가. 검증: `./gradlew test` 성공.
- 2026-02-18: T06 완료. Gateway 1차 필터 전제를 유지하면서 서비스 내부 최소 방어용 Reactive Security Filter Chain 추가(JWT Access 검증), `/v1/me`, `/v1/admin/users` MVC 구현, 역할 접근 매트릭스(401/403/200) 테스트(`AuthorizationMatrixTest`) 추가. 검증: `./gradlew test` 성공.
- 2026-02-18: T07 완료. 초기 admin 부트스트랩(`AdminBootstrapInitializer`, `BootstrapAdminProperties`), Redis INCR 기반 username 시퀀스(`RedisUsernameSequenceService`), 관리자 계정 생성 정책(`POST /v1/admin/users`: `user_XX` + 32자 랜덤 비밀번호 1회 반환) 구현. 관리자 보안 매트릭스 검증용 `/v1/admin/ping` 추가. 테스트(`AdminBootstrapInitializerTest`, `AdminServiceImplTest`, `RedisUsernameSequenceServiceTest`) 추가. 검증: `./gradlew test` 성공.
- 2026-02-18: T08(축소안) 완료. 로그인 제한 제외하고 필수 운영 항목만 반영: 보안 감사로그/메트릭(`SecurityTelemetryService`), 로그인 실패/토큰 검증 실패/refresh 차단 hit 계측, Actuator(health/info/metrics) 노출 및 공개 경로 허용, Swagger/OpenAPI 문서 접근 검증 포함 회귀 테스트 추가. 검증: `./gradlew test` 성공.
- 2026-02-18: T09 완료. `Dockerfile`/`.dockerignore`/`docker-compose.yml` 추가, GitHub Actions CI(`main` PR/Push 시 test + docker build check) 및 CD(`vX.Y.Z` tag push 시 GHCR 이미지 배포) 워크플로우 추가, `README.md` 실행 가이드 작성. 검증: `./gradlew test` 성공.
- 2026-02-18: T09 추가 반영. CD 워크플로우를 AWS 기준으로 전환(`ECR + OIDC + EC2(SSM)`), `docker-compose.yml`의 auth 이미지를 `${AUTH_IMAGE}` 기반으로 변경, AWS 배포 전제 조건을 `README.md`에 문서화.
