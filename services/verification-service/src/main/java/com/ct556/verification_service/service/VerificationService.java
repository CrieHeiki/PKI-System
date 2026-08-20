package com.ct556.verification_service.service;

import com.ct556.verification_service.client.AuditServiceClient;
import com.ct556.verification_service.client.CertificationServiceClient;
import com.ct556.verification_service.client.SigningWorkflowClient;
import com.ct556.verification_service.client.dto.InternalCertificateMetadataResponse;
import com.ct556.verification_service.client.dto.InternalSigningArtifactResponse;
import com.ct556.verification_service.client.dto.InternalTimestampAuthorityMetadataResponse;
import com.ct556.verification_service.dto.request.VerifySignedOutputRequest;
import com.ct556.verification_service.dto.request.VerifySigningRequestRequest;
import com.ct556.verification_service.dto.response.VerificationCheckResponse;
import com.ct556.verification_service.dto.response.VerificationReportResponse;
import com.ct556.verification_service.entity.VerificationReport;
import com.ct556.verification_service.entity.VerificationReportDetail;
import com.ct556.verification_service.entity.VerificationStatus;
import com.ct556.verification_service.exception.ResourceNotFoundException;
import com.ct556.verification_service.repository.VerificationReportRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.DefaultAlgorithmNameFinder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class VerificationService {

    private static final String DEFAULT_TRUST_SOURCE = "INTERNAL_CA";
    private static final String DEFAULT_REVOCATION_SOURCE = "CERTIFICATION_SERVICE";
    private static final BouncyCastleProvider BC_PROVIDER = new BouncyCastleProvider();
    private static final DefaultAlgorithmNameFinder ALGORITHM_NAME_FINDER = new DefaultAlgorithmNameFinder();

    private final VerificationReportRepository verificationReportRepository;
    private final MinioStorageService minioStorageService;
    private final CertificationServiceClient certificationServiceClient;
    private final SigningWorkflowClient signingWorkflowClient;
    private final AuditServiceClient auditServiceClient;
    private final ObjectMapper objectMapper;
    private final ExternalCaTrustValidationService externalCaTrustValidationService;

    public VerificationService(VerificationReportRepository verificationReportRepository,
                               MinioStorageService minioStorageService,
                               CertificationServiceClient certificationServiceClient,
                               SigningWorkflowClient signingWorkflowClient,
                               AuditServiceClient auditServiceClient,
                               ObjectMapper objectMapper,
                               ExternalCaTrustValidationService externalCaTrustValidationService) {
        this.verificationReportRepository = verificationReportRepository;
        this.minioStorageService = minioStorageService;
        this.certificationServiceClient = certificationServiceClient;
        this.signingWorkflowClient = signingWorkflowClient;
        this.auditServiceClient = auditServiceClient;
        this.objectMapper = objectMapper;
        this.externalCaTrustValidationService = externalCaTrustValidationService;
    }

    @Transactional
    public VerificationReportResponse verifySignedOutput(VerifySignedOutputRequest request) {
        return verifyStoredSignedOutputInternal(
                request.getObjectKey(),
                request.getVerifiedBy(),
                request.getTrustSource(),
                null,
                List.of()
        );
    }

    @Transactional
    public VerificationReportResponse verifySigningRequest(UUID requestId, VerifySigningRequestRequest request) {
        InternalSigningArtifactResponse artifact = signingWorkflowClient.getSigningArtifact(requestId);
        VerifySigningRequestRequest normalizedRequest = request == null ? new VerifySigningRequestRequest() : request;

        return verifyStoredSignedOutputInternal(
                artifact.getOutputObjectKey(),
                normalizedRequest.getVerifiedBy(),
                normalizedRequest.getTrustSource(),
                artifact,
                List.of()
        );
    }

    @Transactional
    public VerificationReportResponse verifyUploadedFile(MultipartFile file,
                                                         UUID verifiedBy,
                                                         String trustSource,
                                                         List<MultipartFile> trustAnchors) {
        if (file == null || file.isEmpty()) {
            throw new ResourceNotFoundException("Uploaded file is required");
        }

        try {
            List<X509Certificate> uploadedTrustCertificates = "EXTERNAL_CA".equals(normalizeTrustSource(trustSource))
                    ? externalCaTrustValidationService.parseUploadedCertificates(trustAnchors)
                    : List.of();
            return verifyBinaryContentInternal(
                    null,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes(),
                    verifiedBy,
                    trustSource,
                    null,
                    "Uploaded file received for verification",
                    uploadedTrustCertificates
            );
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot read uploaded file for verification", ex);
        }
    }

    private VerificationReportResponse verifyStoredSignedOutputInternal(String rawObjectKey,
                                                                        UUID verifiedBy,
                                                                        String trustSource,
                                                                        InternalSigningArtifactResponse artifactHint,
                                                                        List<X509Certificate> uploadedTrustCertificates) {
        String objectKey = rawObjectKey == null ? null : rawObjectKey.trim();
        if (isBlank(objectKey)) {
            throw new ResourceNotFoundException("Signed output objectKey is required");
        }
        if (!minioStorageService.objectExists(objectKey)) {
            throw new ResourceNotFoundException("Signed output not found in MinIO: " + objectKey);
        }

        InternalSigningArtifactResponse artifact = artifactHint != null ? artifactHint : tryResolveArtifactByObjectKey(objectKey);
        byte[] signedBytes = minioStorageService.getObjectBytes(objectKey);
        return verifyBinaryContentInternal(
                objectKey,
                artifact == null ? extractLeafName(objectKey) : artifact.getOutputFileName(),
                artifact == null ? null : artifact.getOutputContentType(),
                signedBytes,
                verifiedBy,
                trustSource,
                artifact,
                "Signed output package loaded from MinIO",
                uploadedTrustCertificates
        );
    }

    private VerificationReportResponse verifyBinaryContentInternal(String objectKey,
                                                                  String fileName,
                                                                  String contentType,
                                                                  byte[] signedBytes,
                                                                  UUID verifiedBy,
                                                                  String trustSource,
                                                                  InternalSigningArtifactResponse artifact,
                                                                  String loadMessage,
                                                                  List<X509Certificate> uploadedTrustCertificates) {
        SignatureContainerType containerType = detectContainerType(signedBytes, fileName, contentType, artifact);

        VerificationReport report = initializeReport(verifiedBy, trustSource, objectKey, fileName, contentType, signedBytes, artifact, loadMessage);
        addDetail(report, "signature_container_detected", containerType != SignatureContainerType.UNKNOWN,
                containerType == SignatureContainerType.UNKNOWN
                        ? "Signed output format is not recognized"
                        : "Detected signed output container: " + containerType.name(),
                rawData(
                        "containerType", containerType.name(),
                        "outputFileName", report.getSignedFileName(),
                        "outputContentType", firstNonBlank(contentType, artifact == null ? null : artifact.getOutputContentType())
                ));

        VerificationMaterial verificationMaterial = switch (containerType) {
            case PADES_PDF -> verifyPadesDocument(report, signedBytes, artifact);
            case CMS_CADES -> verifyCmsSignedOutput(report, signedBytes, artifact);
            case LEGACY_ZIP -> verifyLegacySignedOutput(report, signedBytes, artifact);
            case UNKNOWN -> {
                markMalformed(report, "Signed output format is not supported");
                yield null;
            }
        };

        if (verificationMaterial == null) {
            VerificationReport savedReport = verificationReportRepository.save(report);
            publishVerificationAudit(savedReport);
            return toResponse(savedReport);
        }

        TimestampValidationResult timestampValidation = verificationMaterial.signer() == null
                ? TimestampValidationResult.absent()
                : validateTrustedTimestamp(report, verificationMaterial.signer());
        OffsetDateTime trustedSigningTime = timestampValidation.referenceTime();

        if ("INTERNAL_CA".equals(report.getTrustSource())) {
            InternalCertificateMetadataResponse authorityMetadata = null;
            String authorityLookupMessage;
            try {
                authorityMetadata = resolveAuthorityMetadata(report);
                if (authorityMetadata != null) {
                    report.setCertificateId(firstNonNull(report.getCertificateId(), authorityMetadata.getCertificateId()));
                    authorityLookupMessage = "Certificate recognized by certification-service";
                } else {
                    authorityLookupMessage = "Embedded certificate is not recognized by certification-service";
                }
            } catch (Exception ex) {
                authorityLookupMessage = ex.getMessage();
            }

            boolean authorityRecognized = authorityMetadata != null;
            addDetail(report, "certificate_recognized_by_authority", authorityRecognized,
                    authorityLookupMessage,
                    rawData(
                            "certificateId", report.getCertificateId(),
                            "serialNumber", report.getSignerCertificateSerial(),
                            "trustSource", report.getTrustSource()
                    ));

            TrustCheck trustCheck = verifyTrust(authorityMetadata, verificationMaterial.embeddedCertificate(), report);
            report.setTrustChainValid(trustCheck.valid());
            addDetail(report, "certificate_pem_matches_authority", trustCheck.valid(), trustCheck.message(), trustCheck.rawData());

            ValidityCheck validityCheck = verifyCertificateValidity(authorityMetadata, verificationMaterial.embeddedCertificate(), trustedSigningTime);
            report.setExpirationValid(validityCheck.valid());
            addDetail(report, "certificate_time_valid", validityCheck.valid(), validityCheck.message(), validityCheck.rawData());

            RevocationCheck revocationCheck = verifyRevocation(authorityMetadata, trustedSigningTime);
            report.setRevocationValid(revocationCheck.valid());
            addDetail(report, "certificate_not_revoked", revocationCheck.valid(), revocationCheck.message(), revocationCheck.rawData());
        } else {
            ExternalCaTrustValidationService.ExternalTrustValidationResult externalValidation =
                    externalCaTrustValidationService.validate(
                            verificationMaterial.embeddedCertificate(),
                            verificationMaterial.embeddedCertificates(),
                            uploadedTrustCertificates
                    );

            addDetail(report, "external_trust_anchor_available",
                    externalValidation.trustAnchorAvailable(),
                    externalValidation.trustAnchorMessage(),
                    rawData(externalValidation.trustAnchorRawData()));

            report.setTrustChainValid(externalValidation.trustValid());
            addDetail(report, "external_trust_chain_validated",
                    externalValidation.trustValid(),
                    externalValidation.trustMessage(),
                    rawData(externalValidation.trustRawData()));

            ValidityCheck validityCheck = verifyCertificateValidity(null, verificationMaterial.embeddedCertificate(), trustedSigningTime);
            report.setExpirationValid(validityCheck.valid());
            addDetail(report, "certificate_time_valid", validityCheck.valid(), validityCheck.message(), validityCheck.rawData());

            report.setRevocationValid(externalValidation.revocationValid());
            addDetail(report, "external_certificate_not_revoked",
                    externalValidation.revocationValid(),
                    externalValidation.revocationMessage(),
                    rawData(externalValidation.revocationRawData()));
        }

        report.setVerificationStatus(resolveStatus(report));
        report.setOverallMessage(resolveOverallMessage(report, containerType));

        VerificationReport savedReport = verificationReportRepository.save(report);
        publishVerificationAudit(savedReport);
        return toResponse(savedReport);
    }

    @Transactional
    public VerificationReportResponse getReport(UUID reportId) {
        VerificationReport report = verificationReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification report not found: " + reportId));
        return toResponse(report);
    }

    @Transactional
    public VerificationReportResponse getLatestReportBySigningRequest(UUID requestId) {
        VerificationReport report = verificationReportRepository.findFirstBySigningRequestIdOrderByVerifiedAtDesc(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification report not found for signing request: " + requestId));
        return toResponse(report);
    }

    @Transactional
    public List<VerificationReportResponse> getRecentReports() {
        return verificationReportRepository.findTop20ByOrderByVerifiedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private VerificationMaterial verifyPadesDocument(VerificationReport report,
                                                     byte[] signedBytes,
                                                     InternalSigningArtifactResponse artifact) {
        try (PDDocument document = PDDocument.load(signedBytes)) {
            List<PDSignature> signatures = document.getSignatureDictionaries();
            if (signatures.isEmpty()) {
                markMalformed(report, "PDF does not contain any embedded digital signature");
                addDetail(report, "pdf_signature_present", false, "PDF does not contain any signature field", null);
                return null;
            }

            PDSignature pdfSignature = signatures.get(signatures.size() - 1);
            byte[] signedContent = pdfSignature.getSignedContent(signedBytes);
            byte[] cmsBytes = pdfSignature.getContents(signedBytes);
            CMSSignedData signedData = new CMSSignedData(new CMSProcessableByteArray(signedContent), cmsBytes);

            SignerInformation signer = extractSingleSigner(signedData);
            X509Certificate embeddedCertificate = extractSignerCertificate(signedData, signer);
            String digestAlgorithm = firstNonBlank(
                    artifact == null ? null : artifact.getDigestAlgorithm(),
                    normalizeAlgorithmName(signer.getDigestAlgOID())
            );
            String signatureAlgorithm = firstNonBlank(
                    artifact == null ? null : artifact.getSignatureAlgorithm(),
                    deriveSignatureAlgorithm(signer, digestAlgorithm)
            );

            report.setDocumentId(artifact == null ? null : artifact.getDocumentId());
            report.setSigningRequestId(artifact == null ? null : artifact.getSigningRequestId());
            report.setCertificateId(artifact == null ? null : artifact.getCertificateId());
            report.setOriginalFileName(firstNonBlank(artifact == null ? null : artifact.getFileName(), report.getSignedFileName()));
            report.setDigestAlgorithm(digestAlgorithm);
            report.setSignatureAlgorithm(signatureAlgorithm);
            applyCertificateMetadata(report, embeddedCertificate);

            addDetail(report, "pdf_signature_present", true, "Embedded PDF signature dictionary found",
                    rawData(
                            "signatureCount", signatures.size(),
                            "subFilter", pdfSignature.getSubFilter(),
                            "name", pdfSignature.getName()
                    ));

            AttributePresenceCheck cadesCheck = checkCadesAttributes(signer);
            addDetail(report, "cades_attributes_present", cadesCheck.valid(), cadesCheck.message(), cadesCheck.rawData());

            DigestCheck digestCheck = verifyCmsMessageDigest(signedContent, signer, digestAlgorithm);
            report.setIntegrityValid(digestCheck.valid());
            addDetail(report, "document_digest_matches", digestCheck.valid(), digestCheck.message(), digestCheck.rawData());

            SignatureCheck signatureCheck = verifyCmsSignature(signer, embeddedCertificate);
            report.setSignatureValid(signatureCheck.valid());
            addDetail(report, "signature_value_valid", signatureCheck.valid(), signatureCheck.message(), signatureCheck.rawData());

            return new VerificationMaterial(embeddedCertificate, extractEmbeddedCertificates(signedData), signer);
        } catch (Exception ex) {
            markMalformed(report, "Failed to verify embedded PDF signature: " + ex.getMessage());
            addDetail(report, "pdf_signature_parsed", false, "Failed to parse PAdES document", rawData("error", ex.getMessage()));
            return null;
        }
    }

    private VerificationMaterial verifyCmsSignedOutput(VerificationReport report,
                                                       byte[] signedBytes,
                                                       InternalSigningArtifactResponse artifact) {
        try {
            CMSSignedData signedData = new CMSSignedData(signedBytes);
            if (signedData.getSignedContent() == null || signedData.getSignedContent().getContent() == null) {
                markMalformed(report, "CMS signed-data does not contain encapsulated content");
                addDetail(report, "cms_encapsulated_content_present", false, "CMS package does not contain original content", null);
                return null;
            }

            byte[] originalContent = (byte[]) signedData.getSignedContent().getContent();
            SignerInformation signer = extractSingleSigner(signedData);
            X509Certificate embeddedCertificate = extractSignerCertificate(signedData, signer);
            String digestAlgorithm = firstNonBlank(
                    artifact == null ? null : artifact.getDigestAlgorithm(),
                    normalizeAlgorithmName(signer.getDigestAlgOID())
            );
            String signatureAlgorithm = firstNonBlank(
                    artifact == null ? null : artifact.getSignatureAlgorithm(),
                    deriveSignatureAlgorithm(signer, digestAlgorithm)
            );

            report.setDocumentId(artifact == null ? null : artifact.getDocumentId());
            report.setSigningRequestId(artifact == null ? null : artifact.getSigningRequestId());
            report.setCertificateId(artifact == null ? null : artifact.getCertificateId());
            report.setOriginalFileName(firstNonBlank(artifact == null ? null : artifact.getFileName(), stripP7mSuffix(report.getSignedFileName())));
            report.setDigestAlgorithm(digestAlgorithm);
            report.setSignatureAlgorithm(signatureAlgorithm);
            applyCertificateMetadata(report, embeddedCertificate);

            addDetail(report, "cms_encapsulated_content_present", true, "CMS signed-data contains encapsulated content",
                    rawData("contentLength", originalContent.length, "signedFileName", report.getSignedFileName()));

            AttributePresenceCheck cadesCheck = checkCadesAttributes(signer);
            addDetail(report, "cades_attributes_present", cadesCheck.valid(), cadesCheck.message(), cadesCheck.rawData());

            DigestCheck digestCheck = verifyCmsMessageDigest(originalContent, signer, digestAlgorithm);
            report.setIntegrityValid(digestCheck.valid());
            addDetail(report, "document_digest_matches", digestCheck.valid(), digestCheck.message(), digestCheck.rawData());

            SignatureCheck signatureCheck = verifyCmsSignature(signer, embeddedCertificate);
            report.setSignatureValid(signatureCheck.valid());
            addDetail(report, "signature_value_valid", signatureCheck.valid(), signatureCheck.message(), signatureCheck.rawData());

            return new VerificationMaterial(embeddedCertificate, extractEmbeddedCertificates(signedData), signer);
        } catch (Exception ex) {
            markMalformed(report, "Failed to verify CMS/CAdES signed output: " + ex.getMessage());
            addDetail(report, "cms_signed_data_parsed", false, "Failed to parse CMS/CAdES package", rawData("error", ex.getMessage()));
            return null;
        }
    }

    private VerificationMaterial verifyLegacySignedOutput(VerificationReport report,
                                                          byte[] packageBytes,
                                                          InternalSigningArtifactResponse artifact) {
        ParsedSignedPackage parsedPackage = parseLegacySignedPackage(packageBytes, report.getSignedFileName());

        addDetail(report, "original_payload_present",
                parsedPackage.originalContent() != null && parsedPackage.originalContent().length > 0,
                parsedPackage.originalContent() != null && parsedPackage.originalContent().length > 0
                        ? "Original payload entry found in signed package"
                        : "Signed package does not contain original payload entry",
                rawData("entryName", parsedPackage.originalEntryName(), "size",
                        parsedPackage.originalContent() == null ? null : parsedPackage.originalContent().length));

        addDetail(report, "signature_manifest_present",
                parsedPackage.manifestJson() != null && parsedPackage.manifestJson().length > 0,
                parsedPackage.manifestJson() != null && parsedPackage.manifestJson().length > 0
                        ? "signature.json entry found in signed package"
                        : "Signed package does not contain signature.json",
                rawData("entryName", "signature.json"));

        addDetail(report, "signature_manifest_parsed",
                parsedPackage.manifest() != null,
                parsedPackage.parseError() == null ? "signature.json parsed successfully" : parsedPackage.parseError(),
                rawData("signedFileName", parsedPackage.signedFileName()));

        if (parsedPackage.manifest() == null) {
            markMalformed(report, "Signed package is malformed and cannot be verified");
            return null;
        }

        LegacySignedPackageManifest manifest = parsedPackage.manifest();
        report.setDocumentId(firstNonNull(artifact == null ? null : artifact.getDocumentId(), manifest.documentId()));
        report.setSigningRequestId(firstNonNull(artifact == null ? null : artifact.getSigningRequestId(), manifest.signingRequestId()));
        report.setCertificateId(firstNonNull(artifact == null ? null : artifact.getCertificateId(), manifest.certificateId()));
        report.setOriginalFileName(firstNonBlank(artifact == null ? null : artifact.getFileName(),
                firstNonBlank(parsedPackage.originalEntryName(), manifest.originalFileName())));
        report.setDigestAlgorithm(firstNonBlank(artifact == null ? null : artifact.getDigestAlgorithm(), manifest.digestAlgorithm()));
        report.setSignatureAlgorithm(firstNonBlank(artifact == null ? null : artifact.getSignatureAlgorithm(), manifest.signatureAlgorithm()));

        X509Certificate embeddedCertificate = parseCertificate(manifest.certificatePem());
        addDetail(report, "certificate_present_in_manifest",
                embeddedCertificate != null,
                embeddedCertificate != null
                        ? "Embedded certificate found in signature manifest"
                        : "signature.json does not contain a valid certificate PEM",
                rawData("certificateId", report.getCertificateId(), "serialNumber", manifest.certificateSerialNumber()));

        applyCertificateMetadata(report, embeddedCertificate);
        if (isBlank(report.getSignerCertificateSerial())) {
            report.setSignerCertificateSerial(manifest.certificateSerialNumber());
        }
        if (isBlank(report.getSignerSubjectDn())) {
            report.setSignerSubjectDn(manifest.certificateSubjectDn());
        }

        DigestCheck digestCheck = verifyLegacyDigest(parsedPackage.originalContent(), manifest);
        report.setIntegrityValid(digestCheck.valid());
        addDetail(report, "document_digest_matches", digestCheck.valid(), digestCheck.message(), digestCheck.rawData());

        SignatureCheck signatureCheck = verifyLegacySignature(parsedPackage.originalContent(), manifest, embeddedCertificate);
        report.setSignatureValid(signatureCheck.valid());
        addDetail(report, "signature_value_valid", signatureCheck.valid(), signatureCheck.message(), signatureCheck.rawData());

        return new VerificationMaterial(
                embeddedCertificate,
                embeddedCertificate == null ? List.of() : List.of(embeddedCertificate),
                null
        );
    }

    private ParsedSignedPackage parseLegacySignedPackage(byte[] packageBytes, String signedFileName) {
        byte[] manifestJson = null;
        byte[] originalContent = null;
        String originalEntryName = null;
        String parseError = null;

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(packageBytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                byte[] entryBytes = zipInputStream.readAllBytes();
                if ("signature.json".equals(entry.getName())) {
                    manifestJson = entryBytes;
                } else if (entry.getName().startsWith("original/") && originalContent == null) {
                    originalContent = entryBytes;
                    originalEntryName = extractLeafName(entry.getName());
                }
            }
        } catch (Exception ex) {
            parseError = "Failed to read signed ZIP package: " + ex.getMessage();
        }

        LegacySignedPackageManifest manifest = null;
        if (parseError == null && manifestJson != null) {
            try {
                manifest = objectMapper.readValue(manifestJson, LegacySignedPackageManifest.class);
            } catch (Exception ex) {
                parseError = "Failed to parse signature.json: " + ex.getMessage();
            }
        }

        return new ParsedSignedPackage(
                signedFileName,
                originalEntryName,
                originalContent,
                manifestJson,
                manifest,
                parseError
        );
    }

    private VerificationReport initializeReport(UUID verifiedBy,
                                                String trustSource,
                                                String objectKey,
                                                String fileName,
                                                String contentType,
                                                byte[] signedBytes,
                                                InternalSigningArtifactResponse artifact,
                                                String loadMessage) {
        VerificationReport report = new VerificationReport();
        report.setId(UUID.randomUUID());
        report.setVerifiedBy(verifiedBy);
        report.setTrustSource(normalizeTrustSource(trustSource));
        report.setRevocationSource(DEFAULT_REVOCATION_SOURCE);
        report.setSignedObjectKey(objectKey);
        report.setSignedFileName(firstNonBlank(fileName, artifact == null ? null : artifact.getOutputFileName(), extractLeafName(objectKey), "uploaded-file.bin"));
        report.setOriginalFileName(firstNonBlank(artifact == null ? null : artifact.getFileName(), null));
        report.setDocumentId(artifact == null ? null : artifact.getDocumentId());
        report.setSigningRequestId(artifact == null ? null : artifact.getSigningRequestId());
        report.setCertificateId(artifact == null ? null : artifact.getCertificateId());
        report.setDigestAlgorithm(artifact == null ? null : artifact.getDigestAlgorithm());
        report.setSignatureAlgorithm(artifact == null ? null : artifact.getSignatureAlgorithm());
        report.setVerifiedAt(LocalDateTime.now());
        report.setCreatedAt(LocalDateTime.now());

        addDetail(report, "package_loaded", true, loadMessage,
                rawData(
                        "objectKey", objectKey,
                        "packageSize", signedBytes.length,
                        "outputContentType", firstNonBlank(contentType, artifact == null ? null : artifact.getOutputContentType())
                ));

        return report;
    }

    private InternalSigningArtifactResponse tryResolveArtifactByObjectKey(String objectKey) {
        UUID requestId = extractSigningRequestIdFromObjectKey(objectKey);
        if (requestId == null) {
            return null;
        }

        try {
            return signingWorkflowClient.getSigningArtifact(requestId);
        } catch (Exception ex) {
            return null;
        }
    }

    private UUID extractSigningRequestIdFromObjectKey(String objectKey) {
        if (isBlank(objectKey)) {
            return null;
        }

        String[] parts = objectKey.split("/");
        for (int i = 0; i + 1 < parts.length; i++) {
            if ("signed-output".equals(parts[i])) {
                try {
                    return UUID.fromString(parts[i + 1]);
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private SignatureContainerType detectContainerType(byte[] signedBytes,
                                                       String fileName,
                                                       String contentType,
                                                       InternalSigningArtifactResponse artifact) {
        String outputContentType = firstNonBlank(contentType, artifact == null ? null : artifact.getOutputContentType());
        String outputFileName = firstNonBlank(fileName, artifact == null ? null : artifact.getOutputFileName());

        if (looksLikePdf(signedBytes) || equalsIgnoreCase(outputContentType, "application/pdf")
                || endsWithIgnoreCase(outputFileName, ".pdf")) {
            return SignatureContainerType.PADES_PDF;
        }

        if (equalsIgnoreCase(outputContentType, "application/pkcs7-mime")
                || endsWithIgnoreCase(outputFileName, ".p7m")
                || looksLikeCms(signedBytes)) {
            return SignatureContainerType.CMS_CADES;
        }

        if (looksLikeZip(signedBytes) || equalsIgnoreCase(outputContentType, "application/zip")
                || endsWithIgnoreCase(outputFileName, ".zip")) {
            return SignatureContainerType.LEGACY_ZIP;
        }

        return SignatureContainerType.UNKNOWN;
    }

    private boolean looksLikePdf(byte[] signedBytes) {
        return signedBytes.length >= 5
                && signedBytes[0] == '%'
                && signedBytes[1] == 'P'
                && signedBytes[2] == 'D'
                && signedBytes[3] == 'F'
                && signedBytes[4] == '-';
    }

    private boolean looksLikeZip(byte[] signedBytes) {
        return signedBytes.length >= 4
                && signedBytes[0] == 'P'
                && signedBytes[1] == 'K'
                && (signedBytes[2] == 3 || signedBytes[2] == 5 || signedBytes[2] == 7)
                && (signedBytes[3] == 4 || signedBytes[3] == 6 || signedBytes[3] == 8);
    }

    private boolean looksLikeCms(byte[] signedBytes) {
        try {
            new CMSSignedData(signedBytes);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private SignerInformation extractSingleSigner(CMSSignedData signedData) {
        SignerInformation signer = signedData.getSignerInfos().getSigners().stream().findFirst().orElse(null);
        if (signer == null) {
            throw new IllegalStateException("Signed container does not contain SignerInfo");
        }
        return signer;
    }

    private X509Certificate extractSignerCertificate(CMSSignedData signedData, SignerInformation signer) {
        X509CertificateHolder certificateHolder = null;
        for (Object match : signedData.getCertificates().getMatches(signer.getSID())) {
            if (match instanceof X509CertificateHolder holder) {
                certificateHolder = holder;
                break;
            }
        }

        if (certificateHolder == null) {
            return null;
        }

        try {
            return new JcaX509CertificateConverter()
                    .setProvider(BC_PROVIDER)
                    .getCertificate(certificateHolder);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<X509Certificate> extractEmbeddedCertificates(CMSSignedData signedData) {
        List<X509Certificate> certificates = new java.util.ArrayList<>();
        for (Object match : signedData.getCertificates().getMatches(null)) {
            if (match instanceof X509CertificateHolder holder) {
                try {
                    certificates.add(new JcaX509CertificateConverter()
                            .setProvider(BC_PROVIDER)
                            .getCertificate(holder));
                } catch (Exception ignored) {
                }
            }
        }
        return certificates;
    }

    private AttributePresenceCheck checkCadesAttributes(SignerInformation signer) {
        AttributeTable signedAttributes = signer.getSignedAttributes();
        if (signedAttributes == null) {
            return new AttributePresenceCheck(false, "SignerInfo does not contain signed attributes", null);
        }

        boolean hasMessageDigest = signedAttributes.get(CMSAttributes.messageDigest) != null;
        boolean hasSigningTime = signedAttributes.get(CMSAttributes.signingTime) != null;
        boolean hasSigningCertificateV2 = signedAttributes.get(PKCSObjectIdentifiers.id_aa_signingCertificateV2) != null;
        boolean valid = hasMessageDigest && hasSigningCertificateV2;

        return new AttributePresenceCheck(
                valid,
                valid
                        ? "CAdES signed attributes are present"
                        : "CAdES signed attributes are incomplete",
                rawData(
                        "messageDigest", hasMessageDigest,
                        "signingTime", hasSigningTime,
                        "signingCertificateV2", hasSigningCertificateV2
                )
        );
    }

    private TimestampValidationResult validateTrustedTimestamp(VerificationReport report, SignerInformation signer) {
        AttributeTable unsignedAttributes = signer.getUnsignedAttributes();
        Attribute timestampAttribute = unsignedAttributes == null
                ? null
                : unsignedAttributes.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);

        if (timestampAttribute == null || timestampAttribute.getAttrValues() == null || timestampAttribute.getAttrValues().size() == 0) {
            addDetail(report, "trusted_timestamp_present", false, "Signature does not contain RFC 3161 timestamp token", null);
            addDetail(report, "trusted_timestamp_validated", false, "Trusted signing time is not available", null);
            return TimestampValidationResult.absent();
        }

        addDetail(report, "trusted_timestamp_present", true, "Signature contains RFC 3161 timestamp token",
                rawData("attributeOid", PKCSObjectIdentifiers.id_aa_signatureTimeStampToken.getId()));

        try {
            ContentInfo contentInfo = ContentInfo.getInstance(timestampAttribute.getAttrValues().getObjectAt(0));
            TimeStampToken timestampToken = new TimeStampToken(new CMSSignedData(contentInfo));
            TimeStampTokenInfo tokenInfo = timestampToken.getTimeStampInfo();
            OffsetDateTime generatedAt = OffsetDateTime.ofInstant(tokenInfo.getGenTime().toInstant(), ZoneOffset.UTC);

            X509Certificate tsaCertificate = extractTimestampAuthorityCertificate(timestampToken);

            DigestCheck imprintCheck = verifyTimestampMessageImprint(tokenInfo, signer);
            addDetail(report, "timestamp_message_imprint_matches_signature",
                    imprintCheck.valid(),
                    imprintCheck.message(),
                    imprintCheck.rawData());

            SignatureCheck tokenSignatureCheck = verifyTimestampTokenSignature(timestampToken, tsaCertificate);
            addDetail(report, "timestamp_token_signature_valid",
                    tokenSignatureCheck.valid(),
                    tokenSignatureCheck.message(),
                    tokenSignatureCheck.rawData());

            InternalTimestampAuthorityMetadataResponse authorityMetadata = null;
            String authorityLookupMessage;
            try {
                String authoritySerial = tsaCertificate != null
                        ? toCanonicalSerialHex(tsaCertificate.getSerialNumber())
                        : toCanonicalSerialHex(timestampToken.getSID().getSerialNumber());
                authorityMetadata = certificationServiceClient.getTimestampAuthorityMetadataBySerial(authoritySerial);
                authorityLookupMessage = "Timestamp authority recognized by certification-service";
            } catch (Exception ex) {
                authorityLookupMessage = ex.getMessage();
            }

            addDetail(report, "timestamp_authority_recognized",
                    authorityMetadata != null,
                    authorityLookupMessage,
                    rawData(
                            "serialNumber", tsaCertificate == null ? null : toCanonicalSerialHex(tsaCertificate.getSerialNumber()),
                            "generatedAt", generatedAt
                    ));

            TrustCheck authorityTrustCheck = verifyTimestampAuthorityTrust(authorityMetadata, tsaCertificate);
            addDetail(report, "timestamp_authority_matches_registry",
                    authorityTrustCheck.valid(),
                    authorityTrustCheck.message(),
                    authorityTrustCheck.rawData());

            ValidityCheck authorityValidityCheck = verifyTimestampAuthorityValidity(authorityMetadata, tsaCertificate, generatedAt);
            addDetail(report, "timestamp_authority_time_valid",
                    authorityValidityCheck.valid(),
                    authorityValidityCheck.message(),
                    authorityValidityCheck.rawData());

            RevocationCheck authorityRevocationCheck = verifyTimestampAuthorityRevocation(authorityMetadata, generatedAt);
            addDetail(report, "timestamp_authority_not_revoked",
                    authorityRevocationCheck.valid(),
                    authorityRevocationCheck.message(),
                    authorityRevocationCheck.rawData());

            boolean trusted = imprintCheck.valid()
                    && tokenSignatureCheck.valid()
                    && authorityTrustCheck.valid()
                    && authorityValidityCheck.valid()
                    && authorityRevocationCheck.valid();

            addDetail(report, "trusted_timestamp_validated",
                    trusted,
                    trusted
                            ? "Trusted signing time was validated successfully"
                            : "Timestamp token is present but could not be fully trusted",
                    rawData(
                            "generatedAt", generatedAt,
                            "tsaSubjectDn", tsaCertificate == null ? null : tsaCertificate.getSubjectX500Principal().getName()
                    ));

            return trusted
                    ? TimestampValidationResult.trusted(generatedAt)
                    : TimestampValidationResult.untrusted(generatedAt);
        } catch (Exception ex) {
            addDetail(report, "trusted_timestamp_validated",
                    false,
                    "Failed to parse or validate timestamp token: " + ex.getMessage(),
                    rawData("error", ex.getMessage()));
            return TimestampValidationResult.untrusted(null);
        }
    }

    private DigestCheck verifyTimestampMessageImprint(TimeStampTokenInfo tokenInfo, SignerInformation signer) {
        try {
            String digestAlgorithm = firstNonBlank(
                    normalizeAlgorithmName(tokenInfo.getMessageImprintAlgOID().getId()),
                    "SHA-256"
            );
            byte[] expectedDigest = tokenInfo.getMessageImprintDigest();
            byte[] computedDigest = MessageDigest.getInstance(digestAlgorithm).digest(signer.getSignature());
            boolean valid = Arrays.equals(expectedDigest, computedDigest);
            return new DigestCheck(
                    valid,
                    valid
                            ? "Timestamp imprint matches signer signature bytes"
                            : "Timestamp imprint does not match signer signature bytes",
                    rawData(
                            "digestAlgorithm", digestAlgorithm,
                            "expectedDigest", Base64.getEncoder().encodeToString(expectedDigest),
                            "computedDigest", Base64.getEncoder().encodeToString(computedDigest)
                    )
            );
        } catch (Exception ex) {
            return new DigestCheck(false,
                    "Failed to verify timestamp message imprint: " + ex.getMessage(),
                    rawData("error", ex.getMessage()));
        }
    }

    private SignatureCheck verifyTimestampTokenSignature(TimeStampToken timestampToken, X509Certificate tsaCertificate) {
        if (tsaCertificate == null) {
            return new SignatureCheck(false, "Timestamp token does not contain TSA certificate", null);
        }

        try {
            timestampToken.validate(new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider(BC_PROVIDER)
                    .build(tsaCertificate));
            return new SignatureCheck(
                    true,
                    "Timestamp token signature is valid",
                    rawData("certificateSubjectDn", tsaCertificate.getSubjectX500Principal().getName())
            );
        } catch (Exception ex) {
            return new SignatureCheck(false,
                    "Timestamp token signature is invalid: " + ex.getMessage(),
                    rawData("certificateSubjectDn", tsaCertificate.getSubjectX500Principal().getName()));
        }
    }

    private DigestCheck verifyCmsMessageDigest(byte[] contentToDigest,
                                               SignerInformation signer,
                                               String digestAlgorithm) {
        if (contentToDigest == null || contentToDigest.length == 0) {
            return new DigestCheck(false, "Signed content is missing", rawData("digestAlgorithm", digestAlgorithm));
        }

        AttributeTable signedAttributes = signer.getSignedAttributes();
        if (signedAttributes == null) {
            return new DigestCheck(false, "SignerInfo does not contain signed attributes", null);
        }

        Attribute messageDigestAttribute = signedAttributes.get(CMSAttributes.messageDigest);
        if (messageDigestAttribute == null || messageDigestAttribute.getAttrValues().size() == 0) {
            return new DigestCheck(false, "SignerInfo does not contain messageDigest attribute", null);
        }

        try {
            byte[] expectedDigest = ASN1OctetString.getInstance(messageDigestAttribute.getAttrValues().getObjectAt(0)).getOctets();
            byte[] computedDigest = MessageDigest.getInstance(firstNonBlank(digestAlgorithm, "SHA-256")).digest(contentToDigest);
            boolean valid = Arrays.equals(expectedDigest, computedDigest);
            return new DigestCheck(
                    valid,
                    valid
                            ? "Signed attributes digest matches signed content"
                            : "Signed attributes digest does not match signed content",
                    rawData(
                            "digestAlgorithm", firstNonBlank(digestAlgorithm, "SHA-256"),
                            "expectedDigest", Base64.getEncoder().encodeToString(expectedDigest),
                            "computedDigest", Base64.getEncoder().encodeToString(computedDigest)
                    )
            );
        } catch (Exception ex) {
            return new DigestCheck(false, "Failed to verify signed attributes digest: " + ex.getMessage(),
                    rawData("digestAlgorithm", digestAlgorithm));
        }
    }

    private SignatureCheck verifyCmsSignature(SignerInformation signer, X509Certificate certificate) {
        if (certificate == null) {
            return new SignatureCheck(false, "Embedded certificate is missing or invalid", null);
        }

        try {
            boolean valid = signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider(BC_PROVIDER)
                    .build(certificate));

            return new SignatureCheck(
                    valid,
                    valid
                            ? "CMS signature is valid for signed content"
                            : "CMS signature is not valid for signed content",
                    rawData("certificateSubjectDn", certificate.getSubjectX500Principal().getName())
            );
        } catch (Exception ex) {
            return new SignatureCheck(false, "Failed to verify CMS signature: " + ex.getMessage(),
                    rawData("certificateSubjectDn", certificate.getSubjectX500Principal().getName()));
        }
    }

    private DigestCheck verifyLegacyDigest(byte[] originalContent, LegacySignedPackageManifest manifest) {
        if (originalContent == null || originalContent.length == 0) {
            return new DigestCheck(false, "Original payload is missing", rawData("digestAlgorithm", manifest.digestAlgorithm()));
        }
        if (isBlank(manifest.digestAlgorithm())) {
            return new DigestCheck(false, "signature.json does not contain digestAlgorithm", rawData("expectedDigest", manifest.documentDigestBase64()));
        }
        if (isBlank(manifest.documentDigestBase64())) {
            return new DigestCheck(false, "signature.json does not contain documentDigestBase64", rawData("digestAlgorithm", manifest.digestAlgorithm()));
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance(manifest.digestAlgorithm());
            String computedDigest = Base64.getEncoder().encodeToString(messageDigest.digest(originalContent));
            boolean valid = computedDigest.equals(manifest.documentDigestBase64());
            return new DigestCheck(
                    valid,
                    valid ? "Document digest matches original payload" : "Document digest does not match original payload",
                    rawData("digestAlgorithm", manifest.digestAlgorithm(),
                            "expectedDigest", manifest.documentDigestBase64(),
                            "computedDigest", computedDigest)
            );
        } catch (Exception ex) {
            return new DigestCheck(false, "Failed to compute document digest: " + ex.getMessage(),
                    rawData("digestAlgorithm", manifest.digestAlgorithm()));
        }
    }

    private SignatureCheck verifyLegacySignature(byte[] originalContent,
                                                 LegacySignedPackageManifest manifest,
                                                 X509Certificate certificate) {
        if (originalContent == null || originalContent.length == 0) {
            return new SignatureCheck(false, "Original payload is missing",
                    rawData("signatureAlgorithm", manifest.signatureAlgorithm()));
        }
        if (certificate == null) {
            return new SignatureCheck(false, "Embedded certificate is missing or invalid",
                    rawData("certificateId", manifest.certificateId()));
        }
        if (isBlank(manifest.signatureAlgorithm())) {
            return new SignatureCheck(false, "signature.json does not contain signatureAlgorithm",
                    rawData("certificateId", manifest.certificateId()));
        }
        if (isBlank(manifest.signatureValueBase64())) {
            return new SignatureCheck(false, "signature.json does not contain signatureValueBase64",
                    rawData("signatureAlgorithm", manifest.signatureAlgorithm()));
        }

        try {
            java.security.Signature signature = java.security.Signature.getInstance(manifest.signatureAlgorithm());
            signature.initVerify(certificate.getPublicKey());
            signature.update(originalContent);
            boolean valid = signature.verify(Base64.getDecoder().decode(manifest.signatureValueBase64()));

            return new SignatureCheck(
                    valid,
                    valid ? "Signature value is valid for original payload" : "Signature value is not valid for original payload",
                    rawData("signatureAlgorithm", manifest.signatureAlgorithm(),
                            "certificateSubjectDn", certificate.getSubjectX500Principal().getName())
            );
        } catch (Exception ex) {
            return new SignatureCheck(false, "Failed to verify signature: " + ex.getMessage(),
                    rawData("signatureAlgorithm", manifest.signatureAlgorithm()));
        }
    }

    private TrustCheck verifyTrust(InternalCertificateMetadataResponse authorityMetadata,
                                   X509Certificate embeddedCertificate,
                                   VerificationReport report) {
        if (authorityMetadata == null) {
            return new TrustCheck(false, "Certificate authority could not confirm this certificate",
                    rawData("certificateId", report.getCertificateId()));
        }

        String embeddedPem = normalizePem(toPem(embeddedCertificate));
        String authorityPem = normalizePem(authorityMetadata.getCertificatePem());
        boolean pemMatches = embeddedPem.equals(authorityPem);
        boolean serialMatches = normalizeSerial(report.getSignerCertificateSerial())
                .equals(normalizeSerial(authorityMetadata.getSerialNumber()));
        boolean subjectMatches = normalizeText(report.getSignerSubjectDn())
                .equals(normalizeText(authorityMetadata.getSubjectDn()));

        boolean valid = pemMatches && serialMatches && subjectMatches;
        return new TrustCheck(
                valid,
                valid
                        ? "Embedded certificate matches certification-service metadata"
                        : "Embedded certificate does not match certification-service metadata",
                rawData(
                        "pemMatches", pemMatches,
                        "serialMatches", serialMatches,
                        "subjectMatches", subjectMatches
                )
        );
    }

    private TrustCheck verifyTimestampAuthorityTrust(InternalTimestampAuthorityMetadataResponse authorityMetadata,
                                                     X509Certificate embeddedCertificate) {
        if (authorityMetadata == null) {
            return new TrustCheck(false, "Certification service could not confirm this timestamp authority", null);
        }
        if (embeddedCertificate == null) {
            return new TrustCheck(false, "Timestamp token does not contain TSA certificate", null);
        }

        String embeddedPem = normalizePem(toPem(embeddedCertificate));
        String authorityPem = normalizePem(authorityMetadata.getCertificatePem());
        boolean pemMatches = embeddedPem.equals(authorityPem);
        boolean serialMatches = normalizeSerial(toCanonicalSerialHex(embeddedCertificate.getSerialNumber()))
                .equals(normalizeSerial(authorityMetadata.getSerialNumber()));
        boolean subjectMatches = normalizeText(embeddedCertificate.getSubjectX500Principal().getName())
                .equals(normalizeText(authorityMetadata.getSubjectDn()));

        boolean valid = pemMatches && serialMatches && subjectMatches;
        return new TrustCheck(
                valid,
                valid
                        ? "Timestamp authority certificate matches certification-service metadata"
                        : "Timestamp authority certificate does not match certification-service metadata",
                rawData(
                        "pemMatches", pemMatches,
                        "serialMatches", serialMatches,
                        "subjectMatches", subjectMatches
                )
        );
    }

    private ValidityCheck verifyCertificateValidity(InternalCertificateMetadataResponse authorityMetadata,
                                                    X509Certificate embeddedCertificate,
                                                    OffsetDateTime referenceTime) {
        OffsetDateTime evaluatedAt = referenceTime == null ? OffsetDateTime.now(ZoneOffset.UTC) : referenceTime;

        if (authorityMetadata != null) {
            boolean notBeforeValid = authorityMetadata.getValidFrom() == null || !evaluatedAt.isBefore(authorityMetadata.getValidFrom());
            boolean notAfterValid = authorityMetadata.getValidTo() == null || !evaluatedAt.isAfter(authorityMetadata.getValidTo());
            boolean valid = notBeforeValid && notAfterValid;

            return new ValidityCheck(
                    valid,
                    valid
                            ? referenceTime == null
                            ? "Certificate is within its current validity window"
                            : "Certificate was within its validity window at trusted signing time"
                            : referenceTime == null
                            ? "Certificate is outside its current validity window"
                            : "Certificate was outside its validity window at trusted signing time",
                    rawData("status", authorityMetadata.getStatus(),
                            "validFrom", authorityMetadata.getValidFrom(),
                            "validTo", authorityMetadata.getValidTo(),
                            "evaluatedAt", evaluatedAt,
                            "trustedSigningTimeApplied", referenceTime != null)
            );
        }

        if (embeddedCertificate == null) {
            return new ValidityCheck(false, "No certificate available to evaluate validity window", null);
        }

        try {
            embeddedCertificate.checkValidity(java.util.Date.from(evaluatedAt.toInstant()));
            return new ValidityCheck(
                    true,
                    referenceTime == null
                            ? "Embedded certificate is within its validity window"
                            : "Embedded certificate was within its validity window at trusted signing time",
                    rawData(
                            "notBefore", embeddedCertificate.getNotBefore(),
                            "notAfter", embeddedCertificate.getNotAfter(),
                            "evaluatedAt", evaluatedAt,
                            "trustedSigningTimeApplied", referenceTime != null
                    ));
        } catch (Exception ex) {
            return new ValidityCheck(
                    false,
                    referenceTime == null
                            ? "Embedded certificate validity check failed: " + ex.getMessage()
                            : "Embedded certificate was not valid at trusted signing time: " + ex.getMessage(),
                    rawData(
                            "notBefore", embeddedCertificate.getNotBefore(),
                            "notAfter", embeddedCertificate.getNotAfter(),
                            "evaluatedAt", evaluatedAt,
                            "trustedSigningTimeApplied", referenceTime != null
                    ));
        }
    }

    private ValidityCheck verifyTimestampAuthorityValidity(InternalTimestampAuthorityMetadataResponse authorityMetadata,
                                                           X509Certificate embeddedCertificate,
                                                           OffsetDateTime generatedAt) {
        OffsetDateTime evaluatedAt = generatedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : generatedAt;

        if (authorityMetadata != null) {
            boolean activeStatus = isActiveStatus(authorityMetadata.getStatus());
            boolean notBeforeValid = authorityMetadata.getValidFrom() == null || !evaluatedAt.isBefore(authorityMetadata.getValidFrom());
            boolean notAfterValid = authorityMetadata.getValidTo() == null || !evaluatedAt.isAfter(authorityMetadata.getValidTo());
            boolean valid = activeStatus && notBeforeValid && notAfterValid;
            return new ValidityCheck(
                    valid,
                    valid
                            ? "Timestamp authority was valid at token generation time"
                            : "Timestamp authority was not valid at token generation time",
                    rawData(
                            "status", authorityMetadata.getStatus(),
                            "validFrom", authorityMetadata.getValidFrom(),
                            "validTo", authorityMetadata.getValidTo(),
                            "generatedAt", generatedAt
                    )
            );
        }

        if (embeddedCertificate == null) {
            return new ValidityCheck(false, "Timestamp token does not contain TSA certificate", null);
        }

        try {
            embeddedCertificate.checkValidity(java.util.Date.from(evaluatedAt.toInstant()));
            return new ValidityCheck(true, "Timestamp authority certificate was valid at token generation time",
                    rawData("generatedAt", generatedAt));
        } catch (Exception ex) {
            return new ValidityCheck(false,
                    "Timestamp authority certificate was not valid at token generation time: " + ex.getMessage(),
                    rawData("generatedAt", generatedAt));
        }
    }

    private RevocationCheck verifyRevocation(InternalCertificateMetadataResponse authorityMetadata,
                                             OffsetDateTime referenceTime) {
        if (authorityMetadata == null) {
            return new RevocationCheck(false, "Certificate revocation status could not be confirmed", null);
        }

        if (!authorityMetadata.isRevoked()) {
            return new RevocationCheck(true, "Certificate is not revoked",
                    rawData("revoked", false, "trustedSigningTimeApplied", referenceTime != null));
        }

        if (referenceTime != null
                && authorityMetadata.getRevokedAt() != null
                && authorityMetadata.getRevokedAt().isAfter(referenceTime)) {
            return new RevocationCheck(
                    true,
                    "Certificate was revoked after the trusted signing time",
                    rawData(
                            "revoked", true,
                            "revokedAt", authorityMetadata.getRevokedAt(),
                            "evaluatedAt", referenceTime,
                            "trustedSigningTimeApplied", true
                    )
            );
        }

        return new RevocationCheck(
                false,
                "Certificate has been revoked",
                rawData(
                        "revoked", authorityMetadata.isRevoked(),
                        "revokedAt", authorityMetadata.getRevokedAt(),
                        "evaluatedAt", referenceTime,
                        "trustedSigningTimeApplied", referenceTime != null
                )
        );
    }

    private RevocationCheck verifyTimestampAuthorityRevocation(InternalTimestampAuthorityMetadataResponse authorityMetadata,
                                                               OffsetDateTime generatedAt) {
        if (authorityMetadata == null) {
            return new RevocationCheck(false, "Timestamp authority revocation status could not be confirmed", null);
        }

        boolean valid = authorityMetadata.getRevokedAt() == null || authorityMetadata.getRevokedAt().isAfter(generatedAt);
        return new RevocationCheck(
                valid,
                valid
                        ? "Timestamp authority was not revoked at token generation time"
                        : "Timestamp authority had already been revoked at token generation time",
                rawData(
                        "revokedAt", authorityMetadata.getRevokedAt(),
                        "generatedAt", generatedAt
                )
        );
    }

    private X509Certificate extractTimestampAuthorityCertificate(TimeStampToken timestampToken) {
        try {
            Object match = timestampToken.toCMSSignedData().getCertificates().getMatches(timestampToken.getSID())
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (match instanceof X509CertificateHolder holder) {
                return new JcaX509CertificateConverter()
                        .setProvider(BC_PROVIDER)
                        .getCertificate(holder);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private X509Certificate parseCertificate(String certificatePem) {
        if (isBlank(certificatePem)) {
            return null;
        }

        try {
            String normalized = certificatePem
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "");

            byte[] der = Base64.getDecoder().decode(normalized);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der));
        } catch (Exception ex) {
            return null;
        }
    }

    private void applyCertificateMetadata(VerificationReport report, X509Certificate certificate) {
        if (certificate == null) {
            return;
        }

        report.setSignerCertificateSerial(toCanonicalSerialHex(certificate.getSerialNumber()));
        report.setSignerSubjectDn(certificate.getSubjectX500Principal().getName());
        report.setIssuerDn(certificate.getIssuerX500Principal().getName());
    }

    private String toPem(X509Certificate certificate) {
        if (certificate == null) {
            return null;
        }
        try {
            String encoded = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certificate.getEncoded());
            return "-----BEGIN CERTIFICATE-----\n" + encoded + "\n-----END CERTIFICATE-----";
        } catch (Exception ex) {
            return null;
        }
    }

    private void markMalformed(VerificationReport report, String message) {
        report.setVerificationStatus(VerificationStatus.FAILED);
        report.setIntegrityValid(false);
        report.setSignatureValid(false);
        report.setTrustChainValid(false);
        report.setExpirationValid(false);
        report.setRevocationValid(false);
        report.setOverallMessage(message);
    }

    private VerificationStatus resolveStatus(VerificationReport report) {
        if (report.isIntegrityValid()
                && report.isSignatureValid()
                && report.isTrustChainValid()
                && report.isExpirationValid()
                && report.isRevocationValid()) {
            return VerificationStatus.PASSED;
        }

        if (report.isIntegrityValid() && report.isSignatureValid()) {
            return VerificationStatus.PARTIAL;
        }

        return VerificationStatus.FAILED;
    }

    private String resolveOverallMessage(VerificationReport report, SignatureContainerType containerType) {
        boolean trustedTimestamp = hasSuccessfulCheck(report, "trusted_timestamp_validated");
        return switch (report.getVerificationStatus()) {
            case PASSED -> switch (containerType) {
                case PADES_PDF -> trustedTimestamp
                        ? "Embedded PDF signature and trusted timestamp are valid; the signer certificate was trusted and not revoked at signing time"
                        : "Embedded PDF signature is valid and the signer certificate is trusted, active, and not revoked";
                case CMS_CADES -> trustedTimestamp
                        ? "CMS/CAdES signature and trusted timestamp are valid; the signer certificate was trusted and not revoked at signing time"
                        : "CMS/CAdES signature is valid and the signer certificate is trusted, active, and not revoked";
                case LEGACY_ZIP -> "Legacy signed package is valid and the signer certificate is trusted, active, and not revoked";
                case UNKNOWN -> "Signature is valid";
            };
            case PARTIAL -> trustedTimestamp
                    ? "Signature is cryptographically valid and carries a trusted signing time, but some trust checks are incomplete or failed"
                    : "Signature is cryptographically valid, but certificate trust checks are incomplete or failed";
            case FAILED -> "Signed output verification failed";
        };
    }

    private boolean hasSuccessfulCheck(VerificationReport report, String checkName) {
        return report.getDetails().stream()
                .anyMatch(detail -> checkName.equals(detail.getCheckName()) && detail.isCheckResult());
    }

    private VerificationReportResponse toResponse(VerificationReport report) {
        List<VerificationCheckResponse> checks = report.getDetails().stream()
                .map(detail -> VerificationCheckResponse.builder()
                        .checkName(detail.getCheckName())
                        .result(detail.isCheckResult())
                        .detailMessage(detail.getDetailMessage())
                        .rawData(detail.getRawData())
                        .build())
                .toList();

        return VerificationReportResponse.builder()
                .id(report.getId())
                .documentId(report.getDocumentId())
                .documentVersion(report.getDocumentVersion())
                .signingRequestId(report.getSigningRequestId())
                .certificateId(report.getCertificateId())
                .verifiedBy(report.getVerifiedBy())
                .verificationStatus(report.getVerificationStatus() == null ? null : report.getVerificationStatus().name())
                .signedObjectKey(report.getSignedObjectKey())
                .signedFileName(report.getSignedFileName())
                .originalFileName(report.getOriginalFileName())
                .signerCertificateSerial(report.getSignerCertificateSerial())
                .signerSubjectDn(report.getSignerSubjectDn())
                .issuerDn(report.getIssuerDn())
                .trustSource(report.getTrustSource())
                .revocationSource(report.getRevocationSource())
                .digestAlgorithm(report.getDigestAlgorithm())
                .signatureAlgorithm(report.getSignatureAlgorithm())
                .integrityValid(report.isIntegrityValid())
                .signatureValid(report.isSignatureValid())
                .trustChainValid(report.isTrustChainValid())
                .expirationValid(report.isExpirationValid())
                .revocationValid(report.isRevocationValid())
                .overallMessage(report.getOverallMessage())
                .verifiedAt(report.getVerifiedAt())
                .checks(checks)
                .build();
    }

    private void addDetail(VerificationReport report,
                           String checkName,
                           boolean result,
                           String detailMessage,
                           JsonNode rawData) {
        VerificationReportDetail detail = new VerificationReportDetail();
        detail.setId(UUID.randomUUID());
        detail.setCheckName(checkName);
        detail.setCheckResult(result);
        detail.setDetailMessage(detailMessage);
        detail.setRawData(rawData);
        detail.setCreatedAt(LocalDateTime.now());
        report.addDetail(detail);
    }

    private JsonNode rawData(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key != null && value != null) {
                map.put(String.valueOf(key), value);
            }
        }

        return map.isEmpty() ? null : objectMapper.valueToTree(map);
    }

    private JsonNode rawData(Map<String, Object> values) {
        return values == null || values.isEmpty() ? null : objectMapper.valueToTree(values);
    }

    private String normalizeTrustSource(String trustSource) {
        return isBlank(trustSource) ? DEFAULT_TRUST_SOURCE : trustSource.trim().toUpperCase();
    }

    private boolean isActiveStatus(String status) {
        if (isBlank(status)) {
            return false;
        }

        String normalized = normalizeText(status);
        return "ACTIVE".equals(normalized) || "CON HIEU LUC".equals(normalized);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase();
    }

    private String normalizePem(String pem) {
        return pem == null ? "" : pem.replaceAll("\\s+", "");
    }

    private String normalizeSerial(String serial) {
        if (serial == null) {
            return "";
        }
        String normalized = serial.replaceAll("[^A-Fa-f0-9]", "").toUpperCase();
        normalized = normalized.replaceFirst("^0+", "");
        return normalized.isBlank() ? "0" : normalized;
    }

    private String toCanonicalSerialHex(BigInteger serialNumber) {
        if (serialNumber == null) {
            return "";
        }

        byte[] bytes = serialNumber.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0x00) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }

        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02X", value));
        }
        return builder.toString();
    }

    private String normalizeAlgorithmName(String oid) {
        if (isBlank(oid)) {
            return null;
        }
        try {
            String name = ALGORITHM_NAME_FINDER.getAlgorithmName(new ASN1ObjectIdentifier(oid));
            return isBlank(name) ? oid : name;
        } catch (Exception ex) {
            return oid;
        }
    }

    private String deriveSignatureAlgorithm(SignerInformation signer, String digestAlgorithm) {
        String encryptionName = normalizeAlgorithmName(signer.getEncryptionAlgOID());
        if (isBlank(encryptionName)) {
            return digestAlgorithm;
        }

        if ("RSA".equalsIgnoreCase(encryptionName) && !isBlank(digestAlgorithm)) {
            return digestAlgorithm.replace("-", "") + "withRSA";
        }
        if ("ECDSA".equalsIgnoreCase(encryptionName) && !isBlank(digestAlgorithm)) {
            return digestAlgorithm.replace("-", "") + "withECDSA";
        }
        return encryptionName;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private InternalCertificateMetadataResponse resolveAuthorityMetadata(VerificationReport report) {
        if (report.getCertificateId() != null) {
            return certificationServiceClient.getCertificateMetadata(report.getCertificateId());
        }
        if (!isBlank(report.getSignerCertificateSerial())) {
            return certificationServiceClient.getCertificateMetadataBySerial(report.getSignerCertificateSerial());
        }
        return null;
    }

    private <T> T firstNonNull(T first, T fallback) {
        return first != null ? first : fallback;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String extractLeafName(String path) {
        if (isBlank(path)) {
            return null;
        }
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private boolean endsWithIgnoreCase(String value, String suffix) {
        return value != null && suffix != null && value.toLowerCase().endsWith(suffix.toLowerCase());
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String stripP7mSuffix(String fileName) {
        if (isBlank(fileName)) {
            return null;
        }
        return endsWithIgnoreCase(fileName, ".p7m")
                ? fileName.substring(0, fileName.length() - 4)
                : fileName;
    }

    private void publishVerificationAudit(VerificationReport report) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verificationStatus", report.getVerificationStatus() == null ? null : report.getVerificationStatus().name());
        payload.put("signedObjectKey", report.getSignedObjectKey());
        payload.put("certificateId", report.getCertificateId());
        payload.put("signingRequestId", report.getSigningRequestId());
        payload.put("integrityValid", report.isIntegrityValid());
        payload.put("signatureValid", report.isSignatureValid());
        payload.put("trustChainValid", report.isTrustChainValid());
        payload.put("expirationValid", report.isExpirationValid());
        payload.put("revocationValid", report.isRevocationValid());
        payload.put("trustSource", report.getTrustSource());

        auditServiceClient.publishEvent(
                "DOCUMENT_VERIFIED",
                report.getVerifiedBy(),
                null,
                report.getId(),
                "VERIFICATION_REPORT",
                report.getId(),
                report.getSigningRequestId() == null ? report.getId().toString() : report.getSigningRequestId().toString(),
                report.getOverallMessage(),
                payload
        );
    }

    private enum SignatureContainerType {
        PADES_PDF,
        CMS_CADES,
        LEGACY_ZIP,
        UNKNOWN
    }

    private record VerificationMaterial(X509Certificate embeddedCertificate,
                                        List<X509Certificate> embeddedCertificates,
                                        SignerInformation signer) {
    }

    private record ParsedSignedPackage(
            String signedFileName,
            String originalEntryName,
            byte[] originalContent,
            byte[] manifestJson,
            LegacySignedPackageManifest manifest,
            String parseError
    ) {
    }

    private record LegacySignedPackageManifest(
            UUID signingRequestId,
            UUID documentId,
            UUID requesterAccountId,
            UUID signerAccountId,
            UUID certificateId,
            String originalFileName,
            String originalContentType,
            String digestAlgorithm,
            String signatureAlgorithm,
            String documentDigestBase64,
            String signatureValueBase64,
            String certificateSerialNumber,
            String certificateSubjectDn,
            String certificatePem,
            LocalDateTime packagedAt
    ) {
    }

    private record AttributePresenceCheck(boolean valid, String message, JsonNode rawData) {
    }

    private record DigestCheck(boolean valid, String message, JsonNode rawData) {
    }

    private record SignatureCheck(boolean valid, String message, JsonNode rawData) {
    }

    private record TrustCheck(boolean valid, String message, JsonNode rawData) {
    }

    private record ValidityCheck(boolean valid, String message, JsonNode rawData) {
    }

    private record RevocationCheck(boolean valid, String message, JsonNode rawData) {
    }

    private record TimestampValidationResult(boolean present,
                                             boolean trusted,
                                             OffsetDateTime referenceTime) {
        private static TimestampValidationResult absent() {
            return new TimestampValidationResult(false, false, null);
        }

        private static TimestampValidationResult trusted(OffsetDateTime referenceTime) {
            return new TimestampValidationResult(true, true, referenceTime);
        }

        private static TimestampValidationResult untrusted(OffsetDateTime referenceTime) {
            return new TimestampValidationResult(true, false, referenceTime);
        }
    }
}
