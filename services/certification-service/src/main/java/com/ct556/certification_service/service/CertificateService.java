package com.ct556.certification_service.service;

import com.ct556.certification_service.client.AuditGateway;
import com.ct556.certification_service.config.VaultProperties;
import com.ct556.certification_service.domain.CertificateEntity;
import com.ct556.certification_service.domain.CertificateStatus;
import com.ct556.certification_service.dto.request.IssueCertificateRequest;
import com.ct556.certification_service.dto.response.CertificateResponse;
import com.ct556.certification_service.dto.response.IssueCertificateResponse;
import com.ct556.certification_service.dto.response.RevokeCertificateResponse;
import com.ct556.certification_service.exception.BadRequestException;
import com.ct556.certification_service.exception.ForbiddenException;
import com.ct556.certification_service.exception.ResourceNotFoundException;
import com.ct556.certification_service.repository.CertificateRepository;
import com.ct556.certification_service.security.CurrentUser;
import com.ct556.certification_service.util.PemPublicKeyUtils;
import com.ct556.certification_service.util.X509CertificateUtils;
import com.ct556.certification_service.vault.VaultPkiClient;
import com.ct556.certification_service.vault.VaultTransitContentSigner;
import com.ct556.certification_service.vault.dto.VaultIssueCertificateResponse;
import com.ct556.certification_service.vault.dto.VaultReadCertificateResponse;
import com.ct556.certification_service.vault.dto.VaultRevokeCertificateResponse;
import com.ct556.certification_service.vault.dto.VaultSignCertificateRequest;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.StringWriter;
import java.security.PublicKey;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final VaultPkiClient vaultPkiClient;
    private final VaultProperties vaultProperties;
    private final AuditGateway auditGateway;

    public CertificateService(CertificateRepository certificateRepository,
                              VaultPkiClient vaultPkiClient,
                              VaultProperties vaultProperties,
                              AuditGateway auditGateway) {
        this.certificateRepository = certificateRepository;
        this.vaultPkiClient = vaultPkiClient;
        this.vaultProperties = vaultProperties;
        this.auditGateway = auditGateway;
    }

    public IssueCertificateResponse issueCertificate(CurrentUser currentUser, IssueCertificateRequest request) {
        return issueCertificate(currentUser, request, null);
    }

    public IssueCertificateResponse issueCertificate(CurrentUser currentUser,
                                                     IssueCertificateRequest request,
                                                     UUID renewedFromCertificateId) {
        requireCurrentUser(currentUser);

        UUID certificateId = UUID.randomUUID();
        String transitKeyName = buildCertificateTransitKeyName(certificateId);
        vaultPkiClient.createTransitRsaKey(transitKeyName);

        String publicKeyPem = vaultPkiClient.readTransitPublicKeyPem(transitKeyName);
        String csrPem = buildCertificateCsr(request, transitKeyName, publicKeyPem);

        VaultSignCertificateRequest vaultRequest = new VaultSignCertificateRequest(
                csrPem,
                requireCommonName(request.commonName()),
                normalize(request.ttl()),
                normalize(request.organization()),
                normalizeCountry(request.country())
        );

        VaultIssueCertificateResponse.Data vaultData = vaultPkiClient.signCertificateCsr(vaultRequest);

        if (!StringUtils.hasText(vaultData.certificate())) {
            throw new BadRequestException("Vault did not return certificate PEM");
        }

        if (!StringUtils.hasText(vaultData.serialNumber())) {
            throw new BadRequestException("Vault did not return serial number");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        CertificateEntity entity = new CertificateEntity();
        entity.setId(certificateId);
        entity.setAccountId(currentUser.accountId());
        entity.setCommonName(normalize(request.commonName()));
        entity.setSubjectDn(X509CertificateUtils.extractSubjectDn(vaultData.certificate()));
        entity.setSerialNumber(X509CertificateUtils.normalizeSerialNumber(vaultData.serialNumber()));
        entity.setVaultCertificatePath(vaultProperties.mountPath() + "/cert/" + X509CertificateUtils.normalizeSerialNumber(vaultData.serialNumber()));
        entity.setVaultTransitKeyName(transitKeyName);
        entity.setCertificatePem(vaultData.certificate());
        entity.setIssuingCaPem(vaultData.issuingCa());
        entity.setStatus(CertificateStatus.ACTIVE);
        entity.setRenewedFromCertificateId(renewedFromCertificateId);
        entity.setIssuedAt(X509CertificateUtils.extractIssuedAt(vaultData.certificate()));
        entity.setExpiresAt(X509CertificateUtils.extractExpiresAt(vaultData.certificate()));
        entity.setRevokedAt(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        certificateRepository.save(entity);

        return new IssueCertificateResponse(
                "Certificate issued successfully",
                toResponse(entity)
        );
    }

    @Transactional(readOnly = true)
    public CertificateResponse getCertificateById(CurrentUser currentUser, UUID certificateId) {
        requireCurrentUser(currentUser);
        CertificateEntity entity = findAccessibleCertificateById(currentUser, certificateId);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> listMyCertificates(CurrentUser currentUser) {
        requireCurrentUser(currentUser);

        return certificateRepository.findAllByAccountIdOrderByIssuedAtDesc(currentUser.accountId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> listAllCertificates() {
        return certificateRepository.findAllByOrderByIssuedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateResponse getCertificateBySerial(CurrentUser currentUser, String serialNumber) {
        requireCurrentUser(currentUser);

        String normalizedSerial = X509CertificateUtils.normalizeSerialLookup(serialNumber);

        CertificateEntity entity = currentUser.isAdmin()
                ? certificateRepository.findByNormalizedSerialNumber(normalizedSerial)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with serial: " + normalizedSerial))
                : certificateRepository.findByNormalizedSerialNumberAndAccountId(normalizedSerial, currentUser.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with serial: " + normalizedSerial));

        return toResponse(entity);
    }

    public RevokeCertificateResponse revokeCertificate(CurrentUser currentUser, UUID certificateId) {
        requireCurrentUser(currentUser);

        CertificateEntity entity = findAccessibleCertificateById(currentUser, certificateId);

        if (entity.getStatus() == CertificateStatus.REVOKED) {
            throw new BadRequestException("Certificate is already revoked");
        }

        VaultRevokeCertificateResponse.Data vaultRevokeData =
                vaultPkiClient.revokeCertificateBySerial(entity.getSerialNumber());

        OffsetDateTime revokedAt = vaultRevokeData.revocationTime() != null
                ? OffsetDateTime.ofInstant(Instant.ofEpochSecond(vaultRevokeData.revocationTime()), ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC);

        entity.setStatus(CertificateStatus.REVOKED);
        entity.setRevokedAt(revokedAt);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        certificateRepository.save(entity);

        publishAudit(
                "CERTIFICATE_REVOKED",
                currentUser.accountId(),
                currentUser.isAdmin() ? "ADMIN" : "USER",
                entity.getId(),
                "CERTIFICATE",
                entity.getId(),
                "Certificate revoked successfully",
                payload(
                        "accountId", entity.getAccountId(),
                        "serialNumber", entity.getSerialNumber(),
                        "status", entity.getStatus().name(),
                        "revokedAt", entity.getRevokedAt()
                )
        );

        return new RevokeCertificateResponse(
                "Certificate revoked successfully",
                entity.getId(),
                entity.getStatus().name(),
                entity.getRevokedAt()
        );
    }

    private CertificateEntity findAccessibleCertificateById(CurrentUser currentUser, UUID certificateId) {
        CertificateEntity entity = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateId));

        if (!currentUser.isAdmin() && !entity.getAccountId().equals(currentUser.accountId())) {
            throw new ForbiddenException("You are not allowed to access this certificate");
        }

        fillCertificatePemFromVaultIfMissing(entity);

        return entity;
    }

    private void fillCertificatePemFromVaultIfMissing(CertificateEntity entity) {
        if (StringUtils.hasText(entity.getCertificatePem())) {
            return;
        }

        VaultReadCertificateResponse.Data vaultCertificate = vaultPkiClient.readCertificateBySerial(entity.getSerialNumber());

        if (vaultCertificate != null && StringUtils.hasText(vaultCertificate.certificate())) {
            entity.setCertificatePem(vaultCertificate.certificate());
            entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            certificateRepository.save(entity);
        }
    }

    private CertificateResponse toResponse(CertificateEntity entity) {
        return new CertificateResponse(
                entity.getId(),
                entity.getAccountId(),
                entity.getCommonName(),
                entity.getSubjectDn(),
                entity.getSerialNumber(),
                entity.getVaultCertificatePath(),
                entity.getCertificatePem(),
                entity.getIssuingCaPem(),
                resolveDisplayStatus(entity).name(),
                entity.getRenewedFromCertificateId(),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String buildCertificateTransitKeyName(UUID certificateId) {
        String prefix = StringUtils.hasText(vaultProperties.transitKeyPrefix())
                ? vaultProperties.transitKeyPrefix().trim()
                : "cert";
        return prefix + "-" + certificateId;
    }

    private String buildCertificateCsr(IssueCertificateRequest request,
                                       String transitKeyName,
                                       String publicKeyPem) {
        try {
            PublicKey publicKey = PemPublicKeyUtils.parsePublicKey(publicKeyPem);

            X500NameBuilder subjectBuilder = new X500NameBuilder(BCStyle.INSTANCE);
            subjectBuilder.addRDN(BCStyle.CN, requireCommonName(request.commonName()));

            String organization = normalize(request.organization());
            if (StringUtils.hasText(organization)) {
                subjectBuilder.addRDN(BCStyle.O, organization);
            }

            String country = normalizeCountry(request.country());
            if (StringUtils.hasText(country)) {
                subjectBuilder.addRDN(BCStyle.C, country);
            }

            PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(subjectBuilder.build(), publicKey)
                    .build(new VaultTransitContentSigner(vaultPkiClient, transitKeyName));
            return toPem(csr);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to build certificate CSR with Vault Transit key");
        }
    }

    private String toPem(Object object) throws Exception {
        try (StringWriter writer = new StringWriter(); JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(object);
            pemWriter.flush();
            return writer.toString();
        }
    }

    private CertificateStatus resolveDisplayStatus(CertificateEntity entity) {
        if (entity.getStatus() == CertificateStatus.ACTIVE
                && entity.getExpiresAt() != null
                && entity.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            return CertificateStatus.EXPIRED;
        }
        return entity.getStatus();
    }

    private void requireCurrentUser(CurrentUser currentUser) {
        if (currentUser == null || currentUser.accountId() == null) {
            throw new ForbiddenException("Cannot resolve current user from JWT");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String requireCommonName(String commonName) {
        String normalized = normalize(commonName);
        if (!StringUtils.hasText(normalized)) {
            throw new BadRequestException("commonName is required");
        }
        return normalized;
    }

    private String normalizeCountry(String country) {
        String normalized = normalize(country);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
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
