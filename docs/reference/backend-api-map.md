# Backend API Map

Tai lieu nay map cac endpoint hien co trong backend va cho biet frontend hien dang noi that vao dau.

## 1. Account Service

Public/API:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/me/signature-image`
- `GET /api/v1/auth/me/signature-image`
- `DELETE /api/v1/auth/me/signature-image`
- `PATCH /api/v1/auth/me/signature-appearance`
- `GET /api/v1/auth/active-users`
- `GET /api/v1/admin/accounts`
- `GET /api/v1/admin/accounts/{accountId}`
- `PATCH /api/v1/admin/accounts/{accountId}/activate`
- `PATCH /api/v1/admin/accounts/{accountId}/lock`
- `PATCH /api/v1/admin/accounts/{accountId}/unlock`
- `PATCH /api/v1/admin/accounts/{accountId}/disable`
- `PATCH /api/v1/admin/accounts/{accountId}/restore`
- `PATCH /api/v1/admin/accounts/{accountId}/role`

Internal:

- `GET /api/v1/internal/accounts/{accountId}`

Frontend dang noi that:

- login/register/profile/session
- upload / preview / xoa anh chu ky visible tren PDF
- cau hinh `image-only` va vi tri/kich thuoc visible signature theo %
- admin account list/detail/activate/lock/unlock/disable/restore/change role
- user active list de chon signer

## 2. Document Service

Public/API:

- `POST /api/v1/documents`
- `GET /api/v1/documents/my`
- `GET /api/v1/documents/{documentId}`
- `GET /api/v1/documents/{documentId}/download`
- `POST /api/v1/documents/{documentId}/versions`
- `GET /api/v1/admin/documents`

Internal:

- `GET /api/v1/internal/documents/{documentId}/metadata`
- `GET /api/v1/internal/documents/{documentId}/content-for-signing`
- `GET /api/v1/internal/documents/signed-output?objectKey=...`
- `POST /api/v1/internal/documents/{documentId}/signed-output`

Frontend dang noi that:

- list / upload / detail / download / upload version
- admin list all documents

## 3. Certification Service

Public/API:

- `POST /api/v1/certificate-requests`
- `GET /api/v1/certificate-requests/my`
- `GET /api/v1/certificate-requests/{requestId}`
- `GET /api/v1/certificates/my`
- `GET /api/v1/certificates/{certificateId}`
- `GET /api/v1/certificates/serial/{serialNumber}`
- `POST /api/v1/certificates/{certificateId}/renewal-requests`
- `POST /api/v1/certificates/{certificateId}/revoke`
- `GET /api/v1/admin/certificate-requests`
- `POST /api/v1/admin/certificate-requests/{requestId}/approve`
- `POST /api/v1/admin/certificate-requests/{requestId}/reject`
- `GET /api/v1/admin/certificates`
- `POST /api/v1/admin/certificates/{certificateId}/revoke`

Internal:

- `GET /api/v1/internal/certificates/{certificateId}`
- `POST /api/v1/internal/certificates/{certificateId}/sign`

Frontend dang noi that:

- user certificate status + request history
- request cap / gia han
- admin approve / reject / revoke

## 4. Signing Workflow Service

Public/API:

- `POST /api/v1/signing-requests`
- `GET /api/v1/signing-requests/requested-by-me`
- `GET /api/v1/signing-requests/pending-for-me`
- `GET /api/v1/signing-requests/{requestId}`
- `POST /api/v1/signing-requests/{requestId}/approve`
- `POST /api/v1/signing-requests/{requestId}/reject`
- `POST /api/v1/signing-requests/{requestId}/cancel`
- `POST /api/v1/signing-requests/{requestId}/execute-signing`
- `GET /api/v1/signing-requests/{requestId}/source-document`
- `GET /api/v1/signing-requests/{requestId}/signed-output`
- `GET /api/v1/admin/signing-requests`

Internal:

- `GET /api/v1/internal/signing-requests/{requestId}/artifact`

Frontend dang noi that:

- create request
- my requests
- requests to sign
  - bao gom ca request `PENDING` va request `APPROVED` nhung chua ky xong, de signer quay lai tiep tuc execute-signing
- detail
- approve / reject / cancel / execute-signing
- preview source PDF va dat vi tri visible signature ngay trong luc execute-signing
  - draft vi tri visible signature theo `requestId` duoc luu trong `sessionStorage`, roi trang va quay lai van tiep tuc duoc
- download signed output theo `requestId`
- admin request overview
- signed output moi duoc tao tu runtime hien tai se duoc gan them RFC 3161 timestamp token noi bo tren chu ky CMS/PAdES

## 5. Step-up Auth Service

Public/API:

- `POST /api/v1/step-up/mfa-registrations`
- `GET /api/v1/step-up/mfa-registrations/me`
- `POST /api/v1/step-up/mfa-registrations/{registrationId}/disable`
- `POST /api/v1/step-up/challenges`
- `POST /api/v1/step-up/challenges/{challengeId}/verify`
- `POST /api/v1/step-up/webauthn/registrations/options`
- `POST /api/v1/step-up/webauthn/registrations/finish`
- `POST /api/v1/step-up/webauthn/assertions/options`
- `POST /api/v1/step-up/webauthn/assertions/finish`
- `GET /api/v1/step-up/recovery-requests/me`
- `POST /api/v1/step-up/recovery-requests`
- `POST /api/v1/step-up/recovery-requests/{requestId}/cancel`
- `POST /api/v1/admin/step-up/webauthn/enrollment-tickets`
- `GET /api/v1/admin/step-up/webauthn/recovery-requests?userId=...`
- `POST /api/v1/admin/step-up/webauthn/recovery-requests/{requestId}/approve`
- `POST /api/v1/admin/step-up/webauthn/recovery-requests/{requestId}/reject`

Internal:

- `POST /api/v1/internal/step-up/verifications/consume`

Frontend dang noi that:

- module quan ly step-up doc lap
- WebAuthn registration / assertion
- recovery request / approve / reject / cancel
- consume proof token trong execute-signing
- Luu y:
  - product path da chuyen sang WebAuthn-only
  - OTP/TOTP challenge flow da bi khoa trong luong van hanh
  - authenticator WebAuthn dau tien bat buoc co `enrollmentTicket` do admin cap
  - moi tai khoan chi duoc giu toi da 1 authenticator WebAuthn `ACTIVE`
  - khong duoc self-disable authenticator WebAuthn ACTIVE cuoi cung
  - recovery request `PENDING/APPROVED` se khoa assertion / management flow thong thuong
  - recovery duoc approve se cap `recoveryTicket` ngan han de dang ky thiet bi thay the
  - dang ky thiet bi thay the thanh cong se disable authenticator WebAuthn ACTIVE cu

## 6. Verification Service

Public/API:

- `POST /api/v1/verifications/files`
- `POST /api/v1/verifications/signed-output`
- `POST /api/v1/verifications/signing-requests/{requestId}`
- `GET /api/v1/verifications/reports/{reportId}`
- `GET /api/v1/verifications/signing-requests/{requestId}/latest`
- `GET /api/v1/verifications/reports`

Frontend dang noi that:

- verify bang file upload truc tiep tu may nguoi dung
- cho phep chon `INTERNAL_CA` hoac `EXTERNAL_CA`
- voi `EXTERNAL_CA`, co the upload them PEM/CRT root/intermediate de build trust chain
- verify theo signing request
- list recent reports
- report detail
- verifier se uu tien `trusted signing time` neu signature co RFC 3161 timestamp token duoc TSA noi bo tin cay xac nhan

## 6.1 Certification Service internal timestamp endpoints

Internal:

- `POST /api/v1/internal/timestamps/token`
- `GET /api/v1/internal/timestamps/authority`
- `GET /api/v1/internal/timestamps/authority/serial/{serialNumber}`

Dung cho:

- `signing-workflow-service` xin RFC 3161 timestamp token sau khi tao CMS/PAdES signature
- `verification-service` doi chieu TSA noi bo theo serial de validate trusted signing time

## 7. Audit Service

Public/API:

- `GET /api/v1/admin/audit/events`
- `GET /api/v1/admin/audit/events/{eventId}`

Internal:

- `POST /internal/v1/audit/events`

Frontend dang noi that:

- admin audit list
- audit detail

## 8. Gateway / Nginx

Gateway route config:

- `services/api-gateway/src/main/resources/application.yml`

Frontend base URL:

- `frontend/internal-portal/.env` -> `VITE_API_BASE_URL=http://localhost:8080`

Nginx public proxy:

- `infra/nginx/nginx.conf`

## 9. Backend gaps da xac nhan

- Khong co endpoint tong hop dashboard summary cho user/admin
- account transitions da duoc chuan hoa:
  - `activate`: `PENDING -> ACTIVE`
  - `lock`: `ACTIVE -> LOCKED`
  - `unlock`: `LOCKED -> ACTIVE`
  - `disable`: `PENDING/ACTIVE/LOCKED -> DISABLED`
  - `restore`: `DISABLED -> ACTIVE`
- Verification cho CA ben thu 3 / external OCSP-CRL chua co API/UI day du
