package com.ct556.step_up_auth_service.repository;

import com.ct556.step_up_auth_service.entity.MfaRegistration;
import com.ct556.step_up_auth_service.entity.MfaRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MfaRegistrationRepository extends JpaRepository<MfaRegistration, UUID> {

    List<MfaRegistration> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<MfaRegistration> findByUserIdAndRegistrationStatus(UUID userId, MfaRegistrationStatus registrationStatus);

    Optional<MfaRegistration> findByIdAndUserId(UUID id, UUID userId);

    Optional<MfaRegistration> findByWebauthnCredentialId(String webauthnCredentialId);

    List<MfaRegistration> findByUserIdAndRegistrationStatusAndMfaType(UUID userId,
                                                                      MfaRegistrationStatus registrationStatus,
                                                                      com.ct556.step_up_auth_service.entity.MfaType mfaType);
}
