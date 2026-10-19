# System Overview

## Muc tieu san pham

He thong nay la nen tang PKI / digital signing noi bo theo kien truc microservice, huong den mot san pham co the dung end-to-end cho:

- quan ly tai khoan noi bo
- quan ly tai lieu luu trong MinIO
- tao va xu ly signing workflow
  - nhung dau ra ky that:
    - PDF co chu ky nhung theo PAdES detached
    - non-PDF dong goi thanh CMS/CAdES `.p7m`
  - neu signer da tai anh chu ky, PDF da ky se co visible signature appearance gom:
    - hinh anh chu ky
    - signer
    - serial certificate
    - thoi diem ky
    - request id
- step-up authentication cho giao dich nhay cam
  - product path hien tai uu tien WebAuthn-only
- quan ly chung thu so noi bo
- xac minh signed output
- ghi audit trail cho cac hanh dong quan trong

## Kien truc tong the

Luong truy cap hien tai:

`Frontend (Vite / 5173) -> Nginx (8080) -> API Gateway (8090) -> cac microservice`

Thanh phan backend:

- `account-service`
- `document-service`
- `certification-service`
- `signing-workflow-service`
- `step-up-auth-service`
- `verification-service`
- `audit-service`
- `api-gateway`

Ha tang:

- PostgreSQL cho du lieu nghiep vu
- Redis cho challenge/proof state step-up
- MinIO cho tai lieu goc va signed output
- Vault cho PKI noi bo
- ImmuDB cho append-only audit storage
- Nginx lam public reverse proxy

## Phan quyen va guard

Role:

- `ADMIN`
- `USER`

Account status:

- `PENDING`
- `ACTIVE`
- `LOCKED`
- `DISABLED`

Frontend hien tai da co:

- public routes cho guest
- blocked pages theo account status
- `AuthenticatedRoute`
- `ActiveAccountRoute`
- `RoleBasedRoute`

## Luong nghiep vu chinh da co

### USER

- dang ky / dang nhap
- tai anh chu ky ca nhan de nhung vao PDF da ky
- tuy chinh visible signature:
  - chi hien hinh anh
  - chon vi tri / kich thuoc theo % trang PDF
- upload document
- xem danh sach / chi tiet / download / upload version moi
- tao signing request
- xem request toi tao
- xem request can toi ky
- approve / reject / execute signing
- download signed output da ky theo `requestId`
- xem certificate cua toi
- gui yeu cau cap / gia han certificate
- quan ly step-up doc lap va trong flow ky
  - authenticator dau tien chi duoc enroll bang ticket do admin cap
  - moi tai khoan chi duoc giu toi da 1 authenticator WebAuthn `ACTIVE`
  - authenticator ACTIVE cuoi cung khong duoc self-disable
- verify signed output theo request
- xem verification reports

### ADMIN

- dashboard quan tri
- list / detail / activate / lock / disable / doi role account
- xu ly certificate request / renewal request
- revoke certificate
- xem audit log va chi tiet audit

## Hien trang san pham

He thong khong con o muc “demo goi API” nua. Frontend moi da duoc tach thanh app rieng tai `frontend/internal-portal`, da noi vao backend that qua gateway. Tuy nhien van con cac gap can tiep tuc dong lai de dat muc san pham hoan chinh:

- verification cho CA ben thu 3 chua co UI va logic day du
- signed output da co public download endpoint qua signing workflow
- audit, dashboard summary va mot so module chua co API tong hop / pagination tot
- mot so flow van can confirm dialog, skeleton va polish responsive sau cung
- recovery WebAuthn cho tai khoan con session da co; truong hop mat ca thiet bi va session van can helpdesk/admin reset flow (xem `docs/project-status/gap-analysis.md`)
