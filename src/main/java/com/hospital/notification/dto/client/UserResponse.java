package com.hospital.notification.dto.client;

public record UserResponse(
        Long id,
        String email,
        String name,
        String role
) {}