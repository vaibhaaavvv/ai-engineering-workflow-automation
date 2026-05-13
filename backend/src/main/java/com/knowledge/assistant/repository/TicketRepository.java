package com.knowledge.assistant.repository;

import com.knowledge.assistant.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Ticket> findByIdAndUserId(UUID id, UUID userId);

    Optional<Ticket> findByTicketNumberAndUserId(String ticketNumber, UUID userId);

    @Query("SELECT COALESCE(MAX(t.sequenceNumber), 0) FROM Ticket t WHERE t.userId = :userId")
    int findMaxSequenceNumberByUserId(@Param("userId") UUID userId);
}
