package com.dashboard.auth;

import com.dashboard.auth.util.JwtUtil;
import com.dashboard.config.JwtConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void testTokenGenerationAndValidation() {
        JwtConfig config = new JwtConfig();
        config.setSecret("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==");
        config.setExpiration(604800000L);

        JwtUtil jwtUtil = new JwtUtil(config);
        String token = jwtUtil.generateToken(1L, "test@example.com");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("test@example.com", jwtUtil.extractEmail(token));
        assertEquals(1L, jwtUtil.extractUserId(token));
        assertFalse(jwtUtil.isNearExpiry(token));
    }

    @Test
    void testInvalidToken() {
        JwtConfig config = new JwtConfig();
        config.setSecret("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==");
        config.setExpiration(604800000L);

        JwtUtil jwtUtil = new JwtUtil(config);
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }
}