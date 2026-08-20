package com.ct556.signing_workflow_service.service;

import com.ct556.signing_workflow_service.client.CertificateGateway;
import com.ct556.signing_workflow_service.client.dto.AccountMetadataResponse;
import com.ct556.signing_workflow_service.client.dto.CertificateMetadataResponse;
import com.ct556.signing_workflow_service.client.dto.CertificateSignResponse;
import com.ct556.signing_workflow_service.client.dto.DocumentPayloadResponse;
import com.ct556.signing_workflow_service.client.dto.TimestampTokenResponse;
import com.ct556.signing_workflow_service.dto.request.SignatureAppearanceOverrideRequest;
import com.ct556.signing_workflow_service.entity.SigningRequest;
import com.ct556.signing_workflow_service.exception.BusinessException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSUpdateInfo;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

@Service
public class EmbeddedSignatureOutputService {

    private static final BouncyCastleProvider BC_PROVIDER = new BouncyCastleProvider();
    private static final String DEFAULT_DIGEST_ALGORITHM = "SHA-256";
    private static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String CMS_CONTENT_TYPE = "application/pkcs7-mime";
    private static final DateTimeFormatter SIGNATURE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private final CertificateGateway certificateGateway;

    public EmbeddedSignatureOutputService(CertificateGateway certificateGateway) {
        this.certificateGateway = certificateGateway;
    }

    public EmbeddedSignatureResult buildSignedOutput(SigningRequest signingRequest,
                                                     DocumentPayloadResponse document,
                                                     CertificateMetadataResponse certificate,
                                                     AccountMetadataResponse signerAccount,
                                                     SignatureAppearanceOverrideRequest appearanceOverride) {
        byte[] originalContent = decodeBase64(document.getContentBase64());
        String documentDigestBase64 = base64(sha256(originalContent));
        X509Certificate signingCertificate = parseCertificate(certificate.getCertificatePem());

        if (isPdfDocument(document)) {
            return buildSignedPdf(
                    signingRequest,
                    document,
                    certificate,
                    signerAccount,
                    appearanceOverride,
                    signingCertificate,
                    originalContent,
                    documentDigestBase64
            );
        }
        return buildSignedCms(signingRequest, document, certificate, signingCertificate, originalContent, documentDigestBase64);
    }

    private EmbeddedSignatureResult buildSignedPdf(SigningRequest signingRequest,
                                                   DocumentPayloadResponse document,
                                                   CertificateMetadataResponse certificate,
                                                   AccountMetadataResponse signerAccount,
                                                   SignatureAppearanceOverrideRequest appearanceOverride,
                                                   X509Certificate signingCertificate,
                                                   byte[] originalContent,
                                                   String documentDigestBase64) {
        try (PDDocument pdfDocument = PDDocument.load(originalContent);
             SignatureOptions signatureOptions = new SignatureOptions();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Instant signingInstant = Instant.now();
            addVisibleSignatureAppearance(
                    pdfDocument,
                    signingRequest,
                    certificate,
                    signerAccount,
                    appearanceOverride,
                    signingInstant
            );

            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
            signature.setName(firstNonBlank(certificate.getSubjectDn(), "CT556 Internal PKI"));
            signature.setReason("CT556 internal digital signing workflow");
            signature.setLocation("CT556 Internal PKI");

            Calendar signingTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            signingTime.setTimeInMillis(signingInstant.toEpochMilli());
            signature.setSignDate(signingTime);

            signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 4);
            pdfDocument.addSignature(signature, signatureOptions);

            ExternalSigningSupport externalSigning = pdfDocument.saveIncrementalForExternalSigning(outputStream);
            byte[] contentToSign = externalSigning.getContent().readAllBytes();

            CmsSignatureResult cmsSignature = createCmsSignature(
                    contentToSign,
                    certificate,
                    signingCertificate,
                    signingRequest.getCertificateId(),
                    false
            );
            externalSigning.setSignature(cmsSignature.cmsBytes());

            return new EmbeddedSignatureResult(
                    outputStream.toByteArray(),
                    buildSignedPdfFileName(document.getFileName()),
                    PDF_CONTENT_TYPE,
                    DEFAULT_DIGEST_ALGORITHM,
                    cmsSignature.signResponse().getSignatureAlgorithm(),
                    cmsSignature.signResponse().getSignatureValueBase64(),
                    documentDigestBase64,
                    (long) originalContent.length
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build embedded PDF signature", ex);
        }
    }

