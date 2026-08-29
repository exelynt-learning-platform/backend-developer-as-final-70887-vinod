package com.example.booking;

import com.example.booking.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final JwtService jwtService = new JwtService("test-secret-that-is-at-least-32-bytes-long-123456", 3600000);

    @Test
    void generatesAndValidatesToken() {
        String token = jwtService.generateToken("user", "USER");
        assertEquals("user", jwtService.extractUsername(token));
        assertTrue(jwtService.isValid(token, "user"));
        assertFalse(jwtService.isValid(token, "admin"));
    }
}
