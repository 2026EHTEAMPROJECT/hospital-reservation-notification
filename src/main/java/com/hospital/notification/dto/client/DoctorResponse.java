package com.hospital.notification.dto.client;

public record DoctorResponse(
        Long id,
        String name,
        String department
) {}