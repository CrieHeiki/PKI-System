package com.ct556.step_up_auth_service.repository;

import com.ct556.step_up_auth_service.entity.WebAuthnRecoveryRequest;
import com.ct556.step_up_auth_service.entity.WebAuthnRecoveryRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebAuthnRecoveryRequestRepository extends JpaRepository<WebAuthnRecoveryRequest, UUID> {

    List<WebAuthnRecoveryRequest> findByUserIdOrderByRequestedAtDesc(UUID userId);

    Optional<WebAuthnRecoveryRequest> findByIdAndUserId(UUID id, UUID userId);

    Optional<WebAuthnRecoveryRequest> findFirstByUserIdAndStatusInOrderByRequestedAtDesc(UUID userId,
                                                                                          Collection<WebAuthnRecoveryRequestStatus> statuses);

    Optional<WebAuthnRecoveryRequest> findByRecoveryTicket(String recoveryTicket);
}
