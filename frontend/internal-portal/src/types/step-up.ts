export interface MfaRegistration {
  id: string;
  userId: string;
  mfaType: string;
  issuer: string | null;
  label: string;
  registrationStatus: string;
  primary: boolean;
  createdAt: string;
  activatedAt: string | null;
  disabledAt: string | null;
}

export interface WebAuthnRegistrationOptions {
  ceremonyId: string;
  optionsJson: string;
  expiresAt: string;
}

export interface WebAuthnEnrollmentTicket {
  enrollmentTicket: string;
  userId: string;
  issuedByAdminId: string;
  issuedAt: string;
  expiresAt: string;
}

export interface WebAuthnRecoveryRequest {
  id: string;
  userId: string;
  status: string;
  reason: string | null;
  decisionNote: string | null;
  reviewedByAdminId: string | null;
  requestedAt: string;
  reviewedAt: string | null;
  completedAt: string | null;
  recoveryTicket: string | null;
  recoveryTicketIssuedAt: string | null;
  recoveryTicketExpiresAt: string | null;
  completedRegistrationId: string | null;
}

export interface WebAuthnAssertionOptions {
  ceremonyId: string;
  registrationId: string;
  action: string;
  targetType: string | null;
  targetId: string | null;
  optionsJson: string;
  expiresAt: string;
}

export interface StepUpChallenge {
  challengeId: string;
  registrationId: string;
  mfaType: string;
  action: string;
  targetType: string | null;
  targetId: string | null;
  deliveryHint: string | null;
  expiresAt: string;
  maxAttempts: number;
  challengeCode: string | null;
}

export interface StepUpVerification {
  verified: boolean;
  challengeId: string;
  verificationToken: string;
  registrationId: string;
  action: string;
  targetType: string | null;
  targetId: string | null;
  verifiedAt: string;
  proofExpiresAt: string;
}
