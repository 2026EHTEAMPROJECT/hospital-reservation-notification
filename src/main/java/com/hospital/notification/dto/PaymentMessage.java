package com.hospital.notification.dto;

public record PaymentMessage(
        Long paymentId,
        Long reservationId,
        Long patientId,
        String status,
        Integer amount,
        String patientName
) {}
