import type { AccountStatus, AppRole } from "@/types/auth";

export interface AccountRecord {
  id: string;
  username: string;
  email: string;
  role: AppRole;
  status: AccountStatus;
  signatureImageConfigured: boolean;
  signatureImageOnly: boolean;
  signatureBoxXPercent: number;
  signatureBoxYPercent: number;
  signatureBoxWidthPercent: number;
  signatureBoxHeightPercent: number;
}

export interface SignatureAppearancePayload {
  imageOnly: boolean;
  xPercent: number;
  yPercent: number;
  widthPercent: number;
  heightPercent: number;
}
