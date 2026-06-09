package com.knowledge.assistant.service;

import com.knowledge.assistant.model.Integration;
import com.knowledge.assistant.model.IntegrationType;
import com.knowledge.assistant.model.User;
import com.knowledge.assistant.repository.IntegrationRepository;
import com.knowledge.assistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

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

        return Map.of("slack", slackConnected);
    }

    public Optional<Integration> getIntegration(String userEmail, IntegrationType type) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return integrationRepository.findByUserIdAndIntegrationType(user.getId(), type);
    }

    public Optional<Integration> getBySlackTeamId(String teamId) {
        return integrationRepository.findBySlackTeamId(teamId);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public void saveSlackIntegration(UUID userId, Map<String, Object> tokenData) {
        Integration integration = integrationRepository
                .findByUserIdAndIntegrationType(userId, IntegrationType.SLACK)
                .orElse(new Integration());

        integration.setUserId(userId);
        integration.setIntegrationType(IntegrationType.SLACK);
        integration.setAccessToken((String) tokenData.get("access_token"));

        Map<String, Object> team = (Map<String, Object>) tokenData.get("team");
        if (team != null) {
            integration.setSlackTeamId((String) team.get("id"));
        }

        integrationRepository.save(integration);
    }
}
