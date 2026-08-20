package com.ct556.certification_service.vault;

import com.ct556.certification_service.config.VaultProperties;
import com.ct556.certification_service.exception.BadRequestException;
import com.ct556.certification_service.exception.ResourceNotFoundException;
import com.ct556.certification_service.exception.VaultIntegrationException;
import com.ct556.certification_service.vault.dto.VaultIssueCertificateResponse;
import com.ct556.certification_service.vault.dto.VaultMountRequest;
import com.ct556.certification_service.vault.dto.VaultReadCertificateResponse;
import com.ct556.certification_service.vault.dto.VaultRevokeCertificateRequest;
import com.ct556.certification_service.vault.dto.VaultRevokeCertificateResponse;
import com.ct556.certification_service.vault.dto.VaultSignCertificateRequest;
import com.ct556.certification_service.vault.dto.VaultTransitCreateKeyRequest;
import com.ct556.certification_service.vault.dto.VaultTransitReadKeyResponse;
import com.ct556.certification_service.vault.dto.VaultTransitSignRequest;
import com.ct556.certification_service.vault.dto.VaultTransitSignResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;

@Component
public class VaultPkiClient {

    private static final String TRANSIT_RSA_KEY_TYPE = "rsa-2048";
    private static final String TRANSIT_HASH_ALGORITHM = "sha2-256";
    private static final String TRANSIT_SIGNATURE_ALGORITHM = "pkcs1v15";

    private final RestClient vaultRestClient;
    private final VaultProperties vaultProperties;
    private volatile boolean transitMountReady;

    public VaultPkiClient(RestClient vaultRestClient, VaultProperties vaultProperties) {
        this.vaultRestClient = vaultRestClient;
        this.vaultProperties = vaultProperties;
    }

    public VaultIssueCertificateResponse.Data signCertificateCsr(VaultSignCertificateRequest request) {
        try {
            VaultIssueCertificateResponse response = vaultRestClient.post()
                    .uri("/v1/{mountPath}/sign/{roleName}", vaultProperties.mountPath(), vaultProperties.issueRole())
                    .header("X-Vault-Token", vaultProperties.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(VaultIssueCertificateResponse.class);

            if (response == null || response.data() == null) {
                throw new VaultIntegrationException("Vault returned empty sign certificate response");
            }

            return response.data();
        } catch (RestClientResponseException ex) {
            throw translateVaultException("sign certificate CSR", ex);
        } catch (RestClientException ex) {
            throw new VaultIntegrationException("Cannot connect to Vault when signing certificate CSR", ex);
        }
    }

    public void createTransitRsaKey(String keyName) {
        ensureTransitMount();
        try {
            vaultRestClient.post()
                    .uri("/v1/{transitMountPath}/keys/{keyName}", vaultProperties.transitMountPath(), keyName)
                    .header("X-Vault-Token", vaultProperties.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new VaultTransitCreateKeyRequest(TRANSIT_RSA_KEY_TYPE, false, false))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError() && responseBodyContains(ex, "already exists")) {
                return;
            }
            throw translateVaultException("create transit RSA key", ex);
        } catch (RestClientException ex) {
            throw new VaultIntegrationException("Cannot connect to Vault when creating transit RSA key", ex);
        }
    }

    public String readTransitPublicKeyPem(String keyName) {
        ensureTransitMount();
        try {
            VaultTransitReadKeyResponse response = vaultRestClient.get()
                    .uri("/v1/{transitMountPath}/keys/{keyName}", vaultProperties.transitMountPath(), keyName)
                    .header("X-Vault-Token", vaultProperties.token())
                    .retrieve()
                    .body(VaultTransitReadKeyResponse.class);

            if (response == null || response.data() == null || response.data().keys() == null || response.data().keys().isEmpty()) {
                throw new VaultIntegrationException("Vault returned empty transit key metadata");
            }

            Integer latestVersion = response.data().latestVersion();
            VaultTransitReadKeyResponse.KeyVersion keyVersion = latestVersion == null
                    ? response.data().keys().values().iterator().next()
                    : response.data().keys().get(String.valueOf(latestVersion));

            if (keyVersion == null || !StringUtils.hasText(keyVersion.publicKey())) {
                throw new VaultIntegrationException("Vault transit key does not expose a public key");
            }

            return keyVersion.publicKey();
        } catch (RestClientResponseException ex) {
            throw translateVaultException("read transit public key", ex);
        } catch (RestClientException ex) {
            throw new VaultIntegrationException("Cannot connect to Vault when reading transit public key", ex);
        }
    }

