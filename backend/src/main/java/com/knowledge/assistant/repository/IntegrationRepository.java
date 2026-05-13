package com.knowledge.assistant.repository;

import com.knowledge.assistant.model.Integration;
import com.knowledge.assistant.model.IntegrationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationRepository extends JpaRepository<Integration, UUID> {

    Optional<Integration> findByUserIdAndIntegrationType(UUID userId, IntegrationType integrationType);

    List<Integration> findByUserId(UUID userId);
}
