package com.threadly.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncodingTest {

    @Test
    void bcryptRoundTrip() {
        var encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("secret123");
        assertTrue(encoder.matches("secret123", hash));
        assertFalse(encoder.matches("wrong", hash));
    }
}