    private void addVisibleSignatureAppearance(PDDocument pdfDocument,
                                               SigningRequest signingRequest,
                                               CertificateMetadataResponse certificate,
                                               AccountMetadataResponse signerAccount,
                                               SignatureAppearanceOverrideRequest appearanceOverride,
                                               Instant signingInstant) throws Exception {
        if (pdfDocument.getNumberOfPages() == 0) {
            throw new BusinessException("PDF document has no pages");
        }

        SignaturePlacement placement = resolveSignaturePlacement(pdfDocument, signerAccount, appearanceOverride);
        PDPage page = pdfDocument.getPage(placement.pageIndex());
        PDRectangle pageBox = page.getCropBox();
        float x = placement.x();
        float y = placement.y();
        float stampWidth = placement.width();
        float stampHeight = placement.height();

        byte[] signatureImageBytes = decodeOptionalBase64(signerAccount == null ? null : signerAccount.getSignatureImageBase64());
        boolean imageOnly = resolveImageOnlyPreference(signerAccount, appearanceOverride)
                && signatureImageBytes != null
                && signatureImageBytes.length > 0;
        String signerDisplay = resolveSignerDisplayName(signerAccount, certificate);
        String serialDisplay = truncateLine(firstNonBlank(certificate.getSerialNumber(), "N/A"), 24);
        String signedAtDisplay = SIGNATURE_TIME_FORMATTER.format(signingInstant);
        String requestDisplay = truncateLine(signingRequest.getId().toString(), 18);

        try (PDPageContentStream contentStream = new PDPageContentStream(
                pdfDocument,
                page,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true
        )) {
            if (imageOnly) {
                PDImageXObject signatureImage = PDImageXObject.createFromByteArray(pdfDocument, signatureImageBytes, "signature-image");
                drawSignatureImage(contentStream, signatureImage, x, y, stampWidth, stampHeight);
                markPageForIncrementalSave(pdfDocument, page);
                return;
            }

            contentStream.setNonStrokingColor(252, 253, 255);
            contentStream.addRect(x, y, stampWidth, stampHeight);
            contentStream.fill();

            contentStream.setStrokingColor(30, 41, 59);
            contentStream.setLineWidth(1.2f);
            contentStream.addRect(x, y, stampWidth, stampHeight);
            contentStream.stroke();

            contentStream.setNonStrokingColor(15, 23, 42);
            drawTextLine(contentStream, PDType1Font.HELVETICA_BOLD, 9.5f, x + 12f, y + stampHeight - 16f, "CT556 DIGITAL SIGNATURE");

            float imageAreaX = x + 12f;
            float imageAreaY = y + 46f;
            float imageAreaWidth = stampWidth - 24f;
            float imageAreaHeight = 30f;

            if (signatureImageBytes != null && signatureImageBytes.length > 0) {
                try {
                    PDImageXObject signatureImage = PDImageXObject.createFromByteArray(pdfDocument, signatureImageBytes, "signature-image");
                    drawSignatureImage(contentStream, signatureImage, imageAreaX, imageAreaY, imageAreaWidth, imageAreaHeight);
                } catch (Exception ignored) {
                    drawTextLine(contentStream, PDType1Font.HELVETICA_OBLIQUE, 9f, imageAreaX, imageAreaY + 10f, "Visual signature image unavailable");
                }
            } else {
                drawTextLine(contentStream, PDType1Font.HELVETICA_OBLIQUE, 9f, imageAreaX, imageAreaY + 10f, "No signature image uploaded");
            }

            contentStream.setNonStrokingColor(71, 85, 105);
            drawTextLine(contentStream, PDType1Font.HELVETICA_BOLD, 8f, x + 12f, y + 28f, "Signer: " + signerDisplay);
            drawTextLine(contentStream, PDType1Font.HELVETICA, 7.5f, x + 12f, y + 18f, "Serial: " + serialDisplay);
            drawTextLine(contentStream, PDType1Font.HELVETICA, 7.5f, x + 12f, y + 9f, "Signed: " + signedAtDisplay);
            drawTextLine(contentStream, PDType1Font.HELVETICA, 7.5f, x + stampWidth - 102f, y + 9f, "Req: " + requestDisplay);
        }

        markPageForIncrementalSave(pdfDocument, page);
    }

