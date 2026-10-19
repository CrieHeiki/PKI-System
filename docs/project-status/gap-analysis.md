# Gap Analysis

## 1. Tong ket nhanh

He thong hien tai da co phan lon core flow that va frontend moi da noi that vao API. Tuy nhien van ton tai cac gap theo 3 nhom:

- backend contract gap
- frontend completeness gap
- product-quality gap

## 2. Backend contract gaps

### 2.0 WebAuthn recovery da co, nhung hardening sau security change chua day du

Bootstrap cho authenticator WebAuthn dau tien da duoc khoa bang enrollment ticket do admin cap. He thong hien gioi han moi tai khoan chi duoc giu 1 authenticator WebAuthn `ACTIVE`, va recovery flow mat thiet bi hien da co:

- user tao recovery request
- admin approve / reject
- admin cap recovery enrollment ticket ngan han
- recovery dang mo khoa assertion / management flow thong thuong
- dang ky xong thiet bi thay the se disable authenticator ACTIVE cu va hoan tat recovery

Gap con lai:

- chua revoke session cu sau security change
- chua co cooldown window de tam hoan execute-signing sau khi vua doi cau hinh step-up
- chua co helpdesk/admin reset flow rieng neu user khong con login/session

Tac dong:

- truong hop mat thiet bi va van dang nhap duoc da co luong khoi phuc an toan
- truong hop mat thiet bi dong thoi mat session van can reset flow cap cao hon

### 2.1 Dashboard summary API chua co

Hien tai dashboard user/admin dang tu tong hop tren client tu nhieu query:

- accounts
- certificates
- certificate requests
- audit events

Tac dong:

- nhieu request hon can thiet
- khong toi uu cho production

Trang thai:

- chua co endpoint tong hop
- frontend da ghi ro va dang dung query hop le, khong mock

### 2.2 Signed output download da co, nhung loading/error state van can polish them

Backend va frontend hien da co:

- `GET /api/v1/signing-requests/{requestId}/signed-output`
- CTA tai signed output trong chi tiet signing request

Tac dong con lai:

- loading/error state cua thao tac download van can polish them cho dong deu toan he thong

### 2.3 Account status da duoc chuan hoa, nhung can tiep tuc polish UX action nhay cam

Backend hien da co transition ro rang:

- `activate`: `PENDING -> ACTIVE`
- `lock`: `ACTIVE -> LOCKED`
- `unlock`: `LOCKED -> ACTIVE`
- `disable`: `PENDING/ACTIVE/LOCKED -> DISABLED`
- `restore`: `DISABLED -> ACTIVE`

Tac dong con lai:

- frontend admin van can them confirm dialog dung chung cho lock/disable/restore va cac action nhay cam khac

### 2.4 Verification third-party CA chua day du

Verification hien tai da co nhanh `EXTERNAL_CA` voi PKIX validation, trust anchors upload tu user, va revocation check best-effort. Ngoai ra, signed output moi cua he thong da co RFC 3161 trusted timestamp noi bo, va verifier da uu tien `trusted signing time` khi danh gia validity/revocation cho CA noi bo.

Tuy nhien van chua day du cho:

- external TSA / trusted timestamp validation cho CA ngoai
- LT/LTA hoac LTV profile
- nhung OCSP/CRL/TSA evidence vao signed artifact de long-term verification it phu thuoc online state
- UX hien thi chain/trust/revocation/timestamp chi tiet hon
- policy rulset day du cho CA ngoai

## 3. Frontend gaps

### 3.1 Chua co confirm dialog tap trung cho thao tac nhay cam

Hien tai nhieu thao tac quan trong van thuc thi truc tiep:


- disable registration
- revoke certificate
- lock/disable/restore account
- cancel request
- approve/reject recovery request

Can them `ConfirmDialog` dung chung va ap tren cac mutation nhay cam.

### 3.2 Chua co loading skeleton / empty / error dong deu tren moi man

Da co `RouteFallback`, `EmptyState`, mot phan error text. Nhung van chua dong bo tren toan bo module.

### 3.3 Van con mot so text/UI chua du thuan Viet

Da va dang duoc chuan hoa, nhung can tiep tuc ra soat de dam bao:

- toan bo text UI dung tieng Viet
- helper copy va labels khong con pha tieng Anh

### 3.4 Chua co phan quan ly step-up rieng cho admin account cua chinh minh

User da co module step-up doc lap. Admin da co action cap first-enrollment ticket va approve/reject recovery request cho user trong trang chi tiet account, nhung chua co module rieng de admin tu quan ly authenticator cua chinh account admin.

### 3.5 Chua co DataTable/SearchFilterBar/ConfirmDialog tap trung dung chuan toan he thong

Da co `Table` va mot so filter block, nhung chua dong goi day du thanh bo component muc san pham.

## 4. Product-quality gaps

### 4.1 WebAuthn frontend can tiep tuc retest runtime

Frontend helper da duoc sua de chap nhan payload `optionsJson` co wrapper `publicKey`. Recovery flow moi cung da duoc noi vao step-up management page. Can retest lai tren:

- Chrome / Edge
- Windows Hello / PIN
- happy path recovery bang trinh duyet that

### 4.2 Responsive va polish chua dong deu

Admin dashboard, shell va public entry da tot hon. Nhung cac man nghiep vu day bang/form can tiep tuc polish cho mobile/tablet.

### 4.3 Mot so man van dua nhieu vao native select/form

Can tiep tuc dua het ve UI primitives va form pattern thong nhat.

## 5. Nguyen tac xu ly gap

- Khong gia vo da hoan thanh neu backend chua co
- Neu can fallback, fallback chi nam o adapter/service layer va phai duoc note ro
- Uu tien dong cac gap anh huong truc tiep den end-to-end flow truoc:
  1. session revocation / cooldown sau security change
  2. confirm dialog
  3. dashboard summary API
  4. external CA verification UI polish
  5. helpdesk/admin reset flow cho truong hop mat session
