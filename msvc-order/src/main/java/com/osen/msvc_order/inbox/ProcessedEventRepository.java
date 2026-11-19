package com.osen.msvc_order.inbox;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
