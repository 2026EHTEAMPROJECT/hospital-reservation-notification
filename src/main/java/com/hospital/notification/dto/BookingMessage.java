package com.hospital.notification.dto;

public record BookingMessage(
        Long reservationId,
        Long patientId,
        Long doctorId,
        String status,
        String reservationTime
) {}
