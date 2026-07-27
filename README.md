# Fileguard

파일 업로드 시 확장자 기반 차단 정책을 관리하고, 실제 업로드 요청에 서버 사이드 검증을 적용하는 Spring Boot 백엔드 프로젝트다.

## 기술 스택

- Frontend: React 19, Vite 8, Lucide React
- Backend: Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA, Validation
- DB: MySQL 8.4
- Migration: Flyway
- Infra: Docker, Docker Compose
- Test script: PowerShell, curl

## 실행 방법

```powershell
docker compose up -d --build
```

화면과 API 서버는 `http://localhost:8080`에서 함께 실행된다. Docker 빌드 중 React를 먼저 빌드하고 결과물을 Spring Boot 정적 리소스에 포함한다.

MySQL은 로컬 3306 포트 충돌을 피하기 위해 호스트의 `13306` 포트로 노출한다.
Spring Boot 컨테이너는 Docker 내부 네트워크에서 `mysql:3306`으로 접속한다.

```text
host -> localhost:13306
app container -> mysql:3306
```

헬스 체크:

```http
GET /health
```

프론트엔드만 개발 모드로 실행할 때는 Node.js 24 이상을 사용한다. Vite는 `/api`, `/health` 요청을 `localhost:8080`으로 전달한다.

```powershell
cd frontend
npm ci
npm run dev
```

개발 화면은 `http://localhost:5173`에서 확인한다.

## Railway 단일 주소 배포

Railway는 루트의 `Dockerfile`로 React와 Spring Boot를 함께 빌드한다. 생성된 Railway 주소 하나에서 화면, REST API, 파일 다운로드를 모두 제공한다.

```text
Railway 도메인
  -> Spring Boot (React 정적 파일 + REST API)
    -> Railway MySQL
    -> Railway Volume (/app/uploads)
```

### 1. GitHub 저장소 연결

Railway에서 Empty Project를 생성한 후 MySQL 서비스를 추가한다. 같은 프로젝트에 GitHub 저장소를 연결해 애플리케이션 서비스를 생성한다.

Railway는 `railway.json`에 따라 다음 설정을 적용한다.

- 루트 `Dockerfile` 빌드
- `/health` 응답으로 배포 상태 확인
- 실패한 애플리케이션 자동 재시작
- Railway가 제공한 `PORT`로 Spring Boot 실행

### 2. 애플리케이션 변수 설정

애플리케이션 서비스의 Variables에서 `railway-variables.example` 내용을 등록한다.

```dotenv
SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
FILE_STORAGE_DIR=/app/uploads
```

MySQL 서비스 이름이 `MySQL`이 아니라면 `${{MySQL.*}}`의 서비스 이름을 실제 이름으로 변경한다. 공개 TCP 주소가 아닌 같은 Railway 프로젝트의 내부 MySQL 변수를 사용한다.

### 3. 업로드 파일 Volume 연결

애플리케이션 서비스의 Settings에서 Volume을 추가하고 Mount Path를 `/app/uploads`로 지정한다. Volume이 없으면 재배포나 컨테이너 재시작 시 업로드 파일이 사라진다.

### 4. 공개 주소 생성

애플리케이션 서비스의 Settings > Networking에서 Generate Domain을 실행한다. Railway가 발급한 `*.up.railway.app` 주소에서 화면과 API를 함께 확인한다.

배포 시 Flyway가 MySQL 스키마와 고정 확장자 초기 데이터를 자동으로 적용한다. 상태 확인은 Railway 배포 로그와 `/health` 응답을 사용한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1 `
  -BaseUrl https://발급된주소.up.railway.app
```

## DB Schema

Flyway migration 파일: `src/main/resources/db/migration/V1__init.sql`

### extension_policies

확장자 차단 정책 테이블이다.

| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `extension` | 확장자 값, UNIQUE |
| `fixed` | 고정 확장자 여부 |
| `blocked` | 차단 여부 |
| `created_at` | 생성 시각 |
| `updated_at` | 수정 시각 |

고정 확장자 7개는 초기 데이터로 저장한다.

```text
bat, cmd, com, cpl, exe, scr, js
```

고정 확장자는 `fixed=true`이며 삭제할 수 없다. 대신 `blocked` 값을 바꿔 차단 여부를 관리한다.

### policy_change_logs

정책 변경 로그 테이블이다.

| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `action` | ADD, DELETE, TOGGLE, REJECT_DELETE |
| `extension` | 대상 확장자 |
| `policy_id` | 관련 정책 ID |
| `before_blocked` | 변경 전 차단 여부 |
| `after_blocked` | 변경 후 차단 여부 |
| `actor` | 변경 주체, 현재는 system |
| `reason` | 변경 사유 |
| `created_at` | 로그 생성 시각 |

### uploaded_files

실제 파일 업로드 결과 기록 테이블이다.

| 컬럼 | 설명 |
| --- | --- |
| `id` | PK |
| `original_filename` | 원본 파일명 |
| `stored_filename` | 서버 저장 파일명 |
| `extension` | 추출된 확장자 |
| `content_type` | 요청 Content-Type |
| `size` | 파일 크기 |
| `storage_path` | 저장 경로 |
| `status` | STORED 또는 REJECTED |
| `reject_reason` | 거부 사유 |
| `created_at` | 업로드 시각 |

## API 명세

### 확장자 정책 조회

```http
GET /api/extensions
```

응답:

```json
{
  "success": true,
  "data": [
    {
      "id": 5,
      "extension": "exe",
      "fixed": true,
      "blocked": true,
      "createdAt": "2026-07-26T00:00:00",
      "updatedAt": "2026-07-26T00:00:00"
    }
  ],
  "message": null
}
```

### 커스텀 확장자 추가

```http
POST /api/extensions/custom
Content-Type: application/json

{
  "extension": "sh"
}
```

처리 기준:

- `trim`, 소문자 변환, 앞의 점 제거 후 저장한다.
- 최대 20자까지만 허용한다.
- 고정 확장자와 커스텀 확장자의 중복을 막는다.
- 커스텀 확장자는 최대 200개까지 허용한다.
- 중복 요청은 `409 Conflict`로 응답한다.

### 차단 여부 변경

```http
PATCH /api/extensions/{extension}/blocked
Content-Type: application/json

{
  "blocked": true
}
```

고정 확장자와 커스텀 확장자 모두 `blocked` 값을 변경할 수 있다.

### 커스텀 확장자 삭제

```http
DELETE /api/extensions/{id}
```

처리 기준:

- 커스텀 확장자는 삭제한다.
- 고정 확장자는 삭제하지 않고 `FIXED_EXTENSION_NOT_DELETABLE` 오류를 반환한다.

### 파일 업로드

```http
POST /api/files
Content-Type: multipart/form-data

file=@sample.txt
```

처리 기준:

- 빈 파일은 거부한다.
- 원본 파일명은 최대 255자까지 허용한다.
- 점의 위치와 관계없이 마지막 점 뒤의 문자열을 확장자로 판단한다.
- `.env`는 마지막 점 뒤의 `env`를 확장자로 판단한다.
- `.profile.txt`는 `txt` 확장자로 판단한다.
- 확장자가 없는 파일은 확장자 차단 정책으로 유형을 판별할 수 없어 보안상 업로드를 거부한다.
- 업로드 시점에 DB에서 차단 정책을 다시 조회한다.
- 차단된 확장자면 업로드를 거부하고 `REJECTED`로 기록한다.
- 정상 파일은 UUID 기반 저장명으로 저장하고 `STORED`로 기록한다.
- 단일 파일 최대 크기는 10MB, 요청 전체 최대 크기는 20MB로 제한한다.

### 업로드 파일 목록 조회

```http
GET /api/files
```

`STORED` 상태의 파일만 최신 업로드 순으로 반환한다. 프론트엔드는 이 API를 사용해 다운로드 가능한 파일 목록을 표시한다.

### 업로드 파일 다운로드

```http
GET /api/files/{id}/download
```

처리 기준:

- `STORED` 상태인 파일만 다운로드할 수 있다.
- DB의 원본 파일명은 다운로드 이름에만 사용한다.
- 실제 파일 경로는 현재 업로드 디렉터리와 서버 저장명을 기준으로 다시 계산한다.
- 정규화된 경로가 업로드 디렉터리 밖이면 다운로드를 거부한다.
- 응답에 `Content-Disposition: attachment`와 `X-Content-Type-Options: nosniff`를 적용한다.
- DB 기록 또는 실제 파일이 없으면 `FILE_NOT_FOUND`를 반환한다.
