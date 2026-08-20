import { create } from "zustand";
import { persist } from "zustand/middleware";

import type { AccountStatus, AuthUser } from "@/types/auth";

interface AuthState {
  user: AuthUser | null;
  pendingRegistrationStatus: AccountStatus | null;
  setUser: (user: AuthUser) => void;
  setPendingRegistrationStatus: (status: AccountStatus | null) => void;
  clearPendingRegistrationStatus: () => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      pendingRegistrationStatus: null,
      setUser: (user) => set({ user }),
      setPendingRegistrationStatus: (status) => set({ pendingRegistrationStatus: status }),
      clearPendingRegistrationStatus: () => set({ pendingRegistrationStatus: null }),
      logout: () => set({ user: null, pendingRegistrationStatus: null }),
    }),
    {
      name: "ct556-internal-portal-auth",
    },
  ),
);
