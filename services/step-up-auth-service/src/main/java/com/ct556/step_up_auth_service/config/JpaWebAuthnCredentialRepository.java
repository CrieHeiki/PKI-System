package com.ct556.step_up_auth_service.config;

import com.ct556.step_up_auth_service.entity.MfaRegistration;
import com.ct556.step_up_auth_service.entity.MfaRegistrationStatus;
import com.ct556.step_up_auth_service.entity.MfaType;
import com.ct556.step_up_auth_service.repository.MfaRegistrationRepository;
import com.ct556.step_up_auth_service.service.WebAuthnUserHandleCodec;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.exception.Base64UrlException;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JpaWebAuthnCredentialRepository implements CredentialRepository {

    private final MfaRegistrationRepository mfaRegistrationRepository;
    private final WebAuthnUserHandleCodec userHandleCodec;

    public JpaWebAuthnCredentialRepository(MfaRegistrationRepository mfaRegistrationRepository,
                                           WebAuthnUserHandleCodec userHandleCodec) {
        this.mfaRegistrationRepository = mfaRegistrationRepository;
        this.userHandleCodec = userHandleCodec;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        UUID userId = UUID.fromString(username);
        return findActiveWebAuthnRegistrations(userId).stream()
                .map(this::toCredentialDescriptor)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        UUID userId = UUID.fromString(username);
        return Optional.of(userHandleCodec.fromUserId(userId));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return Optional.of(userHandleCodec.toUserId(userHandle).toString());
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        UUID userId = userHandleCodec.toUserId(userHandle);
        return findActiveWebAuthnRegistrations(userId).stream()
                .filter(registration -> credentialId.getBase64Url().equals(registration.getWebauthnCredentialId()))
                .findFirst()
                .map(registration -> toRegisteredCredential(registration, userHandle));
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return mfaRegistrationRepository.findByWebauthnCredentialId(credentialId.getBase64Url())
                .filter(registration -> registration.getRegistrationStatus() == MfaRegistrationStatus.ACTIVE)
                .filter(registration -> registration.getMfaType() == MfaType.WEBAUTHN)
                .map(registration -> toRegisteredCredential(registration, userHandleCodec.fromUserId(registration.getUserId())))
                .stream()
                .collect(Collectors.toSet());
    }

    private List<MfaRegistration> findActiveWebAuthnRegistrations(UUID userId) {
        return mfaRegistrationRepository.findByUserIdAndRegistrationStatusAndMfaType(
                userId,
                MfaRegistrationStatus.ACTIVE,
                MfaType.WEBAUTHN
        );
    }

    private PublicKeyCredentialDescriptor toCredentialDescriptor(MfaRegistration registration) {
        return PublicKeyCredentialDescriptor.builder()
                .id(decode(registration.getWebauthnCredentialId()))
                .build();
    }

    private RegisteredCredential toRegisteredCredential(MfaRegistration registration, ByteArray userHandle) {
        return RegisteredCredential.builder()
                .credentialId(decode(registration.getWebauthnCredentialId()))
                .userHandle(userHandle)
                .publicKeyCose(decode(registration.getWebauthnPublicKeyCose()))
                .signatureCount(registration.getWebauthnSignatureCount() == null ? 0L : registration.getWebauthnSignatureCount())
                .build();
    }

    private ByteArray decode(String value) {
        try {
            return ByteArray.fromBase64Url(value);
        } catch (Base64UrlException ex) {
            throw new IllegalStateException("Stored WebAuthn credential data is not valid Base64Url", ex);
        }
    }
}
