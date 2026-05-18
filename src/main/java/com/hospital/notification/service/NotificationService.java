package com.hospital.notification.service;

import com.hospital.notification.dto.BookingMessage;
import com.hospital.notification.dto.PaymentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final LiveNotificationService liveNotificationService;

    public void handleBookingNotification(BookingMessage message) {
        String text = buildBookingMessage(message);
        log.info("[알림-예약] patientId={}, status={}", message.patientId(), message.status());
        simulateSend(message.patientName(), text);
        liveNotificationService.publish(String.valueOf(message.patientId()), "BOOKING", text);
    }

    public void handlePaymentNotification(PaymentMessage message) {
        String text = buildPaymentMessage(message);
        log.info("[알림-결제] patientId={}, status={}", message.patientId(), message.status());
        simulateSend(message.patientName(), text);
        liveNotificationService.publish(String.valueOf(message.patientId()), "PAYMENT", text);
    }

    private String buildBookingMessage(BookingMessage message) {
        // null 방어
        String status = message.status() != null ? message.status() : "UNKNOWN";
        String name   = message.patientName() != null ? message.patientName() : "환자";
        return switch (status) {
            case "WAITING"        -> "%s님, %s 예약이 접수되었습니다. (예약번호: %d)".formatted(
                    name, message.reservationTime(), message.reservationId());
            case "CONFIRMED"      -> "%s님, %s 예약이 확정되었습니다. 담당의: %s".formatted(
                    name, message.reservationTime(),
                    message.doctorName() != null ? message.doctorName() : "담당의");
            case "CANCELED"       -> "%s님, 예약(번호: %d)이 취소되었습니다.".formatted(
                    name, message.reservationId());
            case "PAYMENT_FAILED" -> "%s님, 결제 실패로 예약(번호: %d)이 취소되었습니다.".formatted(
                    name, message.reservationId());
            default               -> "%s님, 예약 상태가 변경되었습니다: %s".formatted(name, status);
        };
    }

    private String buildPaymentMessage(PaymentMessage message) {
        // null 방어
        String status = message.status() != null ? message.status() : "UNKNOWN";
        String name   = message.patientName() != null ? message.patientName() : "환자";
        return switch (status) {
            case "SUCCESS" -> "%s님, %d원 결제가 완료되었습니다. (결제번호: %d)".formatted(
                    name,
                    message.amount() != null ? message.amount() : 0,
                    message.paymentId());
            case "FAILED"  -> "%s님, 결제에 실패하였습니다. 다시 시도해주세요.".formatted(name);
            default        -> "%s님, 결제 상태가 변경되었습니다: %s".formatted(name, status);
        };
    }

    // 실제 카카오/SMS/이메일 발송 시뮬레이션
    private void simulateSend(String recipient, String message) {
        log.info("[발송 시뮬레이션] 수신자: {} | 길이: {}자", maskName(recipient), message.length());
    }

    private String maskName(String name) {
        if (name == null || name.length() < 2) return "**";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
