package com.osen.msvc_product.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.osen.msvc_product.outbox.OutboxPublisher;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic stockEventsTopic() {
        return TopicBuilder.name(OutboxPublisher.STOCK_EVENTS_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
