# Implementation Plan

> Historical project notes preserved for reference. Tasks listed here are not a promise of current implementation or future work; consult the source and the gap analysis for the current snapshot.

## Nguyen tac thuc hien

- Biet ro hien trang truoc khi code tiep
- Tiep tuc dung API that
- Khong dua logic mock vao component
- Moi pha phai cap nhat `docs/project-status/progress.md`

## Pha 1 — Foundation

Muc tieu:

- audit source
- map backend API
- xac dinh gap
- route/layout/store/guards
- public pages
- blocked pages
- auth/session flow

Trang thai hien tai:

- da co tu truoc
- duoc audit va cap nhat lai trong dot nay
- dang chuan hoa them text public shell va docs

Cong viec con lai trong pha:

- ra soat text UI tieng Viet
- dam bao blocked flow va public flow nhat quan

## Pha 2 — USER core workflow

Muc tieu:

- user shell
- dashboard
- documents
- sign requests

Trang thai hien tai:

- phan lon da co va noi API that

Cong viec tiep theo:

- them confirm dialog cho create/cancel/approve/reject
- polish loading/error/empty states
- hoan thien layout cac man table/form day thong tin

## Pha 3 — Certificates, Verification, Step-up

Muc tieu:

- certificate pages
- verification pages
- step-up management

Trang thai hien tai:

- certificate pages: co
- verification pages: co cho internal CA flow
- step-up management user: da them route/doc lap
- WebAuthn: co, can retest runtime

Cong viec tiep theo:

- them profile-level management tot hon
- can nhac bo sung signed output download khi backend co API
- mo rong verification external CA khi backend san sang

## Pha 4 — ADMIN

Muc tieu:

- admin shell
- account management
- certificate management
- audit pages

Trang thai hien tai:

- co route va page that
- da noi API that

Cong viec tiep theo:

- confirm dialog cho action nguy hiem
- table UX tot hon
- pagination/sorting khi backend co contract tot hon

## Pha 5 — Product polish

Muc tieu:

- responsive
- consistency
- skeleton
- error handling
- confirm dialog
- UX polish

Trang thai hien tai:

- dang trong qua trinh

Thu tu uu tien tiep theo:

1. fix blocker runtime step-up/WebAuthn va retest
2. them ConfirmDialog dung chung
3. polish cac man bang/form admin-user
4. dong bo loading/empty/error state
5. cap nhat docs va test checklist
