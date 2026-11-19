package com.osen.msvc_payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.osen.msvc_payment.outbox.OutboxPublisher;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic paymentEventsTopic() {
        return TopicBuilder.name(OutboxPublisher.PAYMENT_EVENTS_TOPIC).partitions(3).replicas(1).build();
    }

    // stock-events.DLT lo declara tambien msvc-order; declararlo igual aqui es inofensivo
    @Bean
    NewTopic stockEventsDlt() {
        return TopicBuilder.name("stock-events.DLT").partitions(1).replicas(1).build();
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    /**
     * Mismo criterio que en order y product: 4 reintentos con backoff exponencial
     * (1s a 8s), JSON invalido directo al DLT, y los agotados van a stock-events.DLT
     * sin bloquear la particion.
     */
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
