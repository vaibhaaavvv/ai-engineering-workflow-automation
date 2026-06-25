package com.knowledge.assistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.assistant.service.SlackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slack")
public class SlackController {

    private static final Logger log = LoggerFactory.getLogger(SlackController.class);

    private final SlackService slackService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SlackController(SlackService slackService) {
        this.slackService = slackService;
    }

    @PostMapping("/events")
    public ResponseEntity<?> handleEvents(@RequestBody Map<String, Object> payload) {
        String type = (String) payload.get("type");

        if ("url_verification".equals(type)) {
            return ResponseEntity.ok(Map.of("challenge", payload.get("challenge")));
        }

        if ("event_callback".equals(type)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = (Map<String, Object>) payload.get("event");
            String teamId = (String) payload.get("team_id");
            String eventType = (String) event.get("type");
            // Process asynchronously so we return 200 immediately (Slack requires < 3s)
            if ("app_mention".equals(eventType)) {
                slackService.processCommand(event, teamId);
            } else {
                slackService.processMessage(event, teamId);
            }
        }

        return ResponseEntity.ok(Map.of());
    }

    @PostMapping(value = "/interactions", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> handleInteractions(@RequestParam("payload") String payloadJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> actions = (List<Map<String, Object>>) payload.get("actions");
            if (actions == null || actions.isEmpty()) return ResponseEntity.ok(Map.of());

            Map<String, Object> action = actions.get(0);
            String actionId = (String) action.get("action_id");
            String value = (String) action.get("value");

            if ("create_ticket".equals(actionId)) {
                slackService.handleCreateTicket(payload, value);
            } else if ("dismiss_ticket".equals(actionId)) {
                slackService.handleDismiss(payload);
            }

        } catch (Exception e) {
            log.error("Failed to handle Slack interaction: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of());
    }
}
