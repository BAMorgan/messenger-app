package com.example.messenger.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenTest {

    @Test
    void constructorAndSetters_populateFields() throws Exception {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        RefreshToken token = new RefreshToken(1L, "tkn", expiresAt);
        setField(token, "id", 5L);

        token.setUserId(2L);
        token.setToken("tkn-2");
        token.setExpiresAt(expiresAt.plusSeconds(60));
        token.setRevoked(true);

        assertEquals(5L, token.getId());
        assertEquals(2L, token.getUserId());
        assertEquals("tkn-2", token.getToken());
        assertEquals(expiresAt.plusSeconds(60), token.getExpiresAt());
        assertTrue(token.getRevoked());
    }

    @Test
    void isExpiredAndIsValid_reflectExpirationAndRevocation() {
        RefreshToken valid = new RefreshToken(1L, "valid", Instant.now().plusSeconds(300));
        assertFalse(valid.isExpired());
        assertTrue(valid.isValid());

        RefreshToken expired = new RefreshToken(1L, "expired", Instant.now().minusSeconds(300));
        assertTrue(expired.isExpired());
        assertFalse(expired.isValid());

        RefreshToken revoked = new RefreshToken(1L, "revoked", Instant.now().plusSeconds(300));
        revoked.setRevoked(true);
        assertFalse(revoked.isExpired());
        assertFalse(revoked.isValid());
    }

    @Test
    void noArgConstructor_isAvailableForJpa() throws Exception {
        Constructor<RefreshToken> constructor = RefreshToken.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        RefreshToken token = constructor.newInstance();

        assertNotNull(token);
        assertNull(token.getId());
        assertNull(token.getUserId());
        assertNull(token.getToken());
        assertNull(token.getExpiresAt());
        assertFalse(token.getRevoked());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
