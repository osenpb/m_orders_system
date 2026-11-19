package com.osen.msvc_order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.osen.msvc_order.outbox.OutboxPublisher;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(OutboxPublisher.ORDER_EVENTS_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
