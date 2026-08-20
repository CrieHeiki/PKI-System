import { AccountStatusBanner } from "@/components/common/AccountStatusBanner";

export function LockedAccountPage() {
  return (
    <AccountStatusBanner
      status="LOCKED"
      title="Tài khoản đã bị khóa"
      description="Hệ thống đã chặn truy cập nghiệp vụ của tài khoản này. Vui lòng liên hệ quản trị viên để được hỗ trợ."
    />
  );
}
