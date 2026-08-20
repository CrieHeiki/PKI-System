import { Badge } from "@/components/ui/badge";
import type { AccountStatus } from "@/types/auth";

export function StatusBadge({ status }: { status: AccountStatus | string }) {
  const normalized = String(status).toUpperCase();
  const labelMap: Record<string, string> = {
    ACTIVE: "Đang hoạt động",
    ISSUED: "Đã cấp",
    PENDING: "Chờ xử lý",
    PENDING_APPROVAL: "Chờ phê duyệt",
    APPROVED: "Đã duyệt",
    EXPIRED: "Hết hạn",
    LOCKED: "Đã khóa",
    DISABLED: "Vô hiệu hóa",
    REVOKED: "Đã thu hồi",
    REJECTED: "Bị từ chối",
    SIGNED: "Đã ký",
    COMPLETED: "Hoàn tất",
    CANCELLED: "Đã hủy",
    VALID: "Hợp lệ",
    INVALID: "Không hợp lệ",
  };
  const label = labelMap[normalized] ?? normalized;

  if (normalized === "ACTIVE") {
    return <Badge variant="success">{label}</Badge>;
  }
  if (normalized === "ISSUED") {
    return <Badge variant="success">{label}</Badge>;
  }
  if (normalized === "PENDING" || normalized === "PENDING_APPROVAL" || normalized === "EXPIRED") {
    return <Badge variant="warning">{label}</Badge>;
  }
  if (normalized === "APPROVED" || normalized === "SIGNED" || normalized === "COMPLETED" || normalized === "VALID") {
    return <Badge variant="success">{label}</Badge>;
  }
  if (normalized === "LOCKED" || normalized === "DISABLED" || normalized === "REVOKED" || normalized === "REJECTED" || normalized === "INVALID") {
    return <Badge variant="destructive">{label}</Badge>;
  }
  return <Badge variant="info">{label}</Badge>;
}
