package com.ct556.step_up_auth_service.service;

import com.yubico.webauthn.data.ByteArray;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.UUID;

@Component
public class WebAuthnUserHandleCodec {

    public ByteArray fromUserId(UUID userId) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(userId.getMostSignificantBits());
        buffer.putLong(userId.getLeastSignificantBits());
        return new ByteArray(buffer.array());
    }

    public UUID toUserId(ByteArray userHandle) {
        ByteBuffer buffer = ByteBuffer.wrap(userHandle.getBytes());
        long mostSignificantBits = buffer.getLong();
        long leastSignificantBits = buffer.getLong();
        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