    private void drawSignatureImage(PDPageContentStream contentStream,
                                    PDImageXObject signatureImage,
                                    float x,
                                    float y,
                                    float maxWidth,
                                    float maxHeight) throws Exception {
        float imageWidth = signatureImage.getWidth();
        float imageHeight = signatureImage.getHeight();
        float scale = Math.min(maxWidth / imageWidth, maxHeight / imageHeight);
        float drawWidth = imageWidth * scale;
        float drawHeight = imageHeight * scale;
        float drawX = x + ((maxWidth - drawWidth) / 2f);
        float drawY = y + ((maxHeight - drawHeight) / 2f);
        contentStream.drawImage(signatureImage, drawX, drawY, drawWidth, drawHeight);
    }

    private SignaturePlacement resolveSignaturePlacement(PDDocument pdfDocument,
                                                         AccountMetadataResponse signerAccount,
                                                         SignatureAppearanceOverrideRequest appearanceOverride) {
        int pageNumber = appearanceOverride != null && appearanceOverride.getPageNumber() != null
                ? appearanceOverride.getPageNumber()
                : pdfDocument.getNumberOfPages();
        if (pageNumber < 1 || pageNumber > pdfDocument.getNumberOfPages()) {
            throw new BusinessException("Signature appearance pageNumber is out of range");
        }

        PDPage page = pdfDocument.getPage(pageNumber - 1);
        PDRectangle pageBox = page.getCropBox();

        int xPercent = firstNonNull(
                appearanceOverride == null ? null : appearanceOverride.getXPercent(),
                signerAccount == null ? null : signerAccount.getSignatureBoxXPercent(),
                62
        );
        int yPercent = firstNonNull(
                appearanceOverride == null ? null : appearanceOverride.getYPercent(),
                signerAccount == null ? null : signerAccount.getSignatureBoxYPercent(),
                4
        );
        int widthPercent = firstNonNull(
                appearanceOverride == null ? null : appearanceOverride.getWidthPercent(),
                signerAccount == null ? null : signerAccount.getSignatureBoxWidthPercent(),
                34
        );
        int heightPercent = firstNonNull(
                appearanceOverride == null ? null : appearanceOverride.getHeightPercent(),
                signerAccount == null ? null : signerAccount.getSignatureBoxHeightPercent(),
                16
        );

        validatePercentWindow(xPercent, yPercent, widthPercent, heightPercent);

        float width = pageBox.getWidth() * (widthPercent / 100f);
        float height = pageBox.getHeight() * (heightPercent / 100f);
        float x = pageBox.getLowerLeftX() + (pageBox.getWidth() * (xPercent / 100f));
        float y = pageBox.getLowerLeftY() + (pageBox.getHeight() * (yPercent / 100f));

        width = Math.max(90f, Math.min(width, pageBox.getWidth()));
        height = Math.max(36f, Math.min(height, pageBox.getHeight()));
        x = Math.max(pageBox.getLowerLeftX(), Math.min(x, pageBox.getLowerLeftX() + pageBox.getWidth() - width));
        y = Math.max(pageBox.getLowerLeftY(), Math.min(y, pageBox.getLowerLeftY() + pageBox.getHeight() - height));

        return new SignaturePlacement(pageNumber - 1, x, y, width, height);
    }

