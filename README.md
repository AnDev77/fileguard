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
- 확장자가 없는 파일은 거부한다.
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

## 프론트엔드 동작

- 고정 확장자와 커스텀 확장자를 분리해 표시한다.
- 커스텀 확장자 추가·삭제와 고정 확장자 차단 상태 변경 후 알림을 표시한다.
- 파일 업로드 성공과 차단 확장자 오류를 구분해 알림으로 표시한다.
- 업로드한 파일 목록에서 원본 파일명으로 다운로드할 수 있다.
- 정책과 파일 목록은 5초마다 다시 조회한다.
- 창 포커스 복귀, 브라우저 탭 복귀, 수동 새로고침 시 즉시 DB 상태를 다시 조회한다.
- 브라우저를 새로고침해도 화면 상태를 로컬에 임시 저장하지 않고 DB 값을 기준으로 복원한다.

## 공통 오류 응답

```json
{
  "code": "BLOCKED_FILE_EXTENSION",
  "message": "exe extension is blocked by upload policy. recordId: 2",
  "details": [],
  "timestamp": "2026-07-26T00:00:00Z"
}
```

주요 오류 코드:

| 코드 | 상황 |
| --- | --- |
| `INVALID_EXTENSION` | 확장자 입력 형식 오류 |
| `EXTENSION_ALREADY_EXISTS` | 중복 확장자 추가 |
| `CUSTOM_EXTENSION_LIMIT_EXCEEDED` | 커스텀 확장자 200개 초과 |
| `FIXED_EXTENSION_NOT_DELETABLE` | 고정 확장자 삭제 시도 |
| `BLOCKED_FILE_EXTENSION` | 차단 확장자 파일 업로드 |
| `FILE_EXTENSION_REQUIRED` | 확장자 없는 파일 업로드 |
| `FILENAME_TOO_LONG` | 파일명 255자 초과 |
| `FILE_SIZE_EXCEEDED` | 파일 크기 10MB 초과 |
| `FILE_NOT_FOUND` | 다운로드할 파일 기록 또는 실제 파일 없음 |

## 고려사항

### 1. 검증/보안 관점

확장자만으로 파일의 실제 내용을 완전히 신뢰할 수는 없다. 예를 들어 `report.jpg`라는 이름의 파일이 실제로는 실행 파일일 수 있다. 이번 구현은 과제의 핵심 요구사항인 확장자 기반 차단을 우선 구현했고, MIME 타입 검증과 매직 넘버 검증은 향후 보강 항목으로 남겼다.

클라이언트 검증만으로는 우회가 가능하므로 업로드 API에서 서버 사이드 검증을 수행한다. 업로드 시점마다 DB에서 최신 차단 정책을 조회해 화면 상태와 실제 정책이 어긋나는 문제를 줄였다.

파일명은 보안상 그대로 저장하지 않는다. 원본 파일명에는 경로 조작 문자, 긴 문자열, 중복 이름이 포함될 수 있으므로 서버 저장명은 UUID 기반으로 생성한다.

확장자 추출은 마지막 점을 기준으로 한다. 따라서 `.env`는 `env` 확장자 파일이고, `.profile.txt`는 `txt` 확장자 파일이다.

### 2. 정책/데이터 관점

고정 확장자와 커스텀 확장자는 하나의 `extension_policies` 테이블에서 관리한다. `fixed` 컬럼으로 고정 여부를 구분하고, `blocked` 컬럼으로 실제 차단 여부를 관리한다. 이 구조는 고정 확장자를 삭제하지 않으면서 차단 여부만 바꿀 수 있어 확장성이 있다.

고정 확장자와 커스텀 확장자의 중복은 서비스 검증과 DB UNIQUE 제약으로 함께 막는다. 애플리케이션에서 먼저 중복을 확인하고, 동시에 같은 요청이 들어오는 경우에는 DB의 `UNIQUE (extension)` 제약이 최종 방어선이 된다.

정책 변경 로그는 `policy_change_logs`에 남긴다. 현재는 로그인 기능이 없으므로 `actor`는 `system`으로 기록한다. 실제 운영 환경에서는 로그인한 관리자 ID를 기록해 누가 언제 무엇을 바꿨는지 감사할 수 있다.

### 3. UX/예외 관점

차단 또는 실패 시 단순 실패가 아니라 이유를 구분할 수 있는 오류 코드를 반환한다. 예를 들어 중복 확장자는 `EXTENSION_ALREADY_EXISTS`, 고정 확장자 삭제 시도는 `FIXED_EXTENSION_NOT_DELETABLE`, 파일 크기 초과는 `FILE_SIZE_EXCEEDED`로 응답한다.

커스텀 확장자는 최대 20자, 최대 200개로 제한한다. 제한을 넘는 경우 서버에서 오류를 반환하고, 프론트엔드에서는 이 값을 이용해 입력 제한과 안내 메시지를 제공할 수 있다.

### 4. 운영 관점

동시성은 DB UNIQUE 제약으로 방어한다. 같은 확장자를 병렬로 추가하는 테스트에서 10개 요청 중 1개만 성공하고 9개는 `409 Conflict`로 응답하는 것을 확인했다.

업로드 기록은 성공과 실패를 모두 남긴다. 정상 업로드는 `STORED`, 차단 또는 검증 실패는 `REJECTED`로 저장한다. 이를 통해 운영 중 어떤 파일이 왜 거부되었는지 확인할 수 있다.

향후 운영 환경에서는 사용자별 정책, 화이트리스트 방식, 압축 파일 내부 검사, MIME/매직 넘버 검증, 다운로드 보안 헤더, 관리자 인증/인가를 추가할 수 있다.

## 테스트

### 동시성 테스트

같은 확장자를 10개 병렬 요청으로 추가한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-extension-concurrency.ps1
```

확인 결과:

```text
status=200 count=1
status=409 count=9
```

### 파일 크기 제한 테스트

1MB 파일과 11MB 파일을 업로드한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-file-size-limit.ps1
```

기대 결과:

```text
1MB 파일  -> 200 OK, STORED
11MB 파일 -> 413 Payload Too Large, FILE_SIZE_EXCEEDED
```

### 파일명/확장자 테스트

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-upload-cases.ps1
```

기대 결과:

```text
ok.txt        -> 성공
.profile.txt -> 성공
.env          -> env 확장자 정책에 따라 허용 또는 차단
noextension   -> FILE_EXTENSION_REQUIRED
긴 파일명     -> FILENAME_TOO_LONG
```

### 프론트엔드 및 다운로드 통합 테스트

확인 결과:

```text
React production build             -> 성공
npm audit                          -> 취약점 0건
Spring Boot 단위 테스트/패키징     -> 성공
차단된 exe 업로드                  -> 400 BLOCKED_FILE_EXTENSION
정상 파일 업로드 후 목록 조회      -> 업로드 ID 확인
다운로드 파일 SHA-256 비교         -> 원본과 일치
커스텀 확장자 추가/삭제 알림       -> 표시 확인
다른 화면의 정책 변경 반영         -> 자동 조회 및 새로고침 확인
다운로드 버튼 알림                 -> 표시 확인
브라우저 콘솔 오류                 -> 0건
```

## 개발 기록

- [구현 고려사항](docs/CONSIDERATIONS.md)
- [AI 프롬프트 기록](docs/PROMPT_LOG.md)