    public byte[] signWithTransitKey(String keyName, byte[] content) {
        ensureTransitMount();
        try {
            VaultTransitSignResponse response = vaultRestClient.post()
                    .uri("/v1/{transitMountPath}/sign/{keyName}", vaultProperties.transitMountPath(), keyName)
                    .header("X-Vault-Token", vaultProperties.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new VaultTransitSignRequest(
                            Base64.getEncoder().encodeToString(content),
                            TRANSIT_HASH_ALGORITHM,
                            TRANSIT_SIGNATURE_ALGORITHM
                    ))
                    .retrieve()
                    .body(VaultTransitSignResponse.class);

            if (response == null || response.data() == null || !StringUtils.hasText(response.data().signature())) {
                throw new VaultIntegrationException("Vault returned empty transit signature");
            }

            return decodeTransitSignature(response.data().signature());
        } catch (RestClientResponseException ex) {
            throw translateVaultException("sign with transit key", ex);
        } catch (RestClientException ex) {
            throw new VaultIntegrationException("Cannot connect to Vault when signing with transit key", ex);
        }
    }

    public VaultRevokeCertificateResponse.Data revokeCertificateBySerial(String serialNumber) {
        try {
            VaultRevokeCertificateResponse response = vaultRestClient.post()
                    .uri("/v1/{mountPath}/revoke", vaultProperties.mountPath())
                    .header("X-Vault-Token", vaultProperties.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new VaultRevokeCertificateRequest(serialNumber))
                    .retrieve()
                    .body(VaultRevokeCertificateResponse.class);

            if (response == null || response.data() == null) {
                return new VaultRevokeCertificateResponse.Data(null);
            }

            return response.data();
        } catch (RestClientResponseException ex) {
            throw translateVaultException("revoke certificate", ex);
        } catch (RestClientException ex) {
            throw new VaultIntegrationException("Cannot connect to Vault when revoking certificate", ex);
        }
    }

    public VaultReadCertificateResponse.Data readCertificateBySerial(String serialNumber) {
        try {
            VaultReadCertificateResponse response = vaultRestClient.get()
                    .uri("/v1/{mountPath}/cert/{serialNumber}", vaultProperties.mountPath(), serialNumber)
                    .header("X-Vault-Token", vaultProperties.token())
                    .retrieve()
                    .body(VaultReadCertificateResponse.class);

            if (response == null) {
                return null;
            }

            return response.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw translateVaultException("read certificate", ex);
        } catch (RestClientException ex) {
            throw new VaultIntegrationException("Cannot connect to Vault when reading certificate", ex);
        }
    }

    private void ensureTransitMount() {
        if (transitMountReady) {
            return;
        }

        try {
            vaultRestClient.post()
                    .uri("/v1/sys/mounts/{transitMountPath}", vaultProperties.transitMountPath())
                    .header("X-Vault-Token", vaultProperties.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new VaultMountRequest("transit"))
                    .retrieve()
                    .toBodilessEntity();
            transitMountReady = true;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 403) {
                transitMountReady = true;
                return;
            }
            if (ex.getStatusCode().is4xxClientError()
                    && (responseBodyContains(ex, "path is already in use") || responseBodyContains(ex, "existing mount"))) {
                transitMountReady = true;
                return;
            }
            throw translateVaultException("enable transit mount", ex);
        } catch (RestClientException ex) {
            throw new VaultIntegrationException("Cannot connect to Vault when enabling transit mount", ex);
        }
    }

    private byte[] decodeTransitSignature(String vaultSignature) {
        String encoded = vaultSignature;
        int lastSeparator = encoded.lastIndexOf(':');
        if (lastSeparator >= 0) {
            encoded = encoded.substring(lastSeparator + 1);
        }

        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException ex) {
            throw new VaultIntegrationException("Vault returned invalid transit signature encoding", ex);
        }
    }

    private boolean responseBodyContains(RestClientResponseException ex, String expected) {
        return StringUtils.hasText(ex.getResponseBodyAsString())
                && ex.getResponseBodyAsString().toLowerCase().contains(expected.toLowerCase());
    }

    private RuntimeException translateVaultException(String action, RestClientResponseException ex) {
        String body = StringUtils.hasText(ex.getResponseBodyAsString())
                ? ex.getResponseBodyAsString()
                : ex.getStatusText();

        if (ex.getStatusCode().value() == 404) {
            return new ResourceNotFoundException("Vault resource not found while trying to " + action + ": " + body);
        }

        if (ex.getStatusCode().is4xxClientError()) {
            return new BadRequestException("Vault rejected request to " + action + ": " + body);
        }

        return new VaultIntegrationException("Vault failed to " + action + ": " + body, ex);
    }
}
