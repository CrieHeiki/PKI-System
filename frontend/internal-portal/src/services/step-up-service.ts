import { http } from "@/services/http-client";
import type { ApiEnvelope } from "@/types/common";
import type {
  MfaRegistration,
  StepUpChallenge,
  StepUpVerification,
  WebAuthnAssertionOptions,
  WebAuthnEnrollmentTicket,
  WebAuthnRecoveryRequest,
  WebAuthnRegistrationOptions,
} from "@/types/step-up";

async function unwrap<T>(promise: Promise<ApiEnvelope<T>>) {
  const payload = await promise;
  return payload.data;
}

export const stepUpService = {
  listRegistrations() {
    return unwrap(http<ApiEnvelope<MfaRegistration[]>>("/api/v1/step-up/mfa-registrations/me"));
  },

  createRegistration(payload: { mfaType: string; issuer?: string; label: string; markAsPrimary?: boolean }) {
    return unwrap(
      http<ApiEnvelope<MfaRegistration>>("/api/v1/step-up/mfa-registrations", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  },

  markPrimary(registrationId: string, authorizationVerificationToken: string) {
    return unwrap(
      http<ApiEnvelope<MfaRegistration>>(`/api/v1/step-up/mfa-registrations/${registrationId}/primary`, {
        method: "POST",
        body: JSON.stringify({ authorizationVerificationToken }),
      }),
    );
  },

  disable(registrationId: string, authorizationVerificationToken: string) {
    return unwrap(
      http<ApiEnvelope<MfaRegistration>>(`/api/v1/step-up/mfa-registrations/${registrationId}/disable`, {
        method: "POST",
        body: JSON.stringify({ authorizationVerificationToken }),
      }),
    );
  },

  createChallenge(payload: { registrationId?: string; action: string; targetType?: string; targetId?: string }) {
    return unwrap(
      http<ApiEnvelope<StepUpChallenge>>("/api/v1/step-up/challenges", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  },

  verifyChallenge(challengeId: string, otpCode: string) {
    return unwrap(
      http<ApiEnvelope<StepUpVerification>>(`/api/v1/step-up/challenges/${challengeId}/verify`, {
        method: "POST",
        body: JSON.stringify({ otpCode }),
      }),
    );
  },

  beginWebAuthnRegistration(payload: {
    issuer?: string;
    label: string;
    markAsPrimary?: boolean;
    enrollmentTicket?: string;
    authorizationVerificationToken?: string;
  }) {
    return unwrap(
      http<ApiEnvelope<WebAuthnRegistrationOptions>>("/api/v1/step-up/webauthn/registrations/options", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  },

  createFirstEnrollmentTicket(userId: string) {
    return unwrap(
      http<ApiEnvelope<WebAuthnEnrollmentTicket>>("/api/v1/admin/step-up/webauthn/enrollment-tickets", {
        method: "POST",
        body: JSON.stringify({ userId }),
      }),
    );
  },

  listMyRecoveryRequests() {
    return unwrap(http<ApiEnvelope<WebAuthnRecoveryRequest[]>>("/api/v1/step-up/recovery-requests/me"));
  },

  createRecoveryRequest(reason: string) {
    return unwrap(
      http<ApiEnvelope<WebAuthnRecoveryRequest>>("/api/v1/step-up/recovery-requests", {
        method: "POST",
        body: JSON.stringify({ reason }),
      }),
    );
  },

  cancelRecoveryRequest(requestId: string) {
    return unwrap(
      http<ApiEnvelope<WebAuthnRecoveryRequest>>(`/api/v1/step-up/recovery-requests/${requestId}/cancel`, {
        method: "POST",
      }),
    );
  },

  listRecoveryRequestsForAdmin(userId: string, status?: string) {
    const params = new URLSearchParams({ userId });
    if (status) {
      params.set("status", status);
    }
    return unwrap(
      http<ApiEnvelope<WebAuthnRecoveryRequest[]>>(`/api/v1/admin/step-up/webauthn/recovery-requests?${params.toString()}`),
    );
  },

  approveRecoveryRequest(requestId: string, note: string) {
    return unwrap(
      http<ApiEnvelope<WebAuthnRecoveryRequest>>(`/api/v1/admin/step-up/webauthn/recovery-requests/${requestId}/approve`, {
        method: "POST",
        body: JSON.stringify({ note }),
      }),
    );
  },

  rejectRecoveryRequest(requestId: string, note: string) {
    return unwrap(
      http<ApiEnvelope<WebAuthnRecoveryRequest>>(`/api/v1/admin/step-up/webauthn/recovery-requests/${requestId}/reject`, {
        method: "POST",
        body: JSON.stringify({ note }),
      }),
    );
  },

  finishWebAuthnRegistration(payload: { ceremonyId: string; credentialJson: string }) {
    return unwrap(
      http<ApiEnvelope<MfaRegistration>>("/api/v1/step-up/webauthn/registrations/finish", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  },

  beginWebAuthnAssertion(payload: { registrationId?: string; action: string; targetType?: string; targetId?: string }) {
    return unwrap(
      http<ApiEnvelope<WebAuthnAssertionOptions>>("/api/v1/step-up/webauthn/assertions/options", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  },

  finishWebAuthnAssertion(payload: { ceremonyId: string; credentialJson: string }) {
    return unwrap(
      http<ApiEnvelope<StepUpVerification>>("/api/v1/step-up/webauthn/assertions/finish", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  },
};
