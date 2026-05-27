# Tech Log API Documents

## 1. 개요

Tech Log 백엔드 REST API 명세서이다. 현재 `tech-log-back` 구현을 기준으로 작성하였다.

| 항목 | 내용 |
| --- | --- |
| 로컬 Backend Base URL | `http://localhost:8080` |
| 로컬 Frontend URL | `http://localhost:3000` |
| 데이터 형식 | 기본 `application/json`, 게시글 이미지 업로드 시 `multipart/form-data` |
| 인증 방식 | Session 기반 인증, GitHub OAuth2 또는 관리자 Form Login |
| 날짜 형식 | `LocalDateTime.toString()` 형식, 예: `2026-05-24T21:30:00` |

## 2. 인증 및 권한

| 권한 | 설명 |
| --- | --- |
| Public | 인증 없이 접근 가능 |
| GitHub User | GitHub OAuth 로그인을 완료한 사용자 |
| Admin | 관리자 콘솔 로그인을 완료하여 `ROLE_ADMIN` 권한을 가진 사용자 |

| 대상 | 요구 권한 |
| --- | --- |
| 공개 게시글, 카테고리, 댓글 조회 및 이메일 구독 | Public |
| 현재 사용자 확인 | Public |
| 댓글 작성/수정/삭제 | GitHub User |
| `/api/admin/**` | Admin |
| 관리자 콘솔 화면 | 로그인 화면은 Public, 로그인 후 관리 기능은 Admin |

인증 쿠키가 필요한 프론트 요청은 `credentials: include`를 사용한다.

## 3. 공통 응답

### 3.1 페이지 응답

페이지 조회 API는 다음 형식을 사용한다.

