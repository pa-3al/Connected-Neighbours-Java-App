package com.app.domain.model;

import java.time.LocalDateTime;

public record Neighbor(
    String id,
    String name,
    String email,
    String address,
    LocalDateTime joinedAt,
    boolean isActive
) {
    public static Neighbor create(String id, String name, String email, String address) {
        return new Neighbor(id, name, email, address, LocalDateTime.now(), true);
    }
}
