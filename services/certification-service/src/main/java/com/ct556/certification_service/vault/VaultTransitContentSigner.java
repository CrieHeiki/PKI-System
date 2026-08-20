package com.ct556.certification_service.vault;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class VaultTransitContentSigner implements ContentSigner {

    private static final AlgorithmIdentifier SIGNATURE_ALGORITHM =
            new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");

    private final VaultPkiClient vaultPkiClient;
    private final String keyName;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public VaultTransitContentSigner(VaultPkiClient vaultPkiClient, String keyName) {
        this.vaultPkiClient = vaultPkiClient;
        this.keyName = keyName;
    }

    @Override
    public AlgorithmIdentifier getAlgorithmIdentifier() {
        return SIGNATURE_ALGORITHM;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public byte[] getSignature() {
        return vaultPkiClient.signWithTransitKey(keyName, outputStream.toByteArray());
    }
}
