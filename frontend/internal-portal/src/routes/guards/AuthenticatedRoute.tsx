import { Navigate, Outlet, useLocation } from "react-router-dom";

import { useAuthStore } from "@/store/auth-store";

export function AuthenticatedRoute() {
  const user = useAuthStore((state) => state.user);
  const location = useLocation();

  if (!user) {
    return <Navigate to="/dang-nhap" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
