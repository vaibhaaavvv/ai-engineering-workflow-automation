package com.knowledge.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.assistant.dto.TicketAnalysis;
import com.knowledge.assistant.dto.TicketResponse;
import com.knowledge.assistant.model.Integration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class SlackService {

    private static final Logger log = LoggerFactory.getLogger(SlackService.class);

    @Value("${slack.client.id}")
    private String clientId;

    @Value("${slack.client.secret}")
    private String clientSecret;

    @Value("${slack.redirect.uri}")
    private String redirectUri;

    private final AiService aiService;
    private final IntegrationService integrationService;
    private final TicketService ticketService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SlackService(AiService aiService,
                        IntegrationService integrationService,
                        TicketService ticketService) {
        this.aiService = aiService;
        this.integrationService = integrationService;
        this.ticketService = ticketService;
    }

    public String buildOAuthUrl(String userId) {
        return "https://slack.com/oauth/v2/authorize" +
                "?client_id=" + clientId +
                "&scope=channels:history,groups:history,chat:write" +
                "&redirect_uri=" + redirectUri +
                "&state=" + userId;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://slack.com/api/oauth.v2.access",
                new HttpEntity<>(body, headers),
                Map.class);

        return response.getBody();
    }

    @Async
    @SuppressWarnings("unchecked")
    public void processMessage(Map<String, Object> event, String teamId) {
        try {
            // Skip bot messages and message edits/deletes
            if (event.get("bot_id") != null || event.get("subtype") != null) return;

            String text = (String) event.get("text");
            if (text == null || text.isBlank()) return;

            String channel = (String) event.get("channel");
            String ts = (String) event.get("ts");

            Optional<Integration> integrationOpt = integrationService.getBySlackTeamId(teamId);
            if (integrationOpt.isEmpty()) return;

            Integration integration = integrationOpt.get();
            String botToken = integration.getAccessToken();

            List<com.knowledge.assistant.dto.TicketResponse> openTickets =
                    ticketService.getOpenTicketsByUserId(integration.getUserId());

            TicketAnalysis analysis = aiService.analyzeMessage(text, openTickets);
            if (!"PROPOSE".equals(analysis.getAction())) return;

            postProposal(channel, ts, botToken, analysis, integration.getUserId());

        } catch (Exception e) {
            log.error("Failed to process Slack message: {}", e.getMessage());
        }
    }

    private void postProposal(String channel, String threadTs, String botToken,
                               TicketAnalysis analysis, UUID userId) throws Exception {
        String assignee = (analysis.getAssignee() != null && !analysis.getAssignee().isBlank())
                ? analysis.getAssignee() : "admin";

        String valueJson = objectMapper.writeValueAsString(Map.of(
                "userId", userId.toString(),
                "title", analysis.getTitle(),
                "type", analysis.getType(),
                "priority", analysis.getPriority(),
                "description", analysis.getDescription() != null ? analysis.getDescription() : "",
                "assignee", assignee
        ));

        List<Map<String, Object>> blocks = new ArrayList<>();

        blocks.add(Map.of(
                "type", "section",
                "text", Map.of("type", "mrkdwn", "text",
                        ":ticket: *Potential ticket detected*\n\n*" +
                        analysis.getType() + " · " + analysis.getPriority() + "*\n" +
                        analysis.getTitle() + "\n:bust_in_silhouette: Assignee: *" + assignee + "*")
        ));

        if (analysis.getDescription() != null && !analysis.getDescription().isBlank()) {
            blocks.add(Map.of(
                    "type", "context",
                    "elements", List.of(Map.of("type", "mrkdwn", "text", analysis.getDescription()))
            ));
        }

        blocks.add(Map.of("type", "divider"));

        blocks.add(Map.of(
                "type", "actions",
                "elements", List.of(
                        Map.of("type", "button",
                                "text", Map.of("type", "plain_text", "text", "✓ Create Ticket"),
                                "style", "primary",
                                "action_id", "create_ticket",
                                "value", valueJson),
                        Map.of("type", "button",
                                "text", Map.of("type", "plain_text", "text", "✗ Dismiss"),
                                "style", "danger",
                                "action_id", "dismiss_ticket",
                                "value", "dismiss")
                )
        ));

        postToSlack(channel, threadTs, botToken, blocks);
    }

    @SuppressWarnings("unchecked")
    public void handleCreateTicket(Map<String, Object> payload, String actionValue) throws Exception {
        Map<String, Object> ticketData = objectMapper.readValue(actionValue, Map.class);

        UUID userId = UUID.fromString((String) ticketData.get("userId"));
        String title = (String) ticketData.get("title");
        String type = (String) ticketData.get("type");
        String priority = (String) ticketData.get("priority");
        String assignee = ticketData.getOrDefault("assignee", "admin").toString();

        TicketResponse ticket = ticketService.createTicketFromSlack(userId, title, type, priority, assignee);

        String channel = (String) ((Map<String, Object>) payload.get("channel")).get("id");
        String ts = (String) ((Map<String, Object>) payload.get("message")).get("ts");
        String botToken = getBotTokenFromPayload(payload);

        if (botToken == null) return;

        List<Map<String, Object>> confirmBlocks = List.of(Map.of(
                "type", "section",
                "text", Map.of("type", "mrkdwn", "text",
                        ":white_check_mark: Created *" + ticket.getTicketNumber() +
                        "* — " + ticket.getTitle() +
                        "  `" + ticket.getType() + "` `" + ticket.getPriority() + "`")
        ));

        updateSlackMessage(channel, ts, botToken, confirmBlocks);
    }

    @SuppressWarnings("unchecked")
    public void handleDismiss(Map<String, Object> payload) throws Exception {
        String channel = (String) ((Map<String, Object>) payload.get("channel")).get("id");
        String ts = (String) ((Map<String, Object>) payload.get("message")).get("ts");
        String botToken = getBotTokenFromPayload(payload);

        if (botToken == null) return;

        List<Map<String, Object>> blocks = List.of(Map.of(
                "type", "section",
                "text", Map.of("type", "mrkdwn", "text", "Dismissed.")
        ));

        updateSlackMessage(channel, ts, botToken, blocks);
    }

    @SuppressWarnings("unchecked")
    private String getBotTokenFromPayload(Map<String, Object> payload) {
        try {
            String teamId = (String) ((Map<String, Object>) payload.get("team")).get("id");
            return integrationService.getBySlackTeamId(teamId)
                    .map(Integration::getAccessToken)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void postToSlack(String channel, String threadTs, String botToken,
                             List<Map<String, Object>> blocks) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", channel);
        body.put("thread_ts", threadTs);
        body.put("blocks", blocks);

        callSlackApi("https://slack.com/api/chat.postMessage", botToken, body);
    }

    private void updateSlackMessage(String channel, String ts, String botToken,
                                    List<Map<String, Object>> blocks) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", channel);
        body.put("ts", ts);
        body.put("blocks", blocks);
        body.put("text", "Ticket proposal");

        callSlackApi("https://slack.com/api/chat.update", botToken, body);
    }

    private void callSlackApi(String url, String botToken, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(botToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
    }
}
