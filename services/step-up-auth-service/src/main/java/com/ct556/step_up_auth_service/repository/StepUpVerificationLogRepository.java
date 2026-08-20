package com.ct556.step_up_auth_service.repository;

import com.ct556.step_up_auth_service.entity.StepUpVerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StepUpVerificationLogRepository extends JpaRepository<StepUpVerificationLog, UUID> {
}
