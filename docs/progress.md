# Progress

## Dot cap nhat hien tai

Muc tieu dot nay:

- audit lai toan repo theo huong san pham that
- cap nhat docs nen
- doi chieu backend API that voi frontend hien co
- khoa lai Pha 1 foundation
- ghi ro cac gap that thay vi de docs cu bi lech
- siet policy WebAuthn cho step-up va reset du lieu ve trang thai sach

## Da hoan thanh

### Audit repo

- [x] audit cau truc source backend/frontend
- [x] map controllers va endpoint that cua 7 service
- [x] doi chieu route/page/service layer cua frontend moi
- [x] xac dinh docs cu da lech hien trang

### Docs da tao / cap nhat

- [x] `docs/system-overview.md`
- [x] `docs/backend-api-map.md`
- [x] `docs/gap-analysis.md`
- [x] `docs/implementation-plan.md`
- [x] `docs/progress.md`
- [x] `docs/frontend-architecture.md`
- [x] `docs/frontend-routes.md`
- [x] `docs/frontend-pages.md`

### Code da sua trong dot nay

- [x] them module quan ly step-up doc lap cho user
- [x] them route `/app/step-up`
- [x] them menu `Bao mat va step-up`
- [x] sua helper WebAuthn de chap nhan payload options co wrapper `publicKey`
- [x] chuan hoa them text public shell sang tieng Viet
- [x] chuyen step-up product path sang WebAuthn-only
- [x] khoa create/verify OTP challenge trong luong van hanh
- [x] khoa authenticator WebAuthn dau tien bang admin-issued enrollment ticket
- [x] gioi han moi tai khoan chi con 1 authenticator WebAuthn `ACTIVE`
- [x] cam self-disable authenticator WebAuthn ACTIVE cuoi cung
- [x] them admin action cap ticket enroll WebAuthn dau tien trong account detail
- [x] reset PostgreSQL ve trang thai sach va seed lai duy nhat admin trong `account_db`
- [x] chuan hoa transition account status: activate / lock / unlock / disable / restore
- [x] chan admin tu lock/disable chinh minh o ca backend va frontend
- [x] thay signed output ZIP ky gia bang dau ra nhung that:
  - PDF -> PAdES detached
  - non-PDF -> CMS/CAdES `.p7m`
- [x] nang verification de doc va xac minh:
  - PAdES PDF
  - CMS/CAdES `.p7m`
  - legacy ZIP fallback
- [x] them public download endpoint cho signed output theo `requestId`
- [x] them upload / preview / xoa anh chu ky ca nhan trong profile account
- [x] nhung anh chu ky ca nhan vao visible signature appearance tren PDF da ky
- [x] doi verification thanh luong upload file truc tiep tu may nguoi dung
- [x] them internal certificate lookup theo serial de doi chieu trust cho file upload
- [x] nang verification-service de nhan multipart toi da 20MB va tra loi 413 ro rang
- [x] them nhanh verify `EXTERNAL_CA` bang PKIX chain validation
- [x] cho phep upload trust anchor / intermediate PEM-CRT tu user khi verify file ngoai
- [x] them WebAuthn recovery flow:
  - user tao recovery request
  - admin approve / reject recovery request
  - admin cap recovery enrollment ticket ngan han
  - recovery dang mo se khoa assertion / manage flow thong thuong
  - recovery hoan tat se disable authenticator WebAuthn ACTIVE cu
- [x] giu request `APPROVED` trong danh sach signer can xu ly de co the quay lai tiep tuc execute-signing
- [x] luu draft vi tri visible signature PDF theo `requestId` khi signer roi trang va quay lai

## File da tao / sua trong dot nay

### Frontend

