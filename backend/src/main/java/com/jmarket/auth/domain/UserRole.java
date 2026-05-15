package com.jmarket.auth.domain;

public enum UserRole {
    USER,
    ADMIN,
    SUPER_ADMIN;

    public boolean canAccessAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    public boolean canManageRoles() {
        return this == SUPER_ADMIN;
    }
}
