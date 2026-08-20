package com.ct556.certification_service.util;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Locale;

public final class X509CertificateUtils {

    private X509CertificateUtils() {
    }

    public static String extractSubjectDn(String certificatePem) {
        return parseCertificate(certificatePem).getSubjectX500Principal().getName();
    }

    public static OffsetDateTime extractIssuedAt(String certificatePem) {
        return OffsetDateTime.ofInstant(
                parseCertificate(certificatePem).getNotBefore().toInstant(),
                ZoneOffset.UTC
        );
    }

    public static OffsetDateTime extractExpiresAt(String certificatePem) {
        return OffsetDateTime.ofInstant(
                parseCertificate(certificatePem).getNotAfter().toInstant(),
                ZoneOffset.UTC
        );
    }

    public static String extractOrganization(String subjectDn) {
        return extractRdnValue(subjectDn, "O");
    }

    public static String extractCountry(String subjectDn) {
        return extractRdnValue(subjectDn, "C");
    }

    public static String normalizeSerialNumber(String serialNumber) {
        if (serialNumber == null) {
            return null;
        }

        return serialNumber
                .trim()
                .replace("-", ":")
                .toUpperCase(Locale.ROOT);
    }

    public static String normalizeSerialLookup(String serialNumber) {
        if (serialNumber == null) {
            return null;
        }

        String normalized = serialNumber
                .replaceAll("[^A-Fa-f0-9]", "")
                .toUpperCase(Locale.ROOT);

        normalized = normalized.replaceFirst("^0+", "");
        return normalized.isBlank() ? "0" : normalized;
    }

    public static boolean serialNumbersMatch(String left, String right) {
        if (left == null || right == null) {
            return false;
        }

        return normalizeSerialLookup(left).equals(normalizeSerialLookup(right));
    }

    private static String extractRdnValue(String subjectDn, String key) {
        if (subjectDn == null || subjectDn.isBlank()) {
            return null;
        }

        try {
            LdapName ldapName = new LdapName(subjectDn);
            for (Rdn rdn : ldapName.getRdns()) {
                if (key.equalsIgnoreCase(rdn.getType()) && rdn.getValue() != null) {
                    return String.valueOf(rdn.getValue());
                }
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static X509Certificate parseCertificate(String pem) {
        try {
            String normalized = pem
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "");

            byte[] decoded = Base64.getMimeDecoder().decode(normalized);

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(decoded));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot parse certificate PEM", ex);
        }
    }
}
