# Frontend Architecture

## 1. Hien trang frontend

Frontend chinh hien nay la app rieng tai:

- `frontend/internal-portal`

Frontend static cu trong `api-gateway` khong con la trung tam phat trien chinh, chi con gia tri tham chieu cu.

## 2. Stack dang dung

- React
- TypeScript
- Vite
- Tailwind CSS
- React Router
- TanStack Query
- react-hook-form
- zod
- Zustand
- bo UI primitives tu xay theo tinh than shadcn/ui

## 3. Cau truc source hien tai

```text
src/
  app/
  assets/
  components/
    common/
    ui/
  features/
    auth/
  hooks/
  layouts/
  lib/
  pages/
    admin/
    blocked/
    public/
    shared/
    system/
    user/
  routes/
    guards/
  services/
  store/
  types/
```

## 4. Nguyen tac kien truc dang ap dung

- `auth-store.ts` giu session va blocked registration state
- service layer tach rieng cho tung domain:
  - auth
  - account
  - documents
  - signing
  - certificates
  - verification
  - audit
  - step-up
- route guards tap trung:
  - `PublicRoute`
  - `AuthenticatedRoute`
  - `ActiveAccountRoute`
  - `RoleBasedRoute`
- layout tach rieng:
  - `PublicLayout`
  - `BlockedLayout`
  - `AppShell`

## 5. Design system hien tai

- token mau va typography tap trung trong:
  - `tailwind.config.ts`
  - `src/index.css`
- UI primitives dung chung:
  - `Button`
  - `Card`
  - `Input`
  - `Textarea`
  - `Badge`
  - `Select`
  - `Table`

## 6. Dinh huong tiep tuc

De dat muc san pham that, frontend se tiep tuc duoc day theo huong:

- bo `ConfirmDialog` dung chung
- `SearchFilterBar` / `DataTable` dung chung
- loading/empty/error state dong deu
- responsive polish cho man hinh data-dense
- giam le thuoc vao tally tren client khi backend co summary endpoints
