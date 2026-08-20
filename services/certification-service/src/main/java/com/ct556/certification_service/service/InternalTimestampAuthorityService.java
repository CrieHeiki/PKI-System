package com.ct556.certification_service.service;

import com.ct556.certification_service.client.AuditGateway;
import com.ct556.certification_service.config.VaultProperties;
import com.ct556.certification_service.domain.TrustedTimestampAuthorityEntity;
import com.ct556.certification_service.domain.TrustedTimestampAuthorityStatus;
import com.ct556.certification_service.dto.request.InternalTimestampTokenRequest;
import com.ct556.certification_service.dto.response.InternalTimestampAuthorityMetadataResponse;
import com.ct556.certification_service.dto.response.InternalTimestampTokenResponse;
import com.ct556.certification_service.exception.BadRequestException;
import com.ct556.certification_service.exception.ResourceNotFoundException;
import com.ct556.certification_service.repository.TrustedTimestampAuthorityRepository;
import com.ct556.certification_service.util.PemPublicKeyUtils;
import com.ct556.certification_service.util.X509CertificateUtils;
import com.ct556.certification_service.vault.VaultPkiClient;
import com.ct556.certification_service.vault.VaultTransitContentSigner;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.SignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenGenerator;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class InternalTimestampAuthorityService {

    private static final BouncyCastleProvider BC_PROVIDER = new BouncyCastleProvider();
    private static final String DEFAULT_COMMON_NAME = "CT556 Internal TSA";
    private static final String DEFAULT_POLICY_OID = "1.3.6.1.4.1.55556.1.1";
    private static final boolean DEFAULT_ORDERING_ENABLED = true;
    private static final int DEFAULT_ACCURACY_SECONDS = 1;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TrustedTimestampAuthorityRepository repository;
    private final VaultPkiClient vaultPkiClient;
    private final VaultProperties vaultProperties;
    private final AuditGateway auditGateway;

    public InternalTimestampAuthorityService(TrustedTimestampAuthorityRepository repository,
                                             VaultPkiClient vaultPkiClient,
                                             VaultProperties vaultProperties,
                                             AuditGateway auditGateway) {
        this.repository = repository;
        this.vaultPkiClient = vaultPkiClient;
        this.vaultProperties = vaultProperties;
        this.auditGateway = auditGateway;
    }

    public InternalTimestampTokenResponse issueTimestampToken(InternalTimestampTokenRequest request) {
        TrustedTimestampAuthorityEntity authority = getOrProvisionActiveAuthority();
        byte[] messageImprint = decodeBase64(request.messageImprintBase64());
        ASN1ObjectIdentifier digestOid = resolveDigestOid(request.digestAlgorithm());
        OffsetDateTime generatedAt = OffsetDateTime.now(ZoneOffset.UTC);

        try {
            TimeStampToken token = generateTimestampToken(
                    authority,
                    authority.getVaultTransitKeyName(),
                    messageImprint,
                    digestOid,
                    generatedAt.toInstant()
            );
            return new InternalTimestampTokenResponse(
                    Base64.getEncoder().encodeToString(token.getEncoded()),
                    normalizeDigestAlgorithm(request.digestAlgorithm()),
                    generatedAt,
                    authority.getSerialNumber(),
                    authority.getSubjectDn(),
                    authority.getCertificatePem()
            );
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to generate timestamp token");
        }
    }

    public InternalTimestampAuthorityMetadataResponse getActiveAuthorityMetadata() {
        return toMetadata(getOrProvisionActiveAuthority());
    }

    public InternalTimestampAuthorityMetadataResponse getAuthorityMetadataBySerial(String serialNumber) {
        String normalizedSerial = X509CertificateUtils.normalizeSerialLookup(serialNumber);
        TrustedTimestampAuthorityEntity authority = repository.findByNormalizedSerialNumber(normalizedSerial)
                .orElseThrow(() -> new ResourceNotFoundException("Timestamp authority not found with serial: " + normalizedSerial));
        return toMetadata(authority);
    }

    private TrustedTimestampAuthorityEntity getOrProvisionActiveAuthority() {
        return repository.findFirstByStatusOrderByCreatedAtDesc(TrustedTimestampAuthorityStatus.ACTIVE)
                .filter(this::isUsable)
                .orElseGet(this::provisionActiveAuthority);
    }

    private boolean isUsable(TrustedTimestampAuthorityEntity authority) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return authority.getStatus() == TrustedTimestampAuthorityStatus.ACTIVE
                && authority.getRevokedAt() == null
                && authority.getExpiresAt() != null
                && now.isBefore(authority.getExpiresAt());
    }

    private TrustedTimestampAuthorityEntity provisionActiveAuthority() {
        repository.findFirstByStatusOrderByCreatedAtDesc(TrustedTimestampAuthorityStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(TrustedTimestampAuthorityStatus.ROTATED);
                    existing.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    repository.save(existing);
                });

        try {
            OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime expiresAt = issuedAt.plusDays(365);
            UUID authorityId = UUID.randomUUID();
            String transitKeyName = buildTimestampAuthorityTransitKeyName(authorityId);
            vaultPkiClient.createTransitRsaKey(transitKeyName);
            PublicKey publicKey = PemPublicKeyUtils.parsePublicKey(vaultPkiClient.readTransitPublicKeyPem(transitKeyName));
            X509Certificate certificate = generateSelfSignedAuthorityCertificate(
                    publicKey,
                    transitKeyName,
                    issuedAt.toInstant(),
                    expiresAt.toInstant()
            );

            TrustedTimestampAuthorityEntity authority = new TrustedTimestampAuthorityEntity();
            authority.setId(authorityId);
            authority.setCommonName(DEFAULT_COMMON_NAME);
            authority.setSubjectDn(certificate.getSubjectX500Principal().getName());
            authority.setSerialNumber(X509CertificateUtils.normalizeSerialNumber(certificate.getSerialNumber().toString(16).toUpperCase(Locale.ROOT)));
            authority.setVaultTransitKeyName(transitKeyName);
            authority.setCertificatePem(toPem(certificate));
            authority.setStatus(TrustedTimestampAuthorityStatus.ACTIVE);
            authority.setPolicyOid(DEFAULT_POLICY_OID);
            authority.setOrderingEnabled(DEFAULT_ORDERING_ENABLED);
            authority.setAccuracySeconds(DEFAULT_ACCURACY_SECONDS);
            authority.setIssuedAt(issuedAt);
            authority.setExpiresAt(expiresAt);
            authority.setRevokedAt(null);
            authority.setCreatedAt(issuedAt);
            authority.setUpdatedAt(issuedAt);
            repository.save(authority);

            publishAudit(
                    "TIMESTAMP_AUTHORITY_PROVISIONED",
                    authority.getId(),
                    "SYSTEM",
                    authority.getId(),
                    "TSA",
                    authority.getId(),
                    "Internal TSA authority was provisioned",
                    payload(
                            "serialNumber", authority.getSerialNumber(),
                            "subjectDn", authority.getSubjectDn(),
                            "issuedAt", authority.getIssuedAt(),
                            "expiresAt", authority.getExpiresAt()
                    )
            );

            return authority;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to provision internal timestamp authority");
        }
    }

    private X509Certificate generateSelfSignedAuthorityCertificate(PublicKey publicKey,
                                                                   String transitKeyName,
                                                                   Instant issuedAt,
                                                                   Instant expiresAt) throws Exception {
        X500Name subject = new X500Name("CN=" + DEFAULT_COMMON_NAME + ", O=CT556 Internal PKI, C=VN");
        BigInteger serialNumber = new BigInteger(160, SECURE_RANDOM).abs();
        if (serialNumber.signum() <= 0) {
            serialNumber = serialNumber.negate().add(BigInteger.ONE);
        }

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject,
                serialNumber,
                java.util.Date.from(issuedAt.minusSeconds(300)),
                java.util.Date.from(expiresAt),
                subject,
                publicKey
        );

        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
        builder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));
        builder.addExtension(Extension.subjectKeyIdentifier, false, extensionUtils.createSubjectKeyIdentifier(publicKey));
        builder.addExtension(Extension.authorityKeyIdentifier, false, extensionUtils.createAuthorityKeyIdentifier(publicKey));

        ContentSigner contentSigner = new VaultTransitContentSigner(vaultPkiClient, transitKeyName);

        X509CertificateHolder holder = builder.build(contentSigner);
        return new JcaX509CertificateConverter()
                .setProvider(BC_PROVIDER)
                .getCertificate(holder);
    }

    private String buildTimestampAuthorityTransitKeyName(UUID authorityId) {
        String prefix = vaultProperties.transitKeyPrefix() == null || vaultProperties.transitKeyPrefix().isBlank()
                ? "cert"
                : vaultProperties.transitKeyPrefix().trim();
        return prefix + "-tsa-" + authorityId;
    }

    private TimeStampToken generateTimestampToken(TrustedTimestampAuthorityEntity authority,
                                                  String vaultTransitKeyName,
                                                  byte[] messageImprint,
                                                  ASN1ObjectIdentifier digestOid,
                                                  Instant generatedAt) throws Exception {
        X509Certificate certificate = parseCertificate(authority.getCertificatePem());

        SignerInfoGenerator signerInfoGenerator = new SignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder()
                        .setProvider(BC_PROVIDER)
                        .build()
        ).build(
                new VaultTransitContentSigner(vaultPkiClient, vaultTransitKeyName),
                new X509CertificateHolder(certificate.getEncoded())
        );

        DigestCalculator digestCalculator = new JcaDigestCalculatorProviderBuilder()
                .setProvider(BC_PROVIDER)
                .build()
                .get(new org.bouncycastle.asn1.x509.AlgorithmIdentifier(digestOid));

        TimeStampTokenGenerator tokenGenerator = new TimeStampTokenGenerator(
                signerInfoGenerator,
                digestCalculator,
                new ASN1ObjectIdentifier(authority.getPolicyOid())
        );
        tokenGenerator.addCertificates(new JcaCertStore(List.of(certificate)));
        tokenGenerator.setAccuracySeconds(authority.getAccuracySeconds());
        tokenGenerator.setOrdering(authority.isOrderingEnabled());

        TimeStampRequest request = new TimeStampRequestGenerator() {{
            setCertReq(true);
        }}.generate(digestOid, messageImprint, BigInteger.valueOf(Math.abs(SECURE_RANDOM.nextLong())));

        return tokenGenerator.generate(request, BigInteger.valueOf(Math.abs(SECURE_RANDOM.nextLong())), java.util.Date.from(generatedAt));
    }

    private InternalTimestampAuthorityMetadataResponse toMetadata(TrustedTimestampAuthorityEntity authority) {
        return InternalTimestampAuthorityMetadataResponse.builder()
                .authorityId(authority.getId())
                .subjectDn(authority.getSubjectDn())
                .serialNumber(authority.getSerialNumber())
                .certificatePem(authority.getCertificatePem())
                .status(authority.getStatus().name())
                .validFrom(authority.getIssuedAt())
                .validTo(authority.getExpiresAt())
                .revokedAt(authority.getRevokedAt())
                .build();
    }

    private byte[] decodeBase64(String contentBase64) {
        try {
            return Base64.getDecoder().decode(contentBase64);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("messageImprintBase64 is not valid Base64");
        }
    }

    private X509Certificate parseCertificate(String certificatePem) {
        try {
            String normalized = certificatePem
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(normalized);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(der));
        } catch (Exception ex) {
            throw new BadRequestException("Failed to parse TSA certificate PEM");
        }
    }

    private ASN1ObjectIdentifier resolveDigestOid(String digestAlgorithm) {
        return switch (normalizeDigestAlgorithm(digestAlgorithm)) {
            case "SHA-256" -> org.bouncycastle.asn1.nist.NISTObjectIdentifiers.id_sha256;
            case "SHA-384" -> org.bouncycastle.asn1.nist.NISTObjectIdentifiers.id_sha384;
            case "SHA-512" -> org.bouncycastle.asn1.nist.NISTObjectIdentifiers.id_sha512;
            default -> throw new BadRequestException("Unsupported timestamp digest algorithm");
        };
    }

    private String normalizeDigestAlgorithm(String digestAlgorithm) {
        if (digestAlgorithm == null || digestAlgorithm.isBlank()) {
            return "SHA-256";
        }
        return switch (digestAlgorithm.trim().toUpperCase(Locale.ROOT).replace("_", "-")) {
            case "SHA256", "SHA-256" -> "SHA-256";
            case "SHA384", "SHA-384" -> "SHA-384";
            case "SHA512", "SHA-512" -> "SHA-512";
            default -> throw new BadRequestException("Unsupported timestamp digest algorithm");
        };
    }

    private String toPem(X509Certificate certificate) throws Exception {
        try (StringWriter writer = new StringWriter(); JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(certificate);
            pemWriter.flush();
            return writer.toString();
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
