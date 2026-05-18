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
        log.info("[알림-예약] {}", text);
        simulateSend(message.patientName(), text);
        liveNotificationService.publish("BOOKING", text);
    }

    public void handlePaymentNotification(PaymentMessage message) {
        String text = buildPaymentMessage(message);
        log.info("[알림-결제] {}", text);
        simulateSend(message.patientName(), text);
        liveNotificationService.publish("PAYMENT", text);
    }

    private String buildBookingMessage(BookingMessage message) {
        return switch (message.status()) {
            case "WAITING"        -> "%s님, %s 예약이 접수되었습니다. (예약번호: %d)".formatted(
                    message.patientName(), message.reservationTime(), message.reservationId());
            case "CONFIRMED"      -> "%s님, %s 예약이 확정되었습니다. 담당의: %s".formatted(
                    message.patientName(), message.reservationTime(), message.doctorName());
            case "CANCELED"       -> "%s님, 예약(번호: %d)이 취소되었습니다.".formatted(
                    message.patientName(), message.reservationId());
            case "PAYMENT_FAILED" -> "%s님, 결제 실패로 예약(번호: %d)이 취소되었습니다.".formatted(
                    message.patientName(), message.reservationId());
            default               -> "%s님, 예약 상태가 변경되었습니다: %s".formatted(
                    message.patientName(), message.status());
        };
    }

    private String buildPaymentMessage(PaymentMessage message) {
        return switch (message.status()) {
            case "SUCCESS" -> "%s님, %d원 결제가 완료되었습니다. (결제번호: %d)".formatted(
                    message.patientName(), message.amount(), message.paymentId());
            case "FAILED"  -> "%s님, 결제에 실패하였습니다. 다시 시도해주세요.".formatted(
                    message.patientName());
            default        -> "%s님, 결제 상태가 변경되었습니다: %s".formatted(
                    message.patientName(), message.status());
        };
    }

    // 실제 카카오/SMS/이메일 발송 시뮬레이션
    private void simulateSend(String recipient, String message) {
        log.info("[발송 시뮬레이션] 수신자: {} | 내용: {}", recipient, message);
    }
}
