package com.ct556.verification_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExternalCaTrustValidationService {

    private static final Pattern PEM_CERT_PATTERN = Pattern.compile(
            "-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----",
            Pattern.DOTALL
    );

    private final X509TrustManager defaultTrustManager;

    public ExternalCaTrustValidationService() {
        this.defaultTrustManager = loadDefaultTrustManager();
        Security.setProperty("ocsp.enable", "true");
        System.setProperty("com.sun.security.enableCRLDP", "true");
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true");
    }

    public List<X509Certificate> parseUploadedCertificates(List<MultipartFile> trustAnchors) {
        if (trustAnchors == null || trustAnchors.isEmpty()) {
            return List.of();
        }

        List<X509Certificate> certificates = new ArrayList<>();
        for (MultipartFile file : trustAnchors) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            try {
                certificates.addAll(parseCertificates(file.getBytes()));
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot parse trust anchor file: " + file.getOriginalFilename(), ex);
            }
        }
        return deduplicate(certificates);
    }

    public ExternalTrustValidationResult validate(X509Certificate signerCertificate,
                                                  List<X509Certificate> embeddedCertificates,
                                                  List<X509Certificate> uploadedCertificates) {
        if (signerCertificate == null) {
            return new ExternalTrustValidationResult(
                    false,
                    "Embedded signer certificate is missing",
                    mapOf("embeddedCertificates", 0, "uploadedCertificates", uploadedCertificates == null ? 0 : uploadedCertificates.size()),
                    false,
                    "External trust chain could not be validated because signer certificate is missing",
                    null,
                    false,
                    "External revocation check skipped because signer certificate is missing",
                    null
            );
        }

        List<X509Certificate> normalizedEmbedded = deduplicate(embeddedCertificates == null ? List.of() : embeddedCertificates);
        List<X509Certificate> normalizedUploaded = deduplicate(uploadedCertificates == null ? List.of() : uploadedCertificates);

        Set<TrustAnchor> trustAnchors = buildTrustAnchors(normalizedUploaded);
        boolean trustAnchorAvailable = !trustAnchors.isEmpty();
        Map<String, Object> trustAnchorRaw = mapOf(
                "defaultJvmTrustAnchors", defaultTrustManager.getAcceptedIssuers().length,
                "uploadedCertificates", normalizedUploaded.size(),
                "effectiveTrustAnchors", trustAnchors.size()
        );

        if (!trustAnchorAvailable) {
            return new ExternalTrustValidationResult(
                    false,
                    "No external trust anchor is available for chain validation",
                    trustAnchorRaw,
                    false,
                    "External trust chain could not be validated",
                    null,
                    false,
                    "External revocation check skipped because trust chain could not be built",
                    null
            );
        }

        List<X509Certificate> candidateCertificates = deduplicate(mergeCertificates(signerCertificate, normalizedEmbedded, normalizedUploaded));
        PathValidationResult trustValidation = validateTrustChain(signerCertificate, candidateCertificates, trustAnchors);
        if (!trustValidation.valid()) {
            return new ExternalTrustValidationResult(
                    true,
                    "External trust anchors are available for validation",
                    trustAnchorRaw,
                    false,
                    trustValidation.message(),
                    trustValidation.rawData(),
                    false,
                    "External revocation check skipped because trust chain validation failed",
                    null
            );
        }

        PathValidationResult revocationValidation = validateRevocation(trustValidation.pathResult(), candidateCertificates, trustAnchors);
        return new ExternalTrustValidationResult(
                true,
                "External trust anchors are available for validation",
                trustAnchorRaw,
                true,
                trustValidation.message(),
                trustValidation.rawData(),
                revocationValidation.valid(),
                revocationValidation.message(),
                revocationValidation.rawData()
        );
    }

    private X509TrustManager loadDefaultTrustManager() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager x509TrustManager) {
                    return x509TrustManager;
                }
            }
            throw new IllegalStateException("Default X509TrustManager is not available");
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot initialize default trust store for external CA verification", ex);
        }
    }

    private Set<TrustAnchor> buildTrustAnchors(List<X509Certificate> uploadedCertificates) {
        Set<TrustAnchor> trustAnchors = new LinkedHashSet<>();
        for (X509Certificate certificate : defaultTrustManager.getAcceptedIssuers()) {
            trustAnchors.add(new TrustAnchor(certificate, null));
        }
        for (X509Certificate certificate : uploadedCertificates) {
            if (certificate.getBasicConstraints() >= 0 || isSelfSigned(certificate)) {
                trustAnchors.add(new TrustAnchor(certificate, null));
            }
        }
        return trustAnchors;
    }

    private PathValidationResult validateTrustChain(X509Certificate signerCertificate,
                                                    List<X509Certificate> candidateCertificates,
                                                    Set<TrustAnchor> trustAnchors) {
        try {
            PKIXCertPathBuilderResult result = buildPath(signerCertificate, candidateCertificates, trustAnchors);
            String anchorSubject = resolveTrustAnchorSubject(result.getTrustAnchor());
            return new PathValidationResult(
                    true,
                    "External certificate chain validates to a trusted CA",
                    mapOf(
                            "certPathLength", result.getCertPath().getCertificates().size(),
                            "trustAnchorSubjectDn", anchorSubject
                    ),
                    result
            );
        } catch (Exception ex) {
            return new PathValidationResult(
                    false,
                    "External certificate chain validation failed: " + ex.getMessage(),
                    mapOf("error", ex.getClass().getSimpleName(), "message", ex.getMessage()),
                    null
            );
        }
    }

    private PathValidationResult validateRevocation(PKIXCertPathBuilderResult pathResult,
                                                    List<X509Certificate> candidateCertificates,
                                                    Set<TrustAnchor> trustAnchors) {
        if (pathResult == null) {
            return new PathValidationResult(false, "External revocation check skipped because cert path is missing", null, null);
        }

        try {
            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            PKIXParameters parameters = new PKIXParameters(trustAnchors);
            parameters.setDate(new Date());
            parameters.setRevocationEnabled(false);
            parameters.addCertStore(buildCollectionCertStore(candidateCertificates));

            PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) validator.getRevocationChecker();
            revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.PREFER_CRLS));
            parameters.addCertPathChecker(revocationChecker);

            validator.validate(pathResult.getCertPath(), parameters);
            return new PathValidationResult(
                    true,
                    "External certificate revocation check passed",
                    mapOf("checker", "PKIXRevocationChecker", "mode", "PREFER_CRLS_WITH_OCSP_FALLBACK"),
                    pathResult
            );
        } catch (Exception ex) {
            return new PathValidationResult(
                    false,
                    "External revocation status could not be confirmed: " + ex.getMessage(),
                    mapOf("error", ex.getClass().getSimpleName(), "message", ex.getMessage()),
                    pathResult
            );
        }
    }

    private PKIXCertPathBuilderResult buildPath(X509Certificate signerCertificate,
                                                List<X509Certificate> candidateCertificates,
                                                Set<TrustAnchor> trustAnchors) throws Exception {
        X509CertSelector target = new X509CertSelector();
        target.setCertificate(signerCertificate);

        PKIXBuilderParameters parameters = new PKIXBuilderParameters(trustAnchors, target);
        parameters.setDate(new Date());
        parameters.setRevocationEnabled(false);
        parameters.addCertStore(buildCollectionCertStore(candidateCertificates));

        CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
        return (PKIXCertPathBuilderResult) builder.build(parameters);
    }

    private CertStore buildCollectionCertStore(List<X509Certificate> candidateCertificates) throws Exception {
        return CertStore.getInstance("Collection", new CollectionCertStoreParameters(candidateCertificates));
    }

    private List<X509Certificate> parseCertificates(byte[] bytes) throws Exception {
        List<X509Certificate> certificates = new ArrayList<>();
        String text = new String(bytes, StandardCharsets.UTF_8);
        Matcher matcher = PEM_CERT_PATTERN.matcher(text);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        while (matcher.find()) {
            String pemBlock = matcher.group();
            certificates.add((X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(pemBlock.getBytes(StandardCharsets.UTF_8))
            ));
        }

        if (!certificates.isEmpty()) {
            return certificates;
        }

        Collection<? extends java.security.cert.Certificate> parsed = factory.generateCertificates(new ByteArrayInputStream(bytes));
        for (java.security.cert.Certificate certificate : parsed) {
            if (certificate instanceof X509Certificate x509Certificate) {
                certificates.add(x509Certificate);
            }
        }

        if (!certificates.isEmpty()) {
            return certificates;
        }

        certificates.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes)));
        return certificates;
    }

    private List<X509Certificate> mergeCertificates(X509Certificate signerCertificate,
                                                    List<X509Certificate> embeddedCertificates,
                                                    List<X509Certificate> uploadedCertificates) {
        List<X509Certificate> merged = new ArrayList<>();
        merged.add(signerCertificate);
        merged.addAll(embeddedCertificates);
        merged.addAll(uploadedCertificates);
        return merged;
    }

    private List<X509Certificate> deduplicate(List<X509Certificate> certificates) {
        Map<String, X509Certificate> deduplicated = new LinkedHashMap<>();
        for (X509Certificate certificate : certificates) {
            if (certificate == null) {
                continue;
            }
            try {
                deduplicated.putIfAbsent(Base64.getEncoder().encodeToString(certificate.getEncoded()), certificate);
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(deduplicated.values());
    }

    private boolean isSelfSigned(X509Certificate certificate) {
        try {
            certificate.verify(certificate.getPublicKey());
            return certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal());
        } catch (Exception ex) {
            return false;
        }
    }

    private String resolveTrustAnchorSubject(TrustAnchor trustAnchor) {
        if (trustAnchor == null) {
            return null;
        }
        if (trustAnchor.getTrustedCert() != null) {
            return trustAnchor.getTrustedCert().getSubjectX500Principal().getName();
        }
        return trustAnchor.getCAName();
    }

    private Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key != null && value != null) {
                map.put(String.valueOf(key), value);
            }
        }
        return map;
    }

    public record ExternalTrustValidationResult(
            boolean trustAnchorAvailable,
            String trustAnchorMessage,
            Map<String, Object> trustAnchorRawData,
            boolean trustValid,
            String trustMessage,
            Map<String, Object> trustRawData,
            boolean revocationValid,
            String revocationMessage,
            Map<String, Object> revocationRawData
    ) {
    }

    private record PathValidationResult(
            boolean valid,
            String message,
            Map<String, Object> rawData,
            PKIXCertPathBuilderResult pathResult
    ) {
    }
}
