package com.example.messenger.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NoopMessageCryptoTest {

    private final NoopMessageCrypto crypto = new NoopMessageCrypto();

    @Test
    void encryptAndDecrypt_returnInputUnchanged() {
        String payload = "hello";

        assertEquals(payload, crypto.encrypt(42L, payload));
        assertEquals(payload, crypto.decrypt(42L, payload));
    }

    @Test
    void encryptAndDecrypt_allowNullPayload() {
        assertNull(crypto.encrypt(42L, null));
        assertNull(crypto.decrypt(42L, null));
    }
}
