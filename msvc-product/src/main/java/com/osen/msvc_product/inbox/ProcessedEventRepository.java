package com.osen.msvc_product.inbox;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
