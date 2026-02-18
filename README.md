# auth

`aandiclub.com` 핵심 인증 서비스입니다.

## Local Run (Docker Compose)
```bash
docker compose up --build
```

기본 포트:
- Auth API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## CI/CD Workflow
- CI (`.github/workflows/ci.yml`)
  - Trigger: `main` 브랜치 `push`, `pull_request`
  - Actions: `./gradlew test`, Docker build check, `docker-compose.yml` 유효성 검사

- CD (`.github/workflows/cd.yml`)
  - Trigger: 태그 `vX.Y.Z` 푸시 (예: `v1.2.3`)
  - Actions: OIDC로 AWS Role Assume -> ECR 이미지 푸시 -> SSH로 EC2 배포

## AWS CD Prerequisites
- GitHub Actions Repository Variables
  - `AWS_REGION`: 예) `ap-northeast-2`
  - `ECR_REPOSITORY`: 예) `aandiclub/auth`
  - `APP_DIR`: EC2 내 compose 배포 경로 (기본 `/opt/auth`)
  - `POSTGRES_DB`: 기본 `auth`
  - `POSTGRES_USER`: 기본 `auth`
  - `AWS_PORT`: 기본 `22` (옵션)

- GitHub Actions Repository Secrets
  - `AWS_ROLE_TO_ASSUME`: OIDC 신뢰 정책이 설정된 IAM Role ARN
  - `AWS_HOST`: 배포 대상 EC2 호스트
  - `AWS_USER`: SSH 유저
  - `AWS_SSH_KEY`: SSH 개인키
  - `POSTGRES_PASSWORD`: PostgreSQL 비밀번호
  - `REDIS_PASSWORD`: Redis 비밀번호

- EC2 사전 조건
  - Docker + Docker Compose 설치
  - AWS CLI 설치 및 인스턴스 Role에 ECR Pull 권한 부여
