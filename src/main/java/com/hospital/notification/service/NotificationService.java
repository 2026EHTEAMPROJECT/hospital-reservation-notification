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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String UNKNOWN = "UNKNOWN";
    private static final String DEFAULT_PATIENT_NAME = "환자";
    private static final String DEFAULT_DOCTOR_NAME = "담당의";

    // 예약 시각 문구 처리용 기본값.
    // 취소/확정 이벤트는 booking-service 의 publishStatusChangeNotification 이
    // reservationTime 을 보내지 않아 null 로 들어온다(예약 생성 이벤트만 시각 포함).
    private static final String DEFAULT_RESERVATION_TIME = "예약하신 시간";

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
                (message.patientName() != null && !message.patientName().isBlank())
                        ? message.patientName()
                        : (user != null ? user.name() : DEFAULT_PATIENT_NAME);

        String doctorName =
                (message.doctorName() != null && !message.doctorName().isBlank())
                        ? message.doctorName()
                        : (doctor != null ? doctor.name() : DEFAULT_DOCTOR_NAME);

        // 예약 시각을 "H시 m분" 형식으로 변환한다.
        // reservationTime 은 booking-service 에서 LocalDateTime.toString()(ISO-8601)으로 전달된다.
        String reservationTime = formatReservationTime(message.reservationTime());

        return switch (status) {

            // 예약 생성(WAITING): 예약자 이름은 patientId 로 user-service 에서 조회한 값을 사용.
            case "WAITING" ->
                    "%s님 %s 예약이되었습니다"
                            .formatted(
                                    name,
                                    reservationTime
                            );

            // 예약 확정(CONFIRMED): 담당의 이름은 doctorId 로 user-service 에서 조회한 값을 사용.
            // 취소/확정 이벤트는 reservationTime 을 전달받지 못해 기본 문구로 대체될 수 있음.
            case "CONFIRMED" ->
                    "%s님, %s 예약이 확정되었습니다. 담당의: %s"
                            .formatted(
                                    name,
                                    reservationTime,
                                    doctorName
                            );

            case "CANCELED" ->
                    "%s님, %s 예약이 취소되었습니다."
                            .formatted(
                                    name,
                                    reservationTime
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

    // reservationTime(ISO-8601 문자열)을 "H시 m분" 형식으로 변환한다.
    // 값이 없거나(취소/확정 이벤트) 파싱할 수 없는 경우 기본 문구를 반환한다.
    private String formatReservationTime(String reservationTime) {
        if (reservationTime == null || reservationTime.isBlank()) {
            return DEFAULT_RESERVATION_TIME;
        }
        try {
            LocalDateTime dateTime =
                    LocalDateTime.parse(reservationTime);
            return dateTime.format(
                    DateTimeFormatter.ofPattern("H시 m분")
            );
        } catch (DateTimeParseException e) {
            log.warn(
                    "예약 시각 파싱 실패: {}",
                    reservationTime
            );
            return DEFAULT_RESERVATION_TIME;
        }
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

            case "REFUNDED" ->
                    "%s님, 예약이 취소되어 %d원이 환불되었습니다. (결제번호: %d)"
                            .formatted(
                                    name,
                                    message.amount() != null
                                            ? message.amount()
                                            : 0,
                                    message.paymentId()
                            );

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