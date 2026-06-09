package com.knowledge.assistant.controller;

import com.knowledge.assistant.model.User;
import com.knowledge.assistant.repository.UserRepository;
import com.knowledge.assistant.service.IntegrationService;
import com.knowledge.assistant.service.SlackService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;
    private final SlackService slackService;
    private final UserRepository userRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    public IntegrationController(IntegrationService integrationService,
                                 SlackService slackService,
                                 UserRepository userRepository) {
        this.integrationService = integrationService;
        this.slackService = slackService;
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getStatus(Authentication auth) {
        return ResponseEntity.ok(integrationService.getStatus(auth.getName()));
    }

    @GetMapping("/slack/connect")
    public ResponseEntity<Map<String, String>> getSlackConnectUrl(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        String url = slackService.buildOAuthUrl(user.getId().toString());
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/slack/callback")
    public void slackCallback(@RequestParam String code,
                              @RequestParam String state,
                              HttpServletResponse response) throws IOException {
        try {
            UUID userId = UUID.fromString(state);
            Map<String, Object> tokenData = slackService.exchangeCodeForToken(code);
            integrationService.saveSlackIntegration(userId, tokenData);
        } catch (Exception e) {
            // Log and redirect back — user will see Slack not connected
        }
        response.sendRedirect(frontendUrl + "/connect");
    }
}
