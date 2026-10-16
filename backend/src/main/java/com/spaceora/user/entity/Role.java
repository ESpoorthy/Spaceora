package com.spaceora.user.entity;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    CUSTOMER, OWNER, ADMIN;

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
