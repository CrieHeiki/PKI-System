export type AppRole = "ADMIN" | "USER";
export type AccountStatus = "PENDING" | "ACTIVE" | "LOCKED" | "DISABLED";

export interface AuthUser {
  accountId: string;
  username: string;
  email: string;
  role: AppRole;
  status: AccountStatus;
  token: string;
  tokenType: string;
}

export interface LoginPayload {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
}

export interface RegisterResult {
  id: string;
  username: string;
  email: string;
  role: AppRole;
  status: AccountStatus;
}
