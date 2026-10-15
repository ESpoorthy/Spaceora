package com.spaceora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long expirySeconds;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getExpirySeconds() { return expirySeconds; }
    public void setExpirySeconds(long expirySeconds) { this.expirySeconds = expirySeconds; }
}
