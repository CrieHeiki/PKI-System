package com.ct556.certification_service.service;

import com.ct556.certification_service.client.AuditGateway;
import com.ct556.certification_service.domain.CertificateEntity;
import com.ct556.certification_service.domain.CertificateRequestEntity;
import com.ct556.certification_service.domain.CertificateRequestStatus;
import com.ct556.certification_service.domain.CertificateRequestType;
import com.ct556.certification_service.domain.CertificateStatus;
import com.ct556.certification_service.dto.request.CreateCertificateRequestRequest;
import com.ct556.certification_service.dto.request.CreateRevocationRequestRequest;
import com.ct556.certification_service.dto.request.CreateRenewalRequestRequest;
import com.ct556.certification_service.dto.request.IssueCertificateRequest;
import com.ct556.certification_service.dto.request.RejectCertificateRequestRequest;
import com.ct556.certification_service.dto.response.ApproveCertificateRequestResponse;
import com.ct556.certification_service.dto.response.CertificateResponse;
import com.ct556.certification_service.dto.response.CertificateRequestResponse;
import com.ct556.certification_service.dto.response.IssueCertificateResponse;
import com.ct556.certification_service.dto.response.RejectCertificateRequestResponse;
import com.ct556.certification_service.exception.BadRequestException;
import com.ct556.certification_service.exception.ForbiddenException;
import com.ct556.certification_service.exception.ResourceNotFoundException;
import com.ct556.certification_service.repository.CertificateRepository;
import com.ct556.certification_service.repository.CertificateRequestRepository;
import com.ct556.certification_service.security.CurrentUser;
import com.ct556.certification_service.util.X509CertificateUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CertificateRequestService {

    private final CertificateRequestRepository certificateRequestRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateService certificateService;
    private final AuditGateway auditGateway;
    private final ObjectMapper objectMapper;

    public CertificateRequestService(CertificateRequestRepository certificateRequestRepository,
                                     CertificateRepository certificateRepository,
                                     CertificateService certificateService,
                                     AuditGateway auditGateway,
                                     ObjectMapper objectMapper) {
        this.certificateRequestRepository = certificateRequestRepository;
        this.certificateRepository = certificateRepository;
        this.certificateService = certificateService;
        this.auditGateway = auditGateway;
        this.objectMapper = objectMapper;
    }

    private record IssueRequestPayload(
            String commonName,
            String ttl,
            String organization,
            String country
    ) {
    }

    private record RenewalRequestPayload(
            UUID sourceCertificateId,
            String ttl,
            String organization,
            String country
    ) {
    }

    private record RevocationRequestPayload(
            UUID sourceCertificateId
    ) {
    }

    public CertificateRequestResponse createRequest(CurrentUser currentUser, CreateCertificateRequestRequest request) {
        requireAuthenticatedUser(currentUser);

        if (findCurrentActiveCertificate(currentUser.accountId()).isPresent()) {
            throw new BadRequestException("Account already has an active certificate");
        }

        if (certificateRequestRepository.existsByAccountIdAndRequestTypeAndStatus(
                currentUser.accountId(),
                CertificateRequestType.ISSUE,
                CertificateRequestStatus.PENDING
        )) {
            throw new BadRequestException("A pending certificate issue request already exists for this account");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        CertificateRequestEntity entity = new CertificateRequestEntity();
        entity.setId(UUID.randomUUID());
        entity.setAccountId(currentUser.accountId());
        entity.setRequestType(CertificateRequestType.ISSUE);
        entity.setRequestPayload(serializePayload(new IssueRequestPayload(
                normalize(request.commonName()),
                normalize(request.ttl()),
                normalize(request.organization()),
                normalizeCountry(request.country())
        )));
        entity.setRequestReason(null);
        entity.setStatus(CertificateRequestStatus.PENDING);
        entity.setResultCertificateId(null);
        entity.setRequestedAt(now);
        entity.setReviewedAt(null);
        entity.setReviewedByAccountId(null);
        entity.setRejectionReason(null);
        entity.setUpdatedAt(now);

        certificateRequestRepository.save(entity);

        publishAudit(
                "CERTIFICATE_REQUEST_CREATED",
                currentUser.accountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                entity.getId(),
                "CERTIFICATE_REQUEST",
                entity.getId(),
                "Certificate request created",
                payload(
                        "requestType", entity.getRequestType().name(),
                        "accountId", entity.getAccountId(),
                        "status", entity.getStatus().name()
                )
        );

        return toResponse(entity);
    }

    public CertificateRequestResponse createRenewalRequest(CurrentUser currentUser,
                                                           UUID certificateId,
                                                           CreateRenewalRequestRequest request) {
        requireAuthenticatedUser(currentUser);

        CertificateEntity sourceCertificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateId));

        if (!sourceCertificate.getAccountId().equals(currentUser.accountId())) {
            throw new ForbiddenException("You are not allowed to renew this certificate");
        }

        if (sourceCertificate.getStatus() == CertificateStatus.REVOKED) {
            throw new BadRequestException("Cannot renew a revoked certificate");
        }

        if (certificateRepository.existsByRenewedFromCertificateId(certificateId)) {
            throw new BadRequestException("Certificate has already been renewed; renew the latest certificate instead");
        }

        boolean pendingRenewalExists = certificateRequestRepository
                .existsByAccountIdAndRequestTypeAndStatus(
                        currentUser.accountId(),
                        CertificateRequestType.RENEW,
                        CertificateRequestStatus.PENDING
                );

        if (pendingRenewalExists) {
            throw new BadRequestException("A pending renewal request already exists for this certificate");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        CertificateRequestEntity entity = new CertificateRequestEntity();
        entity.setId(UUID.randomUUID());
        entity.setAccountId(currentUser.accountId());
        entity.setRequestType(CertificateRequestType.RENEW);
        entity.setRequestPayload(serializePayload(new RenewalRequestPayload(
                sourceCertificate.getId(),
                normalize(request.ttl()),
                normalize(request.organization()),
                normalizeCountry(request.country())
        )));
        entity.setRequestReason(normalize(request.reason()));
        entity.setStatus(CertificateRequestStatus.PENDING);
        entity.setResultCertificateId(null);
        entity.setRequestedAt(now);
        entity.setReviewedAt(null);
        entity.setReviewedByAccountId(null);
        entity.setRejectionReason(null);
        entity.setUpdatedAt(now);

        certificateRequestRepository.save(entity);

        publishAudit(
                "CERTIFICATE_RENEWAL_REQUEST_CREATED",
                currentUser.accountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                entity.getId(),
                "CERTIFICATE_REQUEST",
                entity.getId(),
                "Certificate renewal request created",
                payload(
                        "requestType", entity.getRequestType().name(),
                        "accountId", entity.getAccountId(),
                        "sourceCertificateId", sourceCertificate.getId(),
                        "status", entity.getStatus().name()
                )
        );

        return toResponse(entity);
    }

    public CertificateRequestResponse createRevocationRequest(CurrentUser currentUser,
                                                              UUID certificateId,
                                                              CreateRevocationRequestRequest request) {
        requireAuthenticatedUser(currentUser);

        CertificateEntity sourceCertificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateId));

        if (!sourceCertificate.getAccountId().equals(currentUser.accountId())) {
            throw new ForbiddenException("You are not allowed to revoke this certificate");
        }

        if (sourceCertificate.getStatus() == CertificateStatus.REVOKED) {
            throw new BadRequestException("Certificate is already revoked");
        }

        boolean pendingRevocationExists = certificateRequestRepository
                .existsByAccountIdAndRequestTypeAndStatus(
                        currentUser.accountId(),
                        CertificateRequestType.REVOKE,
                        CertificateRequestStatus.PENDING
                );

        if (pendingRevocationExists) {
            throw new BadRequestException("A pending revocation request already exists for this certificate");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        CertificateRequestEntity entity = new CertificateRequestEntity();
        entity.setId(UUID.randomUUID());
        entity.setAccountId(currentUser.accountId());
        entity.setRequestType(CertificateRequestType.REVOKE);
        entity.setRequestPayload(serializePayload(new RevocationRequestPayload(sourceCertificate.getId())));
        entity.setRequestReason(normalize(request.reason()));
        entity.setStatus(CertificateRequestStatus.PENDING);
        entity.setResultCertificateId(null);
        entity.setRequestedAt(now);
        entity.setReviewedAt(null);
        entity.setReviewedByAccountId(null);
        entity.setRejectionReason(null);
        entity.setUpdatedAt(now);

        certificateRequestRepository.save(entity);

        publishAudit(
                "CERTIFICATE_REVOCATION_REQUEST_CREATED",
                currentUser.accountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                entity.getId(),
                "CERTIFICATE_REQUEST",
                entity.getId(),
                "Certificate revocation request created",
                payload(
                        "requestType", entity.getRequestType().name(),
                        "accountId", entity.getAccountId(),
                        "sourceCertificateId", sourceCertificate.getId(),
                        "status", entity.getStatus().name()
                )
        );

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CertificateRequestResponse> listMyRequests(CurrentUser currentUser) {
        requireAuthenticatedUser(currentUser);

        return certificateRequestRepository.findAllByAccountIdOrderByRequestedAtDesc(currentUser.accountId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateRequestResponse getMyRequestById(CurrentUser currentUser, UUID requestId) {
        requireAuthenticatedUser(currentUser);

        CertificateRequestEntity entity = certificateRequestRepository.findByIdAndAccountId(requestId, currentUser.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Certificate request not found: " + requestId));

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CertificateRequestResponse> listAllRequests(String status, String requestType) {
        CertificateRequestStatus requestStatus = parseStatus(status);
        CertificateRequestType parsedRequestType = parseRequestType(requestType);

        List<CertificateRequestEntity> entities;

        if (requestStatus == null && parsedRequestType == null) {
            entities = certificateRequestRepository.findAllByOrderByRequestedAtDesc();
        } else if (requestStatus != null && parsedRequestType != null) {
            entities = certificateRequestRepository.findAllByStatusAndRequestTypeOrderByRequestedAtDesc(
                    requestStatus,
                    parsedRequestType
            );
        } else if (requestStatus != null) {
            entities = certificateRequestRepository.findAllByStatusOrderByRequestedAtDesc(requestStatus);
        } else {
            entities = certificateRequestRepository.findAllByRequestTypeOrderByRequestedAtDesc(parsedRequestType);
        }

        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    public ApproveCertificateRequestResponse approveRequest(CurrentUser adminUser, UUID requestId) {
        requireAdmin(adminUser);

        CertificateRequestEntity entity = certificateRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate request not found: " + requestId));

        if (entity.getStatus() != CertificateRequestStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be approved");
        }

        return entity.getRequestType() == CertificateRequestType.RENEW
                ? approveRenewalRequest(adminUser, entity)
                : entity.getRequestType() == CertificateRequestType.REVOKE
                ? approveRevocationRequest(adminUser, entity)
                : approveIssueRequest(adminUser, entity);
    }

    public RejectCertificateRequestResponse rejectRequest(CurrentUser adminUser,
                                                          UUID requestId,
                                                          RejectCertificateRequestRequest request) {
        requireAdmin(adminUser);

        CertificateRequestEntity entity = certificateRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate request not found: " + requestId));

        if (entity.getStatus() != CertificateRequestStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be rejected");
        }

        entity.setStatus(CertificateRequestStatus.REJECTED);
        entity.setReviewedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setReviewedByAccountId(adminUser.accountId());
        entity.setRejectionReason(normalize(request.reason()));
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        certificateRequestRepository.save(entity);

        publishAudit(
                "CERTIFICATE_REQUEST_REJECTED",
                adminUser.accountId(),
                "ADMIN",
                entity.getId(),
                "CERTIFICATE_REQUEST",
                entity.getId(),
                "Certificate request rejected",
                payload(
                        "requestType", entity.getRequestType().name(),
                        "accountId", entity.getAccountId(),
                        "status", entity.getStatus().name(),
                        "reason", entity.getRejectionReason()
                )
        );

        return new RejectCertificateRequestResponse(
                "Certificate request rejected successfully",
                toResponse(entity)
        );
    }

    private ApproveCertificateRequestResponse approveIssueRequest(CurrentUser adminUser,
                                                                  CertificateRequestEntity entity) {
        if (findCurrentActiveCertificate(entity.getAccountId()).isPresent()) {
            throw new BadRequestException("Account already has an active certificate");
        }

        IssueRequestPayload issuePayload = readPayload(
                entity.getRequestPayload(),
                IssueRequestPayload.class,
                "issue request"
        );

        CurrentUser requestOwner = new CurrentUser(
                entity.getAccountId(),
                "request-owner",
                List.of("ROLE_USER")
        );

        IssueCertificateRequest issueRequest = new IssueCertificateRequest(
                issuePayload.commonName(),
                issuePayload.ttl(),
                issuePayload.organization(),
                issuePayload.country()
        );

        IssueCertificateResponse issueResponse = certificateService.issueCertificate(requestOwner, issueRequest);

        entity.setStatus(CertificateRequestStatus.ISSUED);
        entity.setResultCertificateId(issueResponse.certificate().id());
        entity.setReviewedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setReviewedByAccountId(adminUser.accountId());
        entity.setRejectionReason(null);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        certificateRequestRepository.save(entity);

        publishAudit(
                "CERTIFICATE_REQUEST_APPROVED",
                adminUser.accountId(),
                "ADMIN",
                entity.getId(),
                "CERTIFICATE_REQUEST",
                entity.getId(),
                "Certificate request approved and certificate issued",
                payload(
                        "requestType", entity.getRequestType().name(),
                        "certificateId", issueResponse.certificate().id(),
                        "serialNumber", issueResponse.certificate().serialNumber(),
                        "status", entity.getStatus().name()
                )
        );

        return new ApproveCertificateRequestResponse(
                "Certificate request approved and certificate issued successfully",
                toResponse(entity),
                issueResponse.certificate()
        );
    }

    private ApproveCertificateRequestResponse approveRenewalRequest(CurrentUser adminUser,
                                                                    CertificateRequestEntity entity) {
        RenewalRequestPayload renewalPayload = readPayload(
                entity.getRequestPayload(),
                RenewalRequestPayload.class,
                "renewal request"
        );

        if (renewalPayload.sourceCertificateId() == null) {
            throw new BadRequestException("Renewal request does not contain source certificate information");
        }

        CertificateEntity sourceCertificate = certificateRepository.findById(renewalPayload.sourceCertificateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Source certificate not found: " + renewalPayload.sourceCertificateId()
                ));

        if (!sourceCertificate.getAccountId().equals(entity.getAccountId())) {
            throw new BadRequestException("Source certificate does not belong to the same account as the request");
        }

        if (sourceCertificate.getStatus() == CertificateStatus.REVOKED) {
            throw new BadRequestException("Cannot approve renewal for a revoked certificate");
        }

        if (certificateRepository.existsByRenewedFromCertificateId(sourceCertificate.getId())) {
            throw new BadRequestException("Certificate has already been renewed; approve renewal from the latest certificate instead");
        }

        CurrentUser requestOwner = new CurrentUser(
                entity.getAccountId(),
                "request-owner",
                List.of("ROLE_USER")
        );

        IssueCertificateRequest issueRequest = new IssueCertificateRequest(
                sourceCertificate.getCommonName(),
                renewalPayload.ttl(),
                firstNonBlank(
                        renewalPayload.organization(),
                        X509CertificateUtils.extractOrganization(sourceCertificate.getSubjectDn())
                ),
                firstNonBlank(
                        renewalPayload.country(),
                        X509CertificateUtils.extractCountry(sourceCertificate.getSubjectDn())
                )
        );

        IssueCertificateResponse issueResponse = certificateService.issueCertificate(
                requestOwner,
                issueRequest,
                sourceCertificate.getId()
        );

        certificateService.revokeCertificate(adminUser, sourceCertificate.getId());

        entity.setStatus(CertificateRequestStatus.ISSUED);
        entity.setResultCertificateId(issueResponse.certificate().id());
        entity.setReviewedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setReviewedByAccountId(adminUser.accountId());
        entity.setRejectionReason(null);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        certificateRequestRepository.save(entity);

        publishAudit(
                "CERTIFICATE_RENEWAL_APPROVED",
                adminUser.accountId(),
                "ADMIN",
                entity.getId(),
                "CERTIFICATE_REQUEST",
                entity.getId(),
                "Renewal request approved and new certificate issued",
                payload(
                        "requestType", entity.getRequestType().name(),
                        "sourceCertificateId", sourceCertificate.getId(),
                        "newCertificateId", issueResponse.certificate().id(),
                        "serialNumber", issueResponse.certificate().serialNumber(),
                        "status", entity.getStatus().name()
                )
        );

        return new ApproveCertificateRequestResponse(
                "Renewal request approved and new certificate issued successfully",
                toResponse(entity),
                issueResponse.certificate()
        );
    }

    private ApproveCertificateRequestResponse approveRevocationRequest(CurrentUser adminUser,
                                                                       CertificateRequestEntity entity) {
        RevocationRequestPayload revocationPayload = readPayload(
                entity.getRequestPayload(),
                RevocationRequestPayload.class,
                "revocation request"
        );

        if (revocationPayload.sourceCertificateId() == null) {
            throw new BadRequestException("Revocation request does not contain source certificate information");
        }

        CertificateEntity sourceCertificate = certificateRepository.findById(revocationPayload.sourceCertificateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Source certificate not found: " + revocationPayload.sourceCertificateId()
                ));

        if (!sourceCertificate.getAccountId().equals(entity.getAccountId())) {
            throw new BadRequestException("Source certificate does not belong to the same account as the request");
        }

        if (sourceCertificate.getStatus() == CertificateStatus.REVOKED) {
            throw new BadRequestException("Cannot approve revocation for a revoked certificate");
        }

        certificateService.revokeCertificate(adminUser, sourceCertificate.getId());

        entity.setStatus(CertificateRequestStatus.REVOKED);
        entity.setResultCertificateId(null);
        entity.setReviewedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setReviewedByAccountId(adminUser.accountId());
        entity.setRejectionReason(null);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        certificateRequestRepository.save(entity);

        CertificateResponse revokedCertificate = certificateService.getCertificateById(adminUser, sourceCertificate.getId());

        publishAudit(
                "CERTIFICATE_REVOCATION_REQUEST_APPROVED",
                adminUser.accountId(),
                "ADMIN",
                entity.getId(),
                "CERTIFICATE_REQUEST",
                entity.getId(),
                "Revocation request approved and certificate revoked",
                payload(
                        "requestType", entity.getRequestType().name(),
                        "sourceCertificateId", sourceCertificate.getId(),
                        "certificateId", revokedCertificate.id(),
                        "serialNumber", revokedCertificate.serialNumber(),
                        "status", entity.getStatus().name()
                )
        );

        return new ApproveCertificateRequestResponse(
                "Revocation request approved and certificate revoked successfully",
                toResponse(entity),
                revokedCertificate
        );
    }

    private CertificateRequestResponse toResponse(CertificateRequestEntity entity) {
        return new CertificateRequestResponse(
                entity.getId(),
                entity.getAccountId(),
                entity.getRequestType().name(),
                readPayloadForResponse(entity.getRequestPayload()),
                entity.getRequestReason(),
                entity.getStatus().name(),
                entity.getResultCertificateId(),
                entity.getRequestedAt(),
                entity.getReviewedAt(),
                entity.getReviewedByAccountId(),
                entity.getRejectionReason(),
                entity.getUpdatedAt()
        );
    }

    private CertificateRequestStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }

        try {
            return CertificateRequestStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid certificate request status: " + status);
        }
    }

    private CertificateRequestType parseRequestType(String requestType) {
        if (!StringUtils.hasText(requestType)) {
            return null;
        }

        try {
            return CertificateRequestType.valueOf(requestType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid certificate request type: " + requestType);
        }
    }

    private void requireAuthenticatedUser(CurrentUser currentUser) {
        if (currentUser == null || currentUser.accountId() == null) {
            throw new ForbiddenException("Cannot resolve current user from JWT");
        }
    }

    private void requireAdmin(CurrentUser currentUser) {
        requireAuthenticatedUser(currentUser);
        if (!currentUser.isAdmin()) {
            throw new ForbiddenException("Only admin can perform this action");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeCountry(String country) {
        String normalized = normalize(country);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : normalize(second);
    }

    private Optional<CertificateEntity> findCurrentActiveCertificate(UUID accountId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return certificateRepository.findAllByAccountIdOrderByIssuedAtDesc(accountId)
                .stream()
                .filter(certificate -> certificate.getStatus() == CertificateStatus.ACTIVE)
                .filter(certificate -> certificate.getExpiresAt() == null || certificate.getExpiresAt().isAfter(now))
                .findFirst();
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Failed to serialize certificate request payload");
        }
    }

    private <T> T readPayload(String requestPayload, Class<T> payloadType, String context) {
        if (!StringUtils.hasText(requestPayload)) {
            throw new BadRequestException("Missing payload for " + context);
        }

        try {
            return objectMapper.readValue(requestPayload, payloadType);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid payload for " + context);
        }
    }

    private JsonNode readPayloadForResponse(String requestPayload) {
        if (!StringUtils.hasText(requestPayload)) {
            return null;
        }

        try {
            return objectMapper.readTree(requestPayload);
        } catch (JsonProcessingException ex) {
            return TextNode.valueOf(requestPayload);
        }
    }

    private void publishAudit(String eventType,
                              UUID actorId,
                              String actorRole,
                              UUID targetId,
                              String resourceType,
                              UUID resourceId,
                              String summary,
                              Map<String, Object> payload) {
        auditGateway.publishEvent(
                eventType,
                actorId,
                actorRole,
                targetId,
                resourceType,
                resourceId,
                resourceId == null ? null : resourceId.toString(),
                summary,
                new LinkedHashMap<>(payload)
        );
    }

    private LinkedHashMap<String, Object> payload(Object... keyValues) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key != null && value != null) {
                payload.put(String.valueOf(key), value);
            }
        }
        return payload;
    }
}
