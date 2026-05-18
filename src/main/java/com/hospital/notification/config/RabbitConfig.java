package com.hospital.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
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

    @Bean
    DirectExchange hospitalExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue bookingQueue() {
        return new Queue(BOOKING_QUEUE, true);
    }

    @Bean
    Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
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
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
