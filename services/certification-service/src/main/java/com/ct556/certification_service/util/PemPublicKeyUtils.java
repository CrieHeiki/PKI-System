package com.ct556.certification_service.util;

import com.ct556.certification_service.exception.BadRequestException;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;

public final class PemPublicKeyUtils {

    private PemPublicKeyUtils() {
    }

    public static PublicKey parsePublicKey(String publicKeyPem) {
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw new BadRequestException("publicKeyPem is empty");
        }

        try (PEMParser pemParser = new PEMParser(new StringReader(publicKeyPem))) {
            Object object = pemParser.readObject();

            if (object instanceof SubjectPublicKeyInfo publicKeyInfo) {
                return new JcaPEMKeyConverter().getPublicKey(publicKeyInfo);
            }

            if (object instanceof RSAPublicKey rsaPublicKey) {
                return KeyFactory.getInstance("RSA").generatePublic(
                        new RSAPublicKeySpec(rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent())
                );
            }

            throw new BadRequestException("Vault Transit public key PEM is not an RSA public key");
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to parse Vault Transit public key PEM");
        }
    }
}
