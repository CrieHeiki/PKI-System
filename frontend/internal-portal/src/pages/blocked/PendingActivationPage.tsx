import { useNavigate } from "react-router-dom";

import { AccountStatusBanner } from "@/components/common/AccountStatusBanner";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/auth-store";

export function PendingActivationPage() {
  const navigate = useNavigate();
  const clearPendingRegistrationStatus = useAuthStore((state) => state.clearPendingRegistrationStatus);

  return (
    <AccountStatusBanner
      status="PENDING"
      title="Tài khoản đang chờ kích hoạt"
      description="Bạn đã tạo tài khoản thành công, nhưng hiện tại cần quản trị viên kích hoạt trước khi sử dụng các dịch vụ nghiệp vụ."
      actions={
        <Button
          onClick={() => {
            clearPendingRegistrationStatus();
            navigate("/dang-nhap", { replace: true });
          }}
        >
          Quay về trang đăng nhập
        </Button>
      }
    />
  );
}
