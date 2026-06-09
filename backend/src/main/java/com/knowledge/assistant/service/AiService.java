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
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

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

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public TicketAnalysis analyzeMessage(String messageText) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "gpt-4o-mini");
            body.put("max_tokens", 300);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", messageText)
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    OPENAI_API_URL, new HttpEntity<>(body, headers), Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String text = ((String) message.get("content")).trim();

            // Strip markdown code fences if the model wraps the JSON
            if (text.startsWith("```")) {
                text = text.replaceAll("(?s)```[a-z]*\\n?", "").replaceAll("```", "").trim();
            }

            return objectMapper.readValue(text, TicketAnalysis.class);

        } catch (Exception e) {
            log.error("AI analysis failed: {}", e.getMessage());
            TicketAnalysis noAction = new TicketAnalysis();
            noAction.setAction("NO_ACTION");
            return noAction;
        }
    }
}
