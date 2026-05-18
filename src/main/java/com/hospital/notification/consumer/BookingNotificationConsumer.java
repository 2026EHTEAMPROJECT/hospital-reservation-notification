package com.hospital.notification.consumer;

import com.hospital.notification.config.RabbitConfig;
import com.hospital.notification.dto.BookingMessage;
import com.hospital.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.BOOKING_QUEUE)
    public void receive(BookingMessage message) {
        log.info("[예약 메시지 수신] reservationId={}, status={}", message.reservationId(), message.status());
        notificationService.handleBookingNotification(message);
    }
}
