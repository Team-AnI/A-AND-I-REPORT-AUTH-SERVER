# AGENTS.md

## Purpose
- This repository is the core authentication service for `aandiclub.com`.
- All implementation and review work must prioritize security, correctness, and operational stability over delivery speed.

## Tech Context
- Language: Kotlin
- Framework: Spring Boot
- Build: Gradle (`build.gradle.kts`)

## Engineering Principles
- Treat every auth flow as security-critical code.
- Prefer explicitness over convenience in authentication/authorization logic.
- Keep changes small, testable, and auditable.
- Never weaken security controls to pass tests or speed up delivery.

## Security Baseline (Mandatory)
- Validate all external input at boundaries (request DTO, headers, query params).
- Use allowlists over denylists for auth-sensitive fields and values.
- Require strong password hashing (`Argon2id` or `bcrypt` with current recommended cost).
- Never store or log raw passwords, tokens, secrets, or PII.
- Use constant-time comparison for secrets/tokens where applicable.
- Enforce least privilege for roles/permissions and service access.
- Implement rate-limiting / brute-force protection for login and token endpoints.
- Ensure JWT/session handling includes:
  - strict expiration and clock-skew policy
  - key rotation support
  - issuer/audience validation
  - secure signing algorithms only (no `none`, no weak alg fallback)
- Fail closed on authorization checks and token validation errors.
- Return sanitized errors (no stack traces, internals, SQL/auth hints to clients).

## API and Domain Rules
- AuthN and AuthZ logic must live in dedicated service layers, not scattered in controllers.
- Keep security decisions deterministic and centrally testable.
- Avoid hidden magic defaults in security config; configure explicitly.
- Any backward-incompatible auth change requires migration notes in PR description.

## Data and Secrets
- Secrets must come from environment/secret manager, never hardcoded in code or config.
- Do not commit private keys, credentials, or sample real tokens.
- Use minimal retention and masking for security logs.

## Testing Requirements
- Every auth/security change must include tests:
  - unit tests for critical branches and failure modes
  - integration tests for end-to-end auth flows
- Add regression tests for every discovered security bug.
- Validate negative paths explicitly (invalid token, expired token, wrong role, replay).

## Observability and Operations
- Security events must be traceable with safe structured logs (no sensitive payloads).
- Add metrics for login failures, token errors, lockouts, and suspicious patterns.
- Health checks must not leak secret/config details.

## Code Review Checklist
- Are auth boundaries and trust assumptions explicit?
- Are all inputs validated and outputs sanitized?
- Are secrets/tokens handled and logged safely?
- Are authorization checks complete and fail-closed?
- Are tests covering success, failure, and abuse scenarios?
- Does this change introduce timing/oracle/user-enumeration risks?

## Prohibited
- Disabling authentication/authorization in non-test runtime paths.
- Merging auth-impacting code without tests.
- Adding dependencies with unclear maintenance/security posture.

## Delivery Standard
- Prefer secure defaults, measurable behavior, and rollback-safe changes.
- If there is any conflict between product convenience and security guarantees, security wins unless explicitly approved by service owner with documented risk.

---

## 한국어 번역본

## 목적
- 이 저장소는 `aandiclub.com`의 핵심 인증 서비스입니다.
- 모든 구현 및 리뷰 작업은 개발 속도보다 보안, 정확성, 운영 안정성을 우선해야 합니다.

## 기술 맥락
- 언어: Kotlin
- 프레임워크: Spring Boot
- 빌드: Gradle (`build.gradle.kts`)

## 엔지니어링 원칙
- 모든 인증 흐름은 보안 핵심 코드로 취급합니다.
- 인증/인가 로직은 편의성보다 명시성을 우선합니다.
- 변경은 작고, 테스트 가능하며, 감사 가능하게 유지합니다.
- 테스트 통과나 일정 단축을 이유로 보안 통제를 약화하지 않습니다.

## 보안 기준선 (필수)
- 모든 외부 입력은 경계 지점(request DTO, header, query param)에서 검증합니다.
- 인증 민감 필드/값에는 denylist보다 allowlist를 우선 사용합니다.
- 강력한 비밀번호 해시(`Argon2id` 또는 최신 권장 비용의 `bcrypt`)를 사용해야 합니다.
- 원문 비밀번호, 토큰, 비밀값, PII는 저장하거나 로그에 남기지 않습니다.
- 적용 가능한 경우 비밀값/토큰 비교에 constant-time 비교를 사용합니다.
- 역할/권한 및 서비스 접근에 최소 권한 원칙을 적용합니다.
- 로그인/토큰 엔드포인트에 rate-limiting 및 무차별 대입(brute-force) 방어를 적용합니다.
- JWT/세션 처리에는 다음이 반드시 포함되어야 합니다:
  - 엄격한 만료 정책과 clock-skew 정책
  - 키 로테이션 지원
  - issuer/audience 검증
  - 안전한 서명 알고리즘만 허용 (`none` 금지, 약한 알고리즘 fallback 금지)
- 인가 검사 및 토큰 검증 오류 시 fail-closed로 동작해야 합니다.
- 클라이언트에는 정제된 오류만 반환합니다 (stack trace, 내부 정보, SQL/auth 힌트 노출 금지).

## API 및 도메인 규칙
- AuthN/AuthZ 로직은 컨트롤러 전반에 흩어두지 말고 전용 서비스 계층에 위치해야 합니다.
- 보안 결정은 결정적(deterministic)이고 중앙에서 테스트 가능해야 합니다.
- 보안 설정에 숨겨진 매직 기본값을 피하고, 명시적으로 설정합니다.
- 하위 호환이 깨지는 인증 변경은 PR 설명에 마이그레이션 노트를 반드시 포함합니다.

## 데이터 및 비밀 관리
- 비밀값은 코드/설정 하드코딩이 아닌 환경 변수 또는 시크릿 매니저에서 주입해야 합니다.
- 개인키, 자격증명, 실제 토큰 샘플을 커밋하지 않습니다.
- 보안 로그는 최소 보관 및 마스킹 정책을 적용합니다.

## 테스트 요구사항
- 모든 인증/보안 변경에는 다음 테스트가 포함되어야 합니다:
  - 핵심 분기 및 실패 모드에 대한 단위 테스트
  - 종단 간 인증 흐름에 대한 통합 테스트
- 발견된 모든 보안 버그에는 회귀(regression) 테스트를 추가합니다.
- 부정 경로(잘못된 토큰, 만료 토큰, 잘못된 권한, 재전송 공격)를 명시적으로 검증합니다.

## 관측성 및 운영
- 보안 이벤트는 민감정보 없는 안전한 구조화 로그로 추적 가능해야 합니다.
- 로그인 실패, 토큰 오류, 계정 잠금, 이상 패턴에 대한 메트릭을 추가합니다.
- 헬스체크는 비밀값/설정 상세를 노출해서는 안 됩니다.

## 코드 리뷰 체크리스트
- 인증 경계와 신뢰 가정이 명시되어 있는가?
- 모든 입력이 검증되고 출력이 정제되는가?
- 비밀값/토큰이 안전하게 처리 및 로깅되는가?
- 인가 검사가 누락 없이 fail-closed로 동작하는가?
- 테스트가 성공/실패/악용 시나리오를 커버하는가?
- 이 변경이 timing/oracle/user-enumeration 위험을 유발하는가?

## 금지 사항
- 테스트 외 런타임 경로에서 인증/인가를 비활성화하는 행위.
- 테스트 없이 인증 영향 코드를 병합하는 행위.
- 유지보수/보안 상태가 불명확한 의존성을 추가하는 행위.

## 전달 기준
- 안전한 기본값, 측정 가능한 동작, 롤백 가능한 변경을 우선합니다.
- 제품 편의성과 보안 보장이 충돌할 경우, 서비스 오너가 위험을 문서화하여 명시 승인하지 않는 한 보안을 우선합니다.
