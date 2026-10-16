package com.spaceora.auth.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.spaceora.user.entity.User;

public interface JwtService {
    String generateToken(User user);
    DecodedJWT validateToken(String token);
    String extractEmail(DecodedJWT decoded);
    Long extractUserId(DecodedJWT decoded);
    boolean isTokenValid(String token);
}