```json
{
  "content": [],
  "page": 0,
  "size": 5,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `content` | `array` | 현재 페이지 데이터 |
| `page` | `number` | 0부터 시작하는 페이지 번호 |
| `size` | `number` | 페이지 크기 |
| `totalElements` | `number` | 전체 데이터 수 |
| `totalPages` | `number` | 전체 페이지 수 |
| `last` | `boolean` | 마지막 페이지 여부 |

### 3.2 오류 응답

```json
{
  "code": "BOARD_404",
  "message": "Post was not found.",
  "status": 404,
  "path": "/api/posts/999",
  "timestamp": "2026-05-24T21:30:00",
  "errors": []
}
```

검증 실패 시 `errors`가 포함된다.

```json
{
  "code": "COMMON_400",
  "message": "Invalid request.",
  "status": 400,
  "path": "/api/posts/1/comments",
  "timestamp": "2026-05-24T21:30:00",
  "errors": [
    {
      "field": "content",
      "message": "크기가 0에서 500 사이여야 합니다"
    }
  ]
}
```

| HTTP Status | Code | 설명 |
| --- | --- | --- |
| `400` | `COMMON_400` | 요청 값 또는 검증 오류 |
| `400` | `IMAGE_400` | 이미지 파일 오류 |
| `401` | `AUTH_401` | 인증 필요 |
| `403` | `AUTH_403` | 접근 권한 없음 |
| `404` | `BOARD_404` | 게시글 없음 |
| `404` | `CATEGORY_404` | 카테고리 없음 |
| `404` | `COMMENT_404` | 댓글 없음 |
| `409` | `CATEGORY_409` | 카테고리명 중복 |
| `409` | `CATEGORY_409_IN_USE` | 사용 중인 카테고리 삭제 요청 |
| `413` | `IMAGE_413` | 게시글 이미지 전체 크기 20MB 초과 |
| `500` | `COMMON_500` | 내부 서버 오류 |

## 4. 응답 모델

### 4.1 PostSummary

```json
{
  "id": 1,
  "title": "Spring Security OAuth2",
  "excerpt": "게시글 내용 요약...",
  "date": "2026-05-24T21:30:00",
  "author": {
    "name": "Sangwon",
    "avatar": "",
    "role": "Owner"
  },
  "category": "Spring",
  "tags": ["Spring", "Security"],
  "coverImage": "/image/example.png",
  "published": true,
  "views": 10
}
```

### 4.2 PostDetail

`PostSummary` 필드에 전체 본문 `content`가 추가된다.

```json
{
  "id": 1,
  "title": "Spring Security OAuth2",
  "excerpt": "게시글 내용 요약...",
  "content": "# 본문",
  "date": "2026-05-24T21:30:00",
  "author": {
    "name": "Sangwon",
    "avatar": "",
    "role": "Owner"
  },
  "category": "Spring",
  "tags": ["Spring", "Security"],
  "coverImage": "/image/example.png",
  "published": true,
  "views": 11
}
```

### 4.3 Category

```json
{
  "id": 1,
  "name": "Spring Boot",
  "slug": "spring-boot"
}
```

### 4.4 Comment

```json
{
  "id": 1,
  "postId": 10,
  "authorName": "octocat",
  "authorAvatar": "https://avatars.githubusercontent.com/u/1",
  "authorGithubUrl": "https://github.com/octocat",
  "content": "좋은 글 감사합니다.",
  "date": "2026-05-24T21:30:00",
  "ownedByCurrentUser": true
}
```

`ownedByCurrentUser`는 현재 GitHub 로그인 사용자가 작성한 댓글인지 나타내며, 비로그인 조회에서는 `false`다. 삭제된 댓글은 공개 댓글 조회 결과에서 제외된다.

## 5. 공개 API

### 5.1 게시글 목록 및 검색

```http
GET /api/posts
```

| 항목 | 내용 |
| --- | --- |
| 인증 | Public |
| 응답 | `200 OK`, `PageResponse<PostSummary>` |
| 정렬 | 수정 시각 내림차순 |

| Query Parameter | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `category` | `string` | N | - | 카테고리명 완전 일치 필터 |
| `q` | `string` | N | - | 제목, 본문, 카테고리명 검색 |
| `page` | `number` | N | `0` | 페이지 번호 |
| `size` | `number` | N | `5` | 페이지 크기, `1` 이상 `50` 이하 |

```http
GET /api/posts?page=0&size=5&category=Spring&q=security
```

### 5.2 게시글 상세 조회

```http
GET /api/posts/{id}
```

| 항목 | 내용 |
| --- | --- |
| 인증 | Public |
| 성공 응답 | `200 OK`, `PostDetail` |
| 실패 응답 | `404 BOARD_404` |
| 비고 | 공개 상세 조회는 조회수를 변경하지 않는다. |

### 5.3 카테고리 전체 조회

```http
GET /api/categories
```

| 항목 | 내용 |
| --- | --- |
| 인증 | Public |
| 성공 응답 | `200 OK`, `Category[]` |

### 5.4 게시글 댓글 조회

```http
GET /api/posts/{postId}/comments
```

| 항목 | 내용 |
| --- | --- |
| 인증 | Public |
| 성공 응답 | `200 OK`, `Comment[]` |
| 정렬 | 작성 시각 오름차순 |
| 비고 | 삭제 처리된 댓글은 반환하지 않는다. |

### 5.5 새 게시글 이메일 구독 신청

```http
POST /api/subscriptions
Content-Type: application/json
```

| 항목 | 내용 |
| --- | --- |
| 인증 | Public |
| 성공 응답 | `202 Accepted` |
| 동작 | 확인 메일을 발송하며, 확인 링크를 연 이후에만 알림 대상으로 등록된다. |

```json
{
  "email": "reader@example.com"
}
```

### 5.6 이메일 구독 취소

메인 화면에서 입력한 이메일로 취소 메일 요청:

```http
POST /api/subscriptions/unsubscribe-request
Content-Type: application/json
```

```json
{
  "email": "reader@example.com"
}
```

메일 링크를 통한 확인 및 취소:

```http
GET /api/subscriptions/confirm?token={confirmationToken}
GET /api/subscriptions/unsubscribe?token={unsubscribeToken}
```

취소 메일의 링크를 연 경우에만 구독 데이터가 삭제된다. 확인/취소 링크 처리가 완료되면 프론트 메인 화면으로 redirect된다.

## 6. 사용자 및 GitHub 인증 API

### 6.1 현재 사용자 조회

```http
GET /api/auth/me
```

| 항목 | 내용 |
| --- | --- |
| 인증 | Public |
| 성공 응답 | `200 OK` |

비로그인 응답:

```json
{
  "authenticated": false,
  "admin": false
}
```

GitHub 사용자 로그인 응답:

```json
{
  "authenticated": true,
  "admin": false,
  "provider": "github",
  "name": "octocat",
  "avatar": "https://avatars.githubusercontent.com/u/1"
}
```

관리자 로그인 응답:

```json
{
  "authenticated": true,
  "admin": true,
  "provider": "admin-console",
  "name": "admin",
  "avatar": ""
}
```

### 6.2 GitHub 로그인 시작

```http
GET /oauth2/authorization/github
```

| 항목 | 내용 |
| --- | --- |
| 인증 | Public |
| 동작 | GitHub OAuth 인증 페이지로 이동 |
| Callback URL | `/login/oauth2/code/github` |
| 성공 시 이동 | `/` |

GitHub OAuth scope:

```text
read:user
user:email
```

### 6.3 댓글 작성

```http
POST /api/posts/{postId}/comments
Content-Type: application/json
```

| 항목 | 내용 |
| --- | --- |
| 인증 | GitHub OAuth 로그인 필요 |
| 성공 응답 | `201 Created`, `Comment` |
| Location Header | `/api/posts/{postId}/comments/{commentId}` |
| 실패 응답 | `401 AUTH_401`, `404 BOARD_404`, `400 COMMON_400` |

요청 Body:

```json
{
  "content": "좋은 글 감사합니다."
}
```

| 필드 | 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| `content` | `string` | Y | 빈 값 불가, 최대 500자 |

댓글 작성 시 GitHub ID, GitHub 로그인명/표시명, 아바타 URL, 접근 IP가 댓글 데이터에 저장된다.

### 6.4 본인 댓글 수정

```http
PUT /api/posts/{postId}/comments/{commentId}
Content-Type: application/json
```

| 항목 | 내용 |
| --- | --- |
| 인증 | GitHub OAuth 로그인 필요 |
| 권한 | 해당 댓글을 작성한 GitHub 사용자만 가능 |
| 성공 응답 | `200 OK`, `Comment` |
| 실패 응답 | `401 AUTH_401`, `403 AUTH_403`, `404 COMMENT_404`, `400 COMMON_400` |

요청 Body는 댓글 작성과 동일하며, `content`는 빈 값 불가 및 최대 500자다.

### 6.5 본인 댓글 삭제

```http
DELETE /api/posts/{postId}/comments/{commentId}
```

| 항목 | 내용 |
| --- | --- |
| 인증 | GitHub OAuth 로그인 필요 |
| 권한 | 해당 댓글을 작성한 GitHub 사용자만 가능 |
| 성공 응답 | `204 No Content` |
| 동작 | 댓글 행은 유지하고 삭제 상태로 변경하며 공개 조회 결과에서는 제외한다. |

## 7. 관리자 인증 및 콘솔

### 7.1 관리자 콘솔 페이지

```http
GET /admin
```

| 항목 | 내용 |
| --- | --- |
| 인증 | Public |
| 동작 | React 관리자 화면이 현재 세션을 확인하여 로그인 폼 또는 관리 콘솔을 렌더링 |
| 호환 경로 | `GET /admin-console`은 프론트엔드 `/admin`으로 Redirect |

관리 기능 API인 `/api/admin/**`의 실제 호출은 별도로 `ROLE_ADMIN` 권한을 검사한다.

### 7.2 관리자 로그인

```http
POST /api/admin/session/login
Content-Type: application/x-www-form-urlencoded
```

| Form Field | 필수 | 설명 |
| --- | --- | --- |
| `adminId` | Y | 관리자 아이디 |
| `adminPassword` | Y | 관리자 비밀번호 |

| 결과 | 동작 |
| --- | --- |
| 성공 | `204 No Content`, React 화면이 세션 상태를 다시 조회 |
| 실패 | `401 AUTH_401` |

관리자 인증 데이터는 `ADMINS` 테이블에 BCrypt 해시로 저장된다. 최초 계정은 테이블이 비어 있을 때 운영 환경 변수로 한 번 생성되며, 로그인 실패가 5회 누적되면 15분 동안 잠긴다.

### 7.3 관리자 로그아웃

```http
POST /api/admin/session/logout
```

| 항목 | 내용 |
| --- | --- |
| 인증 | Public |
| 성공 동작 | 세션을 로그아웃 처리하고 `204 No Content` 반환 |

## 8. 관리자 게시글 API

모든 API는 `ROLE_ADMIN` 권한이 필요하다.

### 8.1 게시글 생성 - JSON

```http
POST /api/admin/posts
Content-Type: application/json
```

| 성공 응답 | `201 Created`, `PostDetail` |
| --- | --- |
| Location Header | `/api/posts/{id}` |

```json
{
  "title": "Spring Security OAuth2",
  "excerpt": null,
  "content": "# 본문",
  "category": "Spring",
  "coverImage": "https://example.com/cover.png",
  "tags": ["Security"],
  "categories": ["Spring"]
}
```

| 필드 | 타입 | 필수 | 제약/설명 |
| --- | --- | --- | --- |
| `title` | `string` | Y | 빈 값 불가, 최대 255자 |
| `excerpt` | `string` | N | 현재 응답 요약은 본문에서 생성됨 |
| `content` | `string` | Y | 빈 값 불가 |
| `category` | `string` | 조건부 | 적어도 하나의 카테고리 필요 |
| `coverImage` | `string` | N | 커버 이미지 URL |
| `tags` | `string[]` | N | 카테고리와 함께 게시글 분류로 저장됨 |
| `categories` | `string[]` | 조건부 | 적어도 하나의 카테고리 필요 |

### 8.2 게시글 생성 - 이미지 첨부

```http
POST /api/admin/posts
Content-Type: multipart/form-data
```

| 성공 응답 | `201 Created`, `PostDetail` |
| --- | --- |
| 이미지 제한 | 모든 이미지 합계 최대 20MB, `image/*` 파일만 허용 |
| 썸네일 | 첫 번째 첨부 이미지 |

| Form Part | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `title` | `string` | Y | 게시글 제목 |
| `content` | `string` | Y | 게시글 본문 |
| `category` | `string` | 조건부 | `categories`가 없을 때 카테고리 |
| `categories` | `string[]` | 조건부 | 다중 카테고리 |
| `tags` | `string` | N | 쉼표 구분 값, 예: `Spring, Security` |
| `images` | `file[]` | N | 이미지 파일 목록 |

본문에서 업로드 이미지 위치를 지정하려면 다음 placeholder 형식을 사용한다.

```text
pending-image:0
pending-image:1
```

첨부 이미지를 본문에서 참조하지 않으면 이미지 Markdown이 본문 끝에 추가된다.

### 8.3 게시글 수정 - JSON

```http
PUT /api/admin/posts/{id}
Content-Type: application/json
```

| 항목 | 내용 |
| --- | --- |
| 요청 Body | 게시글 생성 JSON과 동일 |
| 성공 응답 | `200 OK`, `PostDetail` |
| 실패 응답 | `404 BOARD_404`, `400 COMMON_400` |

### 8.4 게시글 수정 - 이미지 첨부

```http
PUT /api/admin/posts/{id}
Content-Type: multipart/form-data
```

| 항목 | 내용 |
| --- | --- |
| Form Part | 게시글 생성 multipart와 동일 |
| 성공 응답 | `200 OK`, `PostDetail` |
| 비고 | 새 이미지가 첨부된 경우 저장된 게시글 이미지 목록을 교체한다. |

### 8.5 게시글 삭제

```http
DELETE /api/admin/posts/{id}
```

| 항목 | 내용 |
| --- | --- |
| 성공 응답 | `204 No Content` |
| 실패 응답 | `404 BOARD_404` |

## 9. 관리자 카테고리 API

모든 API는 `ROLE_ADMIN` 권한이 필요하다.

### 9.1 카테고리 생성

```http
POST /api/admin/categories
Content-Type: application/json
```

```json
{
  "name": "Spring Boot",
  "slug": "spring-boot"
}
```

| 필드 | 타입 | 필수 | 제약/설명 |
| --- | --- | --- | --- |
| `name` | `string` | Y | 빈 값 불가, 최대 100자, 중복 불가 |
| `slug` | `string` | N | 요청값과 무관하게 응답 slug는 이름에서 생성됨 |

| 항목 | 내용 |
| --- | --- |
| 성공 응답 | `201 Created`, `Category` |
| Location Header | `/api/categories/{id}` |
| 실패 응답 | `400 COMMON_400`, `409 CATEGORY_409` |

### 9.2 카테고리 수정

```http
PUT /api/admin/categories/{id}
Content-Type: application/json
```

```json
{
  "name": "Spring"
}
```

| 항목 | 내용 |
| --- | --- |
| 성공 응답 | `200 OK`, `Category` |
| 실패 응답 | `404 CATEGORY_404`, `409 CATEGORY_409`, `400 COMMON_400` |

### 9.3 카테고리 삭제

```http
DELETE /api/admin/categories/{id}
```

| 항목 | 내용 |
| --- | --- |
| 성공 응답 | `204 No Content` |
| 실패 응답 | `404 CATEGORY_404`, `409 CATEGORY_409_IN_USE` |

## 10. 관리자 댓글 API

모든 API는 `ROLE_ADMIN` 권한이 필요하다.

### 10.1 댓글 관리 목록 조회

```http
GET /api/admin/comments
```

| Query Parameter | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `page` | `number` | N | `0` | 페이지 번호 |
| `size` | `number` | N | `10` | 페이지 크기 |

| 항목 | 내용 |
| --- | --- |
| 성공 응답 | `200 OK`, `PageResponse<AdminComment>` |
| 정렬 | 작성 시각 오름차순 |

```json
{
  "id": 1,
  "postId": 10,
  "postTitle": "Spring Security OAuth2",
  "authorName": "octocat",
  "authorAvatar": "https://avatars.githubusercontent.com/u/1",
  "authorGithubUrl": "https://github.com/octocat",
  "content": "좋은 글 감사합니다.",
  "deleted": false,
  "accessIp": "127.0.0.1",
  "date": "2026-05-24T21:30:00"
}
```

### 10.2 댓글 삭제

```http
DELETE /api/admin/comments/{id}
```

| 항목 | 내용 |
| --- | --- |
| 성공 응답 | `204 No Content` |
| 실패 응답 | `404 COMMENT_404` |
| 비고 | 물리 삭제가 아닌 삭제 상태 처리이다. |

## 11. 관리자 로그 API

모든 API는 `ROLE_ADMIN` 권한이 필요하다.

### 11.1 접근 로그 조회

```http
GET /api/admin/access-logs
```

| Query Parameter | 타입 | 필수 | 기본값 |
| --- | --- | --- | --- |
| `page` | `number` | N | `0` |
| `size` | `number` | N | `20` |

| 성공 응답 | `200 OK`, `PageResponse<AccessLog>` |
| --- | --- |
| 정렬 | 최신 로그 먼저 |

```json
{
  "id": 1,
  "ip": "127.0.0.1",
  "path": "/api/admin/posts/1",
  "method": "PUT",
  "statusCode": 200,
  "timestamp": "2026-05-24T21:30:00",
  "requestId": "55dc2a12-5bcf-4704-a9e1-a42beef533f9",
  "errorType": null,
  "errorMessage": null,
  "stackTrace": null
}
```

접근 로그 저장 대상:

| 저장 대상 | 예시 |
| --- | --- |
| 관리자 변경 요청 | `POST`, `PUT`, `DELETE /api/admin/**` |
| 댓글 작성/수정/삭제 | `POST /api/posts/{postId}/comments`, `PUT`, `DELETE /api/posts/{postId}/comments/{commentId}` |
| 관리자 API 권한 오류 | `/api/admin/**`의 `401`, `403` |
| 서버 오류 | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` 요청의 `5xx` |

정상 공개 `GET`, `/api/auth/me`, 정상 로그 조회 API, `OPTIONS`, `HEAD` 요청은 접근 로그에 저장하지 않는다.
서버 예외로 처리된 `5xx` 로그에는 `requestId`, 예외 타입, 오류 메시지, 최대 8,000자의 stack trace가 함께 저장되며 관리자 콘솔에서만 확인한다.

### 11.2 로그인 로그 조회

```http
GET /api/admin/login-logs
```

| Query Parameter | 타입 | 필수 | 기본값 |
| --- | --- | --- | --- |
| `page` | `number` | N | `0` |
| `size` | `number` | N | `20` |

| 성공 응답 | `200 OK`, `PageResponse<LoginLog>` |
| --- | --- |
| 정렬 | 최신 로그 먼저 |

```json
{
  "id": 1,
  "provider": "github-success",
  "loginId": "octocat",
  "ip": "127.0.0.1",
  "timestamp": "2026-05-24T21:30:00"
}
```

`provider` 값:

| 값 | 의미 |
| --- | --- |
| `admin-console-success` | 관리자 로그인 성공 |
| `admin-console-failure` | 관리자 로그인 실패 |
| `github-success` | GitHub OAuth 로그인 성공 |
| `github-failure` | GitHub OAuth 로그인 실패 |

## 12. 이미지 리소스

업로드된 게시글 이미지는 별도 Controller API가 아니라 정적 리소스 경로로 제공된다.

```http
GET /image/{storedFileName}
```

| 항목 | 내용 |
| --- | --- |
| 인증 | Public |
| 생성 경로 | 관리자 게시글 multipart 생성/수정 요청에서 업로드된 이미지 |

## 13. 엔드포인트 요약

| Method | Path | 권한 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/posts` | Public | 게시글 목록/검색 |
| `GET` | `/api/posts/{id}` | Public | 게시글 상세 조회 |
| `GET` | `/api/categories` | Public | 카테고리 목록 조회 |
| `GET` | `/api/posts/{postId}/comments` | Public | 공개 댓글 조회 |
| `POST` | `/api/subscriptions` | Public | 이메일 구독 확인 메일 요청 |
| `POST` | `/api/subscriptions/unsubscribe-request` | Public | 구독 취소 확인 메일 요청 |
| `GET` | `/api/subscriptions/confirm` | Public | 구독 확인 링크 처리 |
| `GET` | `/api/subscriptions/unsubscribe` | Public | 구독 취소 링크 처리 |
| `GET` | `/api/auth/me` | Public | 현재 사용자 확인 |
| `GET` | `/oauth2/authorization/github` | Public | GitHub 로그인 시작 |
| `POST` | `/api/posts/{postId}/comments` | GitHub User | 댓글 작성 |
| `PUT` | `/api/posts/{postId}/comments/{commentId}` | GitHub User | 본인 댓글 수정 |
| `DELETE` | `/api/posts/{postId}/comments/{commentId}` | GitHub User | 본인 댓글 삭제 처리 |
| `GET` | `/admin` | Public/Admin | React 관리자 콘솔 페이지 |
| `GET` | `/admin-console` | Public | `/admin` 호환 Redirect |
| `POST` | `/api/admin/session/login` | Public | 관리자 로그인 |
| `POST` | `/api/admin/session/logout` | Public | 세션 로그아웃 |
| `POST` | `/api/admin/posts` | Admin | 게시글 생성 |
| `PUT` | `/api/admin/posts/{id}` | Admin | 게시글 수정 |
| `DELETE` | `/api/admin/posts/{id}` | Admin | 게시글 삭제 |
| `POST` | `/api/admin/categories` | Admin | 카테고리 생성 |
| `PUT` | `/api/admin/categories/{id}` | Admin | 카테고리 수정 |
| `DELETE` | `/api/admin/categories/{id}` | Admin | 카테고리 삭제 |
| `GET` | `/api/admin/comments` | Admin | 댓글 관리 목록 조회 |
| `DELETE` | `/api/admin/comments/{id}` | Admin | 댓글 삭제 처리 |
| `GET` | `/api/admin/access-logs` | Admin | 접근 로그 조회 |
| `GET` | `/api/admin/login-logs` | Admin | 로그인 로그 조회 |
| `GET` | `/image/{storedFileName}` | Public | 업로드 이미지 제공 |
