import { EmptyState } from "@/components/common/EmptyState";

export function UnauthorizedPage() {
  return <EmptyState title="403 - Không đủ quyền" description="Bạn đã đăng nhập nhưng không có quyền truy cập đường dẫn này." />;
}
