package com.osen.msvc_order.config;

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
 * order consume (stock-events). Spring Boot aplica este DefaultErrorHandler a
 * todos los listeners.
 *
 * - Fallo transitorio: 4 reintentos a 1s, 2s, 4s, 8s (seguro: consumidor idempotente).
 * - JSON invalido: directo al DLT, sin reintentos.
 * - Agotados los reintentos: el registro va a stock-events.DLT y el consumo sigue.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    public static final String STOCK_EVENTS_DLT = "stock-events.DLT";

    @Bean
    NewTopic stockEventsDlt() {
        return TopicBuilder.name(STOCK_EVENTS_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> template) {
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
