package com.osen.msvc_product.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Reintentos con backoff exponencial y Dead Letter Topic para los eventos que
 * product consume (order-events). Spring Boot aplica este DefaultErrorHandler
 * a todos los listeners.
 *
 * - Fallo transitorio (BD caida, timeout): 4 reintentos a 1s, 2s, 4s, 8s.
 *   Es seguro reintentar porque el consumidor es idempotente (inbox).
 * - Mensaje irrecuperable (JSON invalido): va directo al DLT, sin reintentos.
 * - Agotados los reintentos: el registro se publica en order-events.DLT y el
 *   consumo continua, asi un mensaje venenoso no bloquea la particion.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    public static final String ORDER_EVENTS_DLT = "order-events.DLT";

    public static final String PAYMENT_EVENTS_DLT = "payment-events.DLT";

    @Bean
    NewTopic orderEventsDlt() {
        return TopicBuilder.name(ORDER_EVENTS_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic paymentEventsDlt() {
        return TopicBuilder.name(PAYMENT_EVENTS_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> template) {
        // partition -1: que el productor elija, el DLT no tiene que coincidir en particiones
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(4);
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(JsonProcessingException.class);
        return handler;
    }
}
