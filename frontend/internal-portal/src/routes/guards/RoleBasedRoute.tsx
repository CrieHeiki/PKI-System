import { Navigate, Outlet } from "react-router-dom";

import { useAuthStore } from "@/store/auth-store";
import type { AppRole } from "@/types/auth";

export function RoleBasedRoute({ role }: { role: AppRole }) {
  const user = useAuthStore((state) => state.user);

  if (!user) {
    return <Navigate to="/dang-nhap" replace />;
  }

  if (user.role !== role) {
    return <Navigate to="/403" replace />;
  }

  return <Outlet />;
}
