package com.ct556.step_up_auth_service.config;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;

@Configuration
public class WebAuthnConfig {

    @Bean
    public RelyingParty relyingParty(JpaWebAuthnCredentialRepository credentialRepository,
                                     WebAuthnProperties webAuthnProperties) {
        return RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id(webAuthnProperties.rpId())
                        .name(webAuthnProperties.rpName())
                        .build())
                .credentialRepository(credentialRepository)
                .origins(new LinkedHashSet<>(webAuthnProperties.allowedOrigins()))
                .build();
    }
}
