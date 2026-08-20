import { Navigate, Outlet } from "react-router-dom";

import { useAuthStore } from "@/store/auth-store";

export function ActiveAccountRoute() {
  const user = useAuthStore((state) => state.user);

  if (!user) {
    return <Navigate to="/dang-nhap" replace />;
  }

  if (user.status !== "ACTIVE") {
    return <Navigate to={`/blocked/${user.status.toLowerCase()}`} replace />;
  }

  return <Outlet />;
}