- `frontend/internal-portal/src/pages/user/StepUpManagementPage.tsx`
- `frontend/internal-portal/src/lib/webauthn.ts`
- `frontend/internal-portal/src/routes/menu.ts`
- `frontend/internal-portal/src/app/router.tsx`
- `frontend/internal-portal/src/layouts/PublicLayout.tsx`
- `frontend/internal-portal/src/pages/public/LandingPage.tsx`
- `frontend/internal-portal/src/pages/public/LoginPage.tsx`
- `frontend/internal-portal/src/pages/public/RegisterPage.tsx`
- `frontend/internal-portal/src/pages/public/IntroPage.tsx`
- `frontend/internal-portal/src/pages/user/SignRequestDetailPage.tsx`
- `frontend/internal-portal/src/services/step-up-service.ts`
- `frontend/internal-portal/src/types/step-up.ts`
- `frontend/internal-portal/src/pages/admin/AccountDetailPage.tsx`
- `frontend/internal-portal/src/components/ui/textarea.tsx`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/dto/request/BeginWebAuthnRegistrationRequest.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/dto/request/CreateWebAuthnEnrollmentTicketRequest.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/dto/request/CreateWebAuthnRecoveryRequest.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/dto/request/ManageMfaRegistrationRequest.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/dto/request/ReviewWebAuthnRecoveryRequest.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/dto/response/WebAuthnEnrollmentTicketResponse.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/dto/response/WebAuthnRecoveryRequestResponse.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/entity/WebAuthnRecoveryRequest.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/entity/WebAuthnRecoveryRequestStatus.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/model/WebAuthnEnrollmentTicketState.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/model/WebAuthnRegistrationCeremonyState.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/repository/WebAuthnRecoveryRequestRepository.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/service/WebAuthnStepUpService.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/service/MfaRegistrationService.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/service/WebAuthnRecoveryService.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/service/StepUpChallengeService.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/service/StepUpRedisStore.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/controller/AdminStepUpController.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/controller/MfaRegistrationController.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/controller/WebAuthnRecoveryController.java`
- `services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/security/SecurityConfig.java`
- `services/step-up-auth-service/src/main/resources/application.yml`
- `services/step-up-auth-service/src/main/resources/db/migration/V3__add_webauthn_recovery_requests.sql`
- `services/api-gateway/src/main/resources/application.yml`
- `services/account-service/src/main/resources/db/migration/V1__init_account_schema.sql`
- `services/signing-workflow-service/pom.xml`
- `services/verification-service/pom.xml`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/service/EmbeddedSignatureOutputService.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/service/SigningExecutionService.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/service/DownloadedSignedOutput.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/service/SigningWorkflowService.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/controller/SigningRequestController.java`
- `services/account-service/src/main/java/com/ct556/account_service/controller/AuthController.java`
- `services/account-service/src/main/java/com/ct556/account_service/domain/Account.java`
- `services/account-service/src/main/java/com/ct556/account_service/dto/AccountResponse.java`
- `services/account-service/src/main/java/com/ct556/account_service/dto/InternalAccountResponse.java`
- `services/account-service/src/main/java/com/ct556/account_service/service/AccountService.java`
- `services/account-service/src/main/java/com/ct556/account_service/service/DownloadedSignatureImage.java`
- `services/account-service/src/main/resources/db/migration/V3__add_signature_image_columns.sql`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/dto/response/InternalSigningArtifactResponse.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/service/InternalSigningArtifactQueryService.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/client/DocumentGateway.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/client/dto/AccountMetadataResponse.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/client/impl/HttpDocumentGateway.java`
- `services/document-service/src/main/java/com/ct556/document_service/controller/InternalDocumentController.java`
- `services/document-service/src/main/java/com/ct556/document_service/service/InternalDocumentStorageService.java`
- `services/verification-service/src/main/java/com/ct556/verification_service/client/dto/InternalSigningArtifactResponse.java`
- `services/verification-service/src/main/java/com/ct556/verification_service/client/CertificationServiceClient.java`
- `services/verification-service/src/main/java/com/ct556/verification_service/service/VerificationService.java`
- `services/verification-service/src/main/java/com/ct556/verification_service/service/ExternalCaTrustValidationService.java`
- `services/verification-service/src/main/java/com/ct556/verification_service/controller/VerificationController.java`
- `services/verification-service/src/main/java/com/ct556/verification_service/exception/GlobalExceptionHandler.java`
- `services/verification-service/src/main/resources/application.yml`
- `services/certification-service/src/main/java/com/ct556/certification_service/controller/InternalCertificateController.java`
- `services/certification-service/src/main/java/com/ct556/certification_service/service/InternalCertificateQueryService.java`
- `frontend/internal-portal/src/pages/shared/ProfilePage.tsx`
- `frontend/internal-portal/src/services/auth-service.ts`
- `frontend/internal-portal/src/types/account.ts`
- `frontend/internal-portal/src/pages/user/VerifyDocumentPage.tsx`
- `frontend/internal-portal/src/services/verification-service.ts`

### Docs

- `docs/system-overview.md`
- `docs/backend-api-map.md`
- `docs/gap-analysis.md`
- `docs/implementation-plan.md`
- `docs/progress.md`
- `docs/frontend-architecture.md`
- `docs/frontend-routes.md`
- `docs/frontend-pages.md`

## API da noi that

- auth
- accounts admin
- documents
- signing workflow
- certificates
- verification
- audit
- step-up (WebAuthn-only product path)
- admin-issued first-enrollment ticket
- WebAuthn recovery request / approve / reject / cancel / recovery ticket
- self-service WebAuthn management actions with proof token
- signed output download theo `requestId`
- source document preview theo `requestId`
- execute-signing override visible signature appearance theo tung request PDF
- verification bang file upload truc tiep qua `POST /api/v1/verifications/files`
- verification external CA bang JVM trust store + PEM/CRT trust anchors do user upload

## Con thieu / can tiep tuc

- external CA verification UI
- confirm dialog dung chung cho action nhay cam
- dashboard summary API
- admin step-up module neu muon quan ly cho tai khoan admin
- session revocation / cooldown sau security change

## Cap nhat 2026-03-31 - TSA noi bo va signing-time validation

### Da lam

- them TSA noi bo trong `certification-service`
- them endpoint noi bo cap RFC 3161 timestamp token
- signing workflow gan `signatureTimeStampToken` vao CMS/PAdES signed output moi
- verification-service validate timestamp token noi bo va uu tien `trusted signing time` khi danh gia validity/revocation cho CA noi bo
- giu backward compatibility cho signed output cu khong co timestamp: verifier van pass neu signature/trust/revocation hien tai hop le, dong thoi hien ro `trusted_timestamp_present = false`

### Files chinh da sua

- `services/certification-service/pom.xml`
- `services/certification-service/src/main/resources/db/migration/V4__add_timestamp_authorities.sql`
- `services/certification-service/src/main/java/com/ct556/certification_service/controller/InternalTimestampController.java`
- `services/certification-service/src/main/java/com/ct556/certification_service/service/InternalTimestampAuthorityService.java`
- `services/certification-service/src/main/java/com/ct556/certification_service/service/InternalCertificateQueryService.java`
- `services/certification-service/src/main/java/com/ct556/certification_service/dto/response/InternalCertificateMetadataResponse.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/client/CertificateGateway.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/client/impl/HttpCertificateGateway.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/client/dto/CertificateMetadataResponse.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/client/dto/TimestampTokenRequest.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/client/dto/TimestampTokenResponse.java`
- `services/signing-workflow-service/src/main/java/com/ct556/signing_workflow_service/service/EmbeddedSignatureOutputService.java`
- `services/verification-service/src/main/java/com/ct556/verification_service/client/CertificationServiceClient.java`
- `services/verification-service/src/main/java/com/ct556/verification_service/client/dto/InternalCertificateMetadataResponse.java`
- `services/verification-service/src/main/java/com/ct556/verification_service/client/dto/InternalTimestampAuthorityMetadataResponse.java`
- `services/verification-service/src/main/java/com/ct556/verification_service/service/VerificationService.java`

### Kiem tra da chay

- `services/certification-service`: `./mvnw.cmd -DskipTests compile` pass
- `services/signing-workflow-service`: `./mvnw.cmd -DskipTests compile` pass
- `services/verification-service`: `./mvnw.cmd -DskipTests compile` pass
- `GET /api/v1/internal/timestamps/authority` tra ve TSA `ACTIVE`
- verify lai signing request `c33d48b7-84c9-45df-86ae-38a8e9417c4a` tren runtime moi:
  - ket qua `PASSED`
  - signed output cu duoc hien ro `trusted_timestamp_present = false`

### Con lai

- can user ky mot signing request moi tren UI sau khi runtime moi da len de xac nhan signed output moi co `trusted_timestamp_present = true` va `trusted_timestamp_validated = true`
- external CA timestamp / LT/LTA / LTV van chua co
