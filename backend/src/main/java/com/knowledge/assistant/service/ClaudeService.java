package com.knowledge.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.assistant.dto.TicketAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeService.class);

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";

    private static final String SYSTEM_PROMPT = """
        You are a ticket triage assistant for an engineering team.
        Analyze Slack messages and decide if they describe actionable engineering work.

        Return ONLY valid JSON — no markdown, no explanation, no code blocks.

        If actionable:
        {"action":"PROPOSE","title":"...","type":"BUG|FEATURE|TASK|CHORE","priority":"LOW|MEDIUM|HIGH|CRITICAL","description":"...","assignee":"..."}

        If not actionable:
        {"action":"NO_ACTION"}

        Rules:
        - BUG: something broken or not working as expected
        - FEATURE: new functionality being requested
        - TASK: technical work like refactoring, config changes, upgrades
        - CHORE: documentation, cleanup, routine maintenance
        - Only PROPOSE for clearly actionable engineering work
        - Casual chat, questions, reactions, greetings → NO_ACTION
        - Keep title under 60 characters
        - Description: 1-2 sentences summarising the issue
        - assignee: extract a person's first name if the message mentions who should fix it
          (e.g. "ask Vaibhav", "Vaibhav should fix this", "@vaibhav" → "Vaibhav")
          If no person is mentioned, use "admin"
        """;

    @Value("${anthropic.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public TicketAnalysis analyzeMessage(String messageText) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "claude-sonnet-4-6");
            body.put("max_tokens", 300);
            body.put("system", SYSTEM_PROMPT);
            body.put("messages", List.of(Map.of("role", "user", "content", messageText)));

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    CLAUDE_API_URL, new HttpEntity<>(body, headers), Map.class);

            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
            String text = ((String) content.get(0).get("text")).trim();

            // Strip markdown code fences if Claude adds them
            if (text.startsWith("```")) {
                text = text.replaceAll("(?s)```[a-z]*\\n?", "").replaceAll("```", "").trim();
            }

            return objectMapper.readValue(text, TicketAnalysis.class);

        } catch (Exception e) {
            log.error("Claude analysis failed: {}", e.getMessage());
            TicketAnalysis noAction = new TicketAnalysis();
            noAction.setAction("NO_ACTION");
            return noAction;
        }
    }
}
