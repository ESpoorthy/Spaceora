package com.spaceora.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.spaceora.config.JwtProperties;
import com.spaceora.user.entity.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;

    public JwtServiceImpl(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public String generateToken(User user) {
        return JWT.create()
                .withSubject(user.getEmail())
                .withClaim("userId", user.getId())
                .withClaim("role", user.getRole().name())
                .withIssuedAt(new Date())
                .withExpiresAt(Date.from(Instant.now().plusSeconds(jwtProperties.getExpirySeconds())))
                .sign(Algorithm.HMAC256(jwtProperties.getSecret()));
    }

    @Override
    public DecodedJWT validateToken(String token) {
        return JWT.require(Algorithm.HMAC256(jwtProperties.getSecret()))
                .build()
                .verify(token);
    }

    @Override
    public String extractEmail(DecodedJWT decoded) {
        return decoded.getSubject();
    }

    @Override
    public Long extractUserId(DecodedJWT decoded) {
        return decoded.getClaim("userId").asLong();
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }
}
