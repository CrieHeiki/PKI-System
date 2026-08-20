package com.ct556.step_up_auth_service.entity;

public enum MfaType {
    TOTP,
    EMAIL_OTP,
    SMS_OTP,
    WEBAUTHN
}
