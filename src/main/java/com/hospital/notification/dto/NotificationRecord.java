package com.hospital.notification.dto;

public record NotificationRecord(
        String type,
        String message,
        String receivedAt
) {}
