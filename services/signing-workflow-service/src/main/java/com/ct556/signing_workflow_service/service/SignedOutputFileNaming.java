package com.ct556.signing_workflow_service.service;

import java.util.Locale;

public final class SignedOutputFileNaming {

    private static final String CMS_CONTENT_TYPE = "application/pkcs7-mime";

    private SignedOutputFileNaming() {
    }

    public static String normalize(String outputFileName, String outputContentType, String originalFileName) {
        String resolvedName = firstNonBlank(outputFileName, originalFileName, "signed-output.bin");
        String normalizedLowerCase = resolvedName.toLowerCase(Locale.ROOT);

        if (normalizedLowerCase.endsWith(".p7m")) {
            return resolvedName;
        }

        if (normalizedLowerCase.endsWith(".p7c")) {
            return resolvedName.substring(0, resolvedName.length() - 4) + ".p7m";
        }

        if (isCmsSignedContainer(outputContentType)) {
            return resolvedName + ".p7m";
        }

        return resolvedName;
    }

    public static boolean isCmsSignedContainer(String contentType) {
        return contentType != null && CMS_CONTENT_TYPE.equalsIgnoreCase(contentType.trim());
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return fallback;
    }
}