    private void validatePercentWindow(int xPercent,
                                       int yPercent,
                                       int widthPercent,
                                       int heightPercent) {
        if (xPercent + widthPercent > 100) {
            throw new BusinessException("Signature appearance width exceeds page boundary");
        }
        if (yPercent + heightPercent > 100) {
            throw new BusinessException("Signature appearance height exceeds page boundary");
        }
    }

    private boolean resolveImageOnlyPreference(AccountMetadataResponse signerAccount,
                                               SignatureAppearanceOverrideRequest appearanceOverride) {
        if (appearanceOverride != null && appearanceOverride.getImageOnly() != null) {
            return appearanceOverride.getImageOnly();
        }
        return signerAccount != null && signerAccount.isSignatureImageOnly();
    }

    private int firstNonNull(Integer first, Integer second, int fallback) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return fallback;
    }

    private void drawTextLine(PDPageContentStream contentStream,
                              PDType1Font font,
                              float fontSize,
                              float x,
                              float y,
                              String text) throws Exception {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private void markPageForIncrementalSave(PDDocument pdfDocument, PDPage page) {
        page.getCOSObject().setNeedToBeUpdated(true);

        if (page.getCOSObject().containsKey(COSName.CONTENTS)) {
            COSBase contentsObject = page.getCOSObject().getDictionaryObject(COSName.CONTENTS);
            if (contentsObject instanceof COSUpdateInfo updateInfo) {
                updateInfo.setNeedToBeUpdated(true);
            }
        }

        PDResources resources = page.getResources();
        if (resources != null) {
            resources.getCOSObject().setNeedToBeUpdated(true);
        }

        pdfDocument.getPages().getCOSObject().setNeedToBeUpdated(true);
        pdfDocument.getDocumentCatalog().getCOSObject().setNeedToBeUpdated(true);
    }

    private EmbeddedSignatureResult buildSignedCms(SigningRequest signingRequest,
                                                   DocumentPayloadResponse document,
                                                   CertificateMetadataResponse certificate,
                                                   X509Certificate signingCertificate,
                                                   byte[] originalContent,
                                                   String documentDigestBase64) {
        try {
            CmsSignatureResult cmsSignature = createCmsSignature(
                    originalContent,
                    certificate,
                    signingCertificate,
                    signingRequest.getCertificateId(),
                    true
            );

            return new EmbeddedSignatureResult(
                    cmsSignature.cmsBytes(),
                    buildCmsFileName(document.getFileName()),
                    CMS_CONTENT_TYPE,
                    DEFAULT_DIGEST_ALGORITHM,
                    cmsSignature.signResponse().getSignatureAlgorithm(),
                    cmsSignature.signResponse().getSignatureValueBase64(),
                    documentDigestBase64,
                    (long) originalContent.length
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build CMS/CAdES signed package", ex);
        }
    }

    private CmsSignatureResult createCmsSignature(byte[] contentToSign,
                                                  CertificateMetadataResponse certificateMetadata,
                                                  X509Certificate signingCertificate,
                                                  UUID certificateId,
                                                  boolean encapsulate) throws Exception {
        X509CertificateHolder certHolder = new JcaX509CertificateHolder(signingCertificate);
        RemoteSignatureContentSigner contentSigner = new RemoteSignatureContentSigner(certificateId);

        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        generator.addCertificates(new JcaCertStore(buildEmbeddedCertificateChain(signingCertificate, certificateMetadata)));

        SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder()
                        .setProvider(BC_PROVIDER)
                        .build()
        )
                .setSignedAttributeGenerator(buildCadesSignedAttributes(certHolder))
                .build(contentSigner, signingCertificate);

        generator.addSignerInfoGenerator(signerInfoGenerator);

        CMSSignedData signedData = generator.generate(new CMSProcessableByteArray(contentToSign), encapsulate);
        CMSSignedData timestampedData = appendSignatureTimestamp(signedData);
        return new CmsSignatureResult(timestampedData.getEncoded(), contentSigner.getLastSignResponse());
    }

    private CMSSignedData appendSignatureTimestamp(CMSSignedData signedData) throws Exception {
        SignerInformationStore signerStore = signedData.getSignerInfos();
        if (signerStore.size() != 1) {
            throw new IllegalStateException("Timestamping requires exactly one signer");
        }

        SignerInformation signerInformation = signerStore.getSigners().iterator().next();
        byte[] messageImprint = sha256(signerInformation.getSignature());
        TimestampTokenResponse timestampToken = certificateGateway.issueTimestampToken(
                DEFAULT_DIGEST_ALGORITHM,
                base64(messageImprint)
        );

        ContentInfo timestampContent = ContentInfo.getInstance(
                ASN1Primitive.fromByteArray(decodeBase64(timestampToken.getTimestampTokenBase64()))
        );
        Attribute timestampAttribute = new Attribute(
                PKCSObjectIdentifiers.id_aa_signatureTimeStampToken,
                new DERSet(timestampContent)
        );

        Hashtable<ASN1ObjectIdentifier, Attribute> unsignedAttributes = signerInformation.getUnsignedAttributes() == null
                ? new Hashtable<>()
                : signerInformation.getUnsignedAttributes().toHashtable();
        unsignedAttributes.put(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, timestampAttribute);

        SignerInformation updatedSigner = SignerInformation.replaceUnsignedAttributes(
                signerInformation,
                new AttributeTable(unsignedAttributes)
        );
        return CMSSignedData.replaceSigners(signedData, new SignerInformationStore(List.of(updatedSigner)));
    }

    private List<X509Certificate> buildEmbeddedCertificateChain(X509Certificate signingCertificate,
                                                                CertificateMetadataResponse certificateMetadata) {
        List<X509Certificate> certificates = new ArrayList<>();
        certificates.add(signingCertificate);

        X509Certificate issuingCa = parseOptionalCertificate(certificateMetadata == null ? null : certificateMetadata.getIssuingCaPem());
        if (issuingCa != null && !issuingCa.getSubjectX500Principal().equals(signingCertificate.getSubjectX500Principal())) {
            certificates.add(issuingCa);
        }

        return certificates;
    }

    private DefaultSignedAttributeTableGenerator buildCadesSignedAttributes(X509CertificateHolder certHolder) throws Exception {
        Hashtable<ASN1ObjectIdentifier, Attribute> attributes = new Hashtable<>();
        attributes.put(
                PKCSObjectIdentifiers.id_aa_signingCertificateV2,
                new Attribute(
                        PKCSObjectIdentifiers.id_aa_signingCertificateV2,
                        new DERSet(buildSigningCertificateV2(certHolder))
                )
        );
        return new DefaultSignedAttributeTableGenerator(new org.bouncycastle.asn1.cms.AttributeTable(attributes));
    }

    private SigningCertificateV2 buildSigningCertificateV2(X509CertificateHolder certHolder) throws Exception {
        byte[] certificateDigest = MessageDigest.getInstance(DEFAULT_DIGEST_ALGORITHM)
                .digest(certHolder.getEncoded());

        ESSCertIDv2 essCertIDv2 = new ESSCertIDv2(
                new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256),
                certificateDigest
        );
        return new SigningCertificateV2(new ESSCertIDv2[]{essCertIDv2});
    }

    private boolean isPdfDocument(DocumentPayloadResponse document) {
        String contentType = firstNonBlank(document.getContentType(), "");
        String fileName = firstNonBlank(document.getFileName(), "");
        return PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)
                || fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private X509Certificate parseCertificate(String certificatePem) {
        if (certificatePem == null || certificatePem.isBlank()) {
            throw new BusinessException("Certificate PEM is missing");
        }

        try {
            String normalized = certificatePem
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "");

            byte[] der = Base64.getDecoder().decode(normalized);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new java.io.ByteArrayInputStream(der));
        } catch (Exception ex) {
            throw new BusinessException("Certificate PEM is invalid");
        }
    }

    private X509Certificate parseOptionalCertificate(String certificatePem) {
        if (certificatePem == null || certificatePem.isBlank()) {
            return null;
        }
        try {
            return parseCertificate(certificatePem);
        } catch (BusinessException ex) {
            return null;
        }
    }

    private byte[] decodeBase64(String contentBase64) {
        try {
            return Base64.getDecoder().decode(contentBase64);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Document content is not valid Base64");
        }
    }

    private byte[] sha256(byte[] content) {
        try {
            return MessageDigest.getInstance(DEFAULT_DIGEST_ALGORITHM).digest(content);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate SHA-256 digest", ex);
        }
    }

    private String buildSignedPdfFileName(String originalFileName) {
        String fileName = firstNonBlank(originalFileName, "signed-document.pdf");
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot) + "-signed.pdf";
        }
        return fileName + "-signed.pdf";
    }

    private String buildCmsFileName(String originalFileName) {
        return firstNonBlank(originalFileName, "signed-document.bin") + ".p7m";
    }

    private byte[] decodeOptionalBase64(String contentBase64) {
        if (contentBase64 == null || contentBase64.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(contentBase64);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String resolveSignerDisplayName(AccountMetadataResponse signerAccount,
                                            CertificateMetadataResponse certificate) {
        String username = signerAccount == null ? null : signerAccount.getUsername();
        if (username != null && !username.isBlank()) {
            return truncateLine(username.trim(), 24);
        }

        String subjectDn = certificate == null ? null : certificate.getSubjectDn();
        if (subjectDn != null && !subjectDn.isBlank()) {
            for (String token : subjectDn.split(",")) {
                String trimmed = token.trim();
                if (trimmed.regionMatches(true, 0, "CN=", 0, 3)) {
                    return truncateLine(trimmed.substring(3).trim(), 24);
                }
            }
            return truncateLine(subjectDn.trim(), 24);
        }

        return "Unknown signer";
    }

    private String truncateLine(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String base64(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first.trim();
    }

    public record EmbeddedSignatureResult(
            byte[] outputBytes,
            String outputFileName,
            String outputContentType,
            String digestAlgorithm,
            String signatureAlgorithm,
            String signatureValueBase64,
            String documentDigestBase64,
            Long signedPayloadLength
    ) {
    }

    private record CmsSignatureResult(byte[] cmsBytes, CertificateSignResponse signResponse) {
    }

    private record SignaturePlacement(int pageIndex, float x, float y, float width, float height) {
    }

    private final class RemoteSignatureContentSigner implements ContentSigner {

        private final UUID certificateId;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final AlgorithmIdentifier algorithmIdentifier;
        private CertificateSignResponse lastSignResponse;

        private RemoteSignatureContentSigner(UUID certificateId) {
            this.certificateId = certificateId;
            this.algorithmIdentifier = new DefaultSignatureAlgorithmIdentifierFinder()
                    .find(DEFAULT_SIGNATURE_ALGORITHM);
        }

        @Override
        public AlgorithmIdentifier getAlgorithmIdentifier() {
            return algorithmIdentifier;
        }

        @Override
        public OutputStream getOutputStream() {
            buffer.reset();
            return buffer;
        }

        @Override
        public byte[] getSignature() {
            lastSignResponse = certificateGateway.signPayload(
                    certificateId,
                    Base64.getEncoder().encodeToString(buffer.toByteArray())
            );

            if (lastSignResponse == null || lastSignResponse.getSignatureValueBase64() == null) {
                throw new IllegalStateException("Certification service did not return a valid signature");
            }

            return Base64.getDecoder().decode(lastSignResponse.getSignatureValueBase64());
        }

        private CertificateSignResponse getLastSignResponse() {
            if (lastSignResponse == null) {
                throw new IllegalStateException("CMS/PDF signature was not generated");
            }
            return lastSignResponse;
        }
    }
}
