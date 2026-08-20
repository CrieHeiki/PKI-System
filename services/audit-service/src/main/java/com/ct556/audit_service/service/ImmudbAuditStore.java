package com.ct556.audit_service.service;

import com.ct556.audit_service.config.ImmudbProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codenotary.immudb4j.Entry;
import io.codenotary.immudb4j.ImmuClient;
import io.codenotary.immudb4j.TxHeader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Component
public class ImmudbAuditStore {

    private static final String KEY_PREFIX = "audit:event:";

    private final ImmudbProperties immudbProperties;
    private final ObjectMapper objectMapper;

    public ImmudbAuditStore(ImmudbProperties immudbProperties, ObjectMapper objectMapper) {
        this.immudbProperties = immudbProperties;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return immudbProperties.enabled();
    }

    public Long appendEvent(ImmutableAuditEvent event) {
        if (!immudbProperties.enabled()) {
            return null;
        }

        return withClient(immuClient -> {
            TxHeader txHeader = immuClient.verifiedSet(
                    buildKey(event.id()),
                    objectMapper.writeValueAsBytes(event)
            );
            return txHeader.getId();
        });
    }

    public Optional<ImmutableAuditEvent> getEvent(UUID eventId, Long txId) {
        if (!immudbProperties.enabled() || txId == null) {
            return Optional.empty();
        }

        return withClient(immuClient -> {
            Entry entry = immuClient.verifiedGetAtTx(buildKey(eventId), txId);
            return Optional.of(objectMapper.readValue(entry.getValue(), ImmutableAuditEvent.class));
        });
    }

    private byte[] buildKey(UUID eventId) {
        return (KEY_PREFIX + eventId).getBytes(StandardCharsets.UTF_8);
    }

    private <T> T withClient(ImmuOperation<T> operation) {
        ImmuClient immuClient = ImmuClient.newBuilder()
                .withServerUrl(immudbProperties.host())
                .withServerPort(immudbProperties.port())
                .build();

        try {
            immuClient.openSession(
                    immudbProperties.database(),
                    immudbProperties.username(),
                    immudbProperties.password()
            );
            return operation.apply(immuClient);
        } catch (Exception ex) {
            throw new IllegalStateException("ImmuDB operation failed", ex);
        } finally {
            try {
                immuClient.closeSession();
            } catch (Exception ignored) {
            }
            try {
                immuClient.shutdown();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
            }
        }
    }

    @FunctionalInterface
    private interface ImmuOperation<T> {
        T apply(ImmuClient immuClient) throws Exception;
    }
}
