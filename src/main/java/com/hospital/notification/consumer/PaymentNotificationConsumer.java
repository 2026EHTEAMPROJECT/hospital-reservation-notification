package com.hospital.notification.consumer;

import com.hospital.notification.config.RabbitConfig;
import com.hospital.notification.dto.PaymentMessage;
import com.hospital.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentNotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.PAYMENT_QUEUE)
    public void receive(PaymentMessage message) {
        log.info("[결제 메시지 수신] paymentId={}, status={}", message.paymentId(), message.status());
        notificationService.handlePaymentNotification(message);
    }
}
