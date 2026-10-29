package com.epam.edp.demo.entity.auth;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Role {

    CUSTOMER("ROLE_CUSTOMER"),
    TRAVEL_AGENT("ROLE_TRAVEL_AGENT"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public static Role fromAuthority(String authority) {
        return Arrays.stream(Role.values())
                .filter(role -> role.getAuthority().equals(authority))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No Role found for authority: " + authority));
    }
}

