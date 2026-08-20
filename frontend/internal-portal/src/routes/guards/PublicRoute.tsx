import { Navigate, Outlet } from "react-router-dom";

import { useAuthStore } from "@/store/auth-store";

export function PublicRoute() {
  const user = useAuthStore((state) => state.user);
  const pendingRegistrationStatus = useAuthStore((state) => state.pendingRegistrationStatus);

  if (pendingRegistrationStatus === "PENDING") {
    return <Navigate to="/blocked/pending" replace />;
  }

  if (!user) {
    return <Outlet />;
  }

  if (user.status !== "ACTIVE") {
    return <Navigate to={`/blocked/${user.status.toLowerCase()}`} replace />;
  }

  return <Navigate to={user.role === "ADMIN" ? "/admin" : "/app"} replace />;
}
