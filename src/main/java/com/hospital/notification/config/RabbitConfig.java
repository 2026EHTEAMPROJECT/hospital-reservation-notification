package com.hospital.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfig {

    public static final String EXCHANGE = "hospital.exchange";

    public static final String BOOKING_QUEUE = "booking.notification.queue";
    public static final String BOOKING_KEY   = "booking.notification";

    public static final String PAYMENT_QUEUE = "payment.notification.queue";
    public static final String PAYMENT_KEY   = "payment.notification";

    public static final String DLX = "hospital.dlx";
    public static final String BOOKING_DLQ = "booking.notification.dlq";
    public static final String PAYMENT_DLQ = "payment.notification.dlq";

    @Bean
    DirectExchange hospitalExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue bookingQueue() {
        return QueueBuilder.durable(BOOKING_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", BOOKING_KEY + ".dlq")
                .build();
    }

    @Bean
    Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", PAYMENT_KEY + ".dlq")
                .build();
    }

    @Bean
    Queue bookingDlq() {
        return QueueBuilder.durable(BOOKING_DLQ).build();
    }

    @Bean
    Queue paymentDlq() {
        return QueueBuilder.durable(PAYMENT_DLQ).build();
    }

    @Bean
    Binding bookingBinding(Queue bookingQueue, DirectExchange hospitalExchange) {
        return BindingBuilder.bind(bookingQueue).to(hospitalExchange).with(BOOKING_KEY);
    }

    @Bean
    Binding paymentBinding(Queue paymentQueue, DirectExchange hospitalExchange) {
        return BindingBuilder.bind(paymentQueue).to(hospitalExchange).with(PAYMENT_KEY);
    }

    @Bean
    Binding bookingDlqBinding(Queue bookingDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(bookingDlq).to(deadLetterExchange).with(BOOKING_KEY + ".dlq");
    }

    @Bean
    Binding paymentDlqBinding(Queue paymentDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(paymentDlq).to(deadLetterExchange).with(PAYMENT_KEY + ".dlq");
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
