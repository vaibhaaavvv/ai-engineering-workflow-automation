package com.knowledge.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.assistant.dto.TicketAnalysis;
import com.knowledge.assistant.dto.TicketResponse;
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
        Analyze a Slack message and decide what action to take.

        Return ONLY valid JSON — no markdown, no explanation, no code blocks.

        Possible responses:

        If actionable and not already covered by an existing ticket:
        {"action":"PROPOSE","title":"...","type":"BUG|FEATURE|TASK|CHORE","priority":"LOW|MEDIUM|HIGH|CRITICAL","description":"...","assignee":"..."}

        If the message describes something already covered by an existing open ticket:
        {"action":"DUPLICATE"}

        If the message is not actionable engineering work:
        {"action":"NO_ACTION"}

        --- Type Classification (judge by INTENT, not keywords) ---
        BUG     — something is broken, not working, crashing, erroring, or behaving wrongly.
                  The word "bug" never needs to appear.
                  e.g. "the login page won't load", "users are getting 500 errors", "the export looks corrupted"
        FEATURE — a new capability or improvement that doesn't exist yet.
                  e.g. "can we add email notifications?", "it would be great to filter by date"
        TASK    — technical work with no direct user-facing output: refactoring, config changes, upgrades, migrations.
                  e.g. "we should upgrade the DB version", "let's move this to a separate service"
        CHORE   — documentation, cleanup, or routine non-technical maintenance.
                  e.g. "update the README", "remove old feature flags"

        --- Priority ---
        CRITICAL — production down, data loss, or security vulnerability
        HIGH     — major feature broken, many users affected
        MEDIUM   — partial functionality broken, or an impactful feature request
        LOW      — minor issue, cosmetic problem, or low-traffic path

        --- Other rules ---
        - assignee: extract the first name if the message mentions who should fix it
          ("ask Vaibhav", "Vaibhav should fix this", "@vaibhav" → "Vaibhav"). If nobody is mentioned → "admin"
        - title: under 60 characters, start with an action verb when possible
        - description: 2-3 sentences covering what is broken or needed, what impact it has,
          and any relevant context from the message (who reported it, which screen/flow is affected)
        - Messages often mix casual language with a real issue embedded inside them.
          Always scan the ENTIRE message. If any part describes an actionable engineering
          problem, PROPOSE based on that part — ignore the surrounding small talk.
          e.g. "Yeah sounds good. Also the export button is broken" → PROPOSE BUG
          e.g. "Haha totally. Btw can we add dark mode?" → PROPOSE FEATURE
        - Only return NO_ACTION when the ENTIRE message is purely social with zero
          engineering content (greetings, reactions, emoji-only, scheduling chat, etc.)
        """;

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public TicketAnalysis analyzeMessage(String messageText, List<TicketResponse> openTickets) {
        try {
            String userContent = buildUserContent(messageText, openTickets);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "gpt-4o-mini");
            body.put("max_tokens", 400);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userContent)
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    OPENAI_API_URL, new HttpEntity<>(body, headers), Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String text = ((String) message.get("content")).trim();

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

    private String buildUserContent(String messageText, List<TicketResponse> openTickets) {
        StringBuilder sb = new StringBuilder();
        sb.append("Slack message:\n").append(messageText);

        if (openTickets != null && !openTickets.isEmpty()) {
            sb.append("\n\nExisting open tickets (check for duplicates before proposing):\n");
            // Cap at 30 tickets to keep the prompt reasonable
            openTickets.stream().limit(30).forEach(t ->
                sb.append(String.format("- %s [%s] %s — Assignee: %s%n",
                        t.getTicketNumber(), t.getType(), t.getTitle(), t.getAssignee()))
            );
        }

        return sb.toString();
    }
}
