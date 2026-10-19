# Frontend Routes

## 1. Public routes

- `/`
- `/gioi-thieu`
- `/dang-nhap`
- `/dang-ky`

Guard:

- `PublicRoute`

## 2. Blocked routes

- `/blocked/pending`
- `/blocked/locked`
- `/blocked/disabled`
- `/403`
- `* -> /404`

Layout:

- `BlockedLayout`

## 3. USER routes

Base:

- `/app`

Children:

- `/app`
- `/app/documents`
- `/app/documents/upload`
- `/app/documents/:id`
- `/app/sign-requests/create`
- `/app/sign-requests/mine`
- `/app/sign-requests/to-sign`
- `/app/sign-requests/:id`
- `/app/certificate`
- `/app/certificate/request`
- `/app/certificate/renew`
- `/app/verify`
- `/app/verification-reports`
- `/app/step-up`
- `/app/profile`

Guards:

- `AuthenticatedRoute`
- `ActiveAccountRoute`
- `RoleBasedRoute(role="USER")`

Layout:

- `AppShell`

## 4. ADMIN routes

Base:

- `/admin`

Children:

- `/admin`
- `/admin/accounts`
- `/admin/accounts/:id`
- `/admin/certificates/requests`
- `/admin/certificates/renewals`
- `/admin/certificates/manage`
- `/admin/audit`
- `/admin/audit/:id`
- `/admin/profile`

Guards:

- `AuthenticatedRoute`
- `ActiveAccountRoute`
- `RoleBasedRoute(role="ADMIN")`

Layout:

- `AppShell`

## 5. Menu hien tai

### USER

- Tong quan
- Tai lieu
- Yeu cau ky
- Chung thu va xac minh
- Tai khoan
  - Bao mat va step-up
  - Ho so ca nhan

### ADMIN

- Tong quan
- Quan ly tai khoan
- Quan ly chung thu
- Audit
- Ho so
