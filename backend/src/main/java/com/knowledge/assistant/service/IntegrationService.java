package com.knowledge.assistant.service;

import com.knowledge.assistant.model.Integration;
import com.knowledge.assistant.model.IntegrationType;
import com.knowledge.assistant.model.User;
import com.knowledge.assistant.repository.IntegrationRepository;
import com.knowledge.assistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class IntegrationService {

    private final IntegrationRepository integrationRepository;
    private final UserRepository userRepository;

    @Autowired
    public IntegrationService(IntegrationRepository integrationRepository, UserRepository userRepository) {
        this.integrationRepository = integrationRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Boolean> getStatus(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        boolean slackConnected = integrationRepository
                .findByUserIdAndIntegrationType(user.getId(), IntegrationType.SLACK)
                .isPresent();

        boolean githubConnected = integrationRepository
                .findByUserIdAndIntegrationType(user.getId(), IntegrationType.GITHUB)
                .isPresent();

        return Map.of("slack", slackConnected, "github", githubConnected);
    }

    public Optional<Integration> getIntegration(String userEmail, IntegrationType type) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return integrationRepository.findByUserIdAndIntegrationType(user.getId(), type);
    }
}
