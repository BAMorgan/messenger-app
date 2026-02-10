package com.example.messenger.domain;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppUserTest {

    @Test
    void constructorAndSetters_populateFields() throws Exception {
        AppUser user = new AppUser("alice", "alice@example.com", "hashed");
        setField(user, "id", 99L);

        user.setEmail("alice2@example.com");
        user.setPasswordHash("hashed2");
        user.setDisplayName("Alice");
        user.setAvatarUrl("https://example.com/avatar.png");

        assertEquals(99L, user.getId());
        assertEquals("alice", user.getUsername());
        assertEquals("alice2@example.com", user.getEmail());
        assertEquals("hashed2", user.getPassword());
        assertEquals("Alice", user.getDisplayName());
        assertEquals("https://example.com/avatar.png", user.getAvatarUrl());
    }

    @Test
    void userDetailsMethods_returnExpectedDefaults() {
        AppUser user = new AppUser("bob", "bob@example.com", "secret");

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.iterator().next().getAuthority());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    void noArgConstructor_isAvailableForJpa() throws Exception {
        Constructor<AppUser> constructor = AppUser.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        AppUser user = constructor.newInstance();

        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getEmail());
        assertNull(user.getPassword());
        assertNull(user.getDisplayName());
        assertNull(user.getAvatarUrl());
        assertFalse(user.getAuthorities().isEmpty());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
