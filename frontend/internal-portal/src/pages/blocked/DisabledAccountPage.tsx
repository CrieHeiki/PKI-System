import { AccountStatusBanner } from "@/components/common/AccountStatusBanner";

export function DisabledAccountPage() {
  return (
    <AccountStatusBanner
      status="DISABLED"
      title="Tài khoản đã bị vô hiệu hóa"
      description="Tài khoản này không còn được phép sử dụng hệ thống. Vui lòng liên hệ quản trị viên nếu cần giải trình hoặc cấp lại quyền."
    />
  );
}
