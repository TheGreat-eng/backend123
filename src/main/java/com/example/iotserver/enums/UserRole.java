// src/main/java/com/example/iotserver/enums/UserRole.java

package com.example.iotserver.enums;

import org.springframework.security.core.GrantedAuthority;

public enum UserRole implements GrantedAuthority {
    ADMIN,
    FARMER,
    VIEWER;

    @Override
    public String getAuthority() {
        return name(); // Trả về tên của enum (VD: "ADMIN")
    }
}