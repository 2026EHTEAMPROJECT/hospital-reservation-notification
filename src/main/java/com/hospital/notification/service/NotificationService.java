package com.hospital.notification.service;

import com.hospital.notification.client.DoctorClient;
import com.hospital.notification.client.UserClient;
import com.hospital.notification.dto.client.DoctorResponse;
import com.hospital.notification.dto.client.UserResponse;
import com.hospital.notification.dto.BookingMessage;
import com.hospital.notification.dto.PaymentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String UNKNOWN = "UNKNOWN";
    private static final String DEFAULT_PATIENT_NAME = "환자";
    private static final String DEFAULT_DOCTOR_NAME = "담당의";

    private final LiveNotificationService liveNotificationService;
    private final UserClient userClient;
    private final DoctorClient doctorClient;

    public void handleBookingNotification(BookingMessage message) {

        UserResponse user = null;
        DoctorResponse doctor = null;

        try {
            user = userClient.getUser(message.patientId());
        } catch (Exception e) {
            log.warn("사용자 조회 실패: {}", e.getMessage());
        }

        try {
            doctor = doctorClient.getDoctor(message.doctorId());
        } catch (Exception e) {
            log.warn("의사 조회 실패: {}", e.getMessage());
        }

        String text = buildBookingMessage(message, user, doctor);

        log.info(
                "[알림-예약] patientId={}, status={}",
                message.patientId(),
                message.status()
        );

        simulateSend(
                user != null ? user.name() : "환자",
                text
        );

        liveNotificationService.publish(
                String.valueOf(message.patientId()),
                "BOOKING",
                text
        );
    }

    public void handlePaymentNotification(PaymentMessage message) {

        String text = buildPaymentMessage(message);

        log.info(
                "[알림-결제] patientId={}, status={}",
                message.patientId(),
                message.status()
        );

        simulateSend(message.patientName(), text);

        liveNotificationService.publish(
                String.valueOf(message.patientId()),
                "PAYMENT",
                text
        );
    }

    private String buildBookingMessage(
            BookingMessage message,
            UserResponse user,
            DoctorResponse doctor
    ) {

        String status =
                message.status() != null
                        ? message.status()
                        : UNKNOWN;

        String name =
                user != null
                        ? user.name()
                        : DEFAULT_PATIENT_NAME;

        String doctorName =
                doctor != null
                        ? doctor.name()
                        : DEFAULT_DOCTOR_NAME;

        return switch (status) {

            case "WAITING" ->
                    "%s님, %s 예약이 접수되었습니다. (예약번호: %d)"
                            .formatted(
                                    name,
                                    message.reservationTime(),
                                    message.reservationId()
                            );

            case "CONFIRMED" ->
                    "%s님, %s 예약이 확정되었습니다. 담당의: %s"
                            .formatted(
                                    name,
                                    message.reservationTime(),
                                    doctorName
                            );

            case "CANCELED" ->
                    "%s님, 예약(번호: %d)이 취소되었습니다."
                            .formatted(
                                    name,
                                    message.reservationId()
                            );

            case "PAYMENT_FAILED" ->
                    "%s님, 결제 실패로 예약(번호: %d)이 취소되었습니다."
                            .formatted(
                                    name,
                                    message.reservationId()
                            );

            default ->
                    "%s님, 예약 상태가 변경되었습니다: %s"
                            .formatted(
                                    name,
                                    status
                            );
        };
    }

    private String buildPaymentMessage(PaymentMessage message) {

        String status =
                message.status() != null
                        ? message.status()
                        : UNKNOWN;

        String name =
                message.patientName() != null
                        ? message.patientName()
                        : DEFAULT_PATIENT_NAME;

        return switch (status) {

            case "SUCCESS" ->
                    "%s님, %d원 결제가 완료되었습니다. (결제번호: %d)"
                            .formatted(
                                    name,
                                    message.amount() != null
                                            ? message.amount()
                                            : 0,
                                    message.paymentId()
                            );

            case "FAILED" ->
                    "%s님, 결제에 실패하였습니다. 다시 시도해주세요."
                            .formatted(name);

            default ->
                    "%s님, 결제 상태가 변경되었습니다: %s"
                            .formatted(
                                    name,
                                    status
                            );
        };
    }

    private void simulateSend(String recipient, String message) {
        log.info(
                "[발송 시뮬레이션] 수신자: {} | 길이: {}자",
                maskName(recipient),
                message.length()
        );
    }

    private String maskName(String name) {
        if (name == null || name.length() < 2) {
            return "**";
        }
        return name.charAt(0)
                + "*".repeat(name.length() - 1);
    }
}