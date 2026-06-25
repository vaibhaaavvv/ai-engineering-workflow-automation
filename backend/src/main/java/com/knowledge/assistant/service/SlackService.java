package com.knowledge.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.assistant.dto.SlackUpdateCommand;
import com.knowledge.assistant.dto.TicketAnalysis;
import com.knowledge.assistant.dto.TicketResponse;
import com.knowledge.assistant.model.Integration;
import com.knowledge.assistant.model.TicketPriority;
import com.knowledge.assistant.model.TicketType;

import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final List<String> TEST_SCRIPT = List.of(
        "Alex: morning! you catch that game last night?",
        "Priya: lol no i was buried in PRs til midnight, what happened",
        "Alex: they lost in OT it was brutal. anyway how are we looking for the release today?",
        "Priya: mostly fine. Raj deployed the hotfix from yesterday and CI is green",
        "Alex: nice. quick thing — did you see the support channel? 5 users in a row saying they can't log in at all. page just spins and never goes through. started about 2-3 hours ago",
        "Priya: yeah just saw that. looks like it broke session tokens for users who signed up before last Thursday — something in the auth migration. Vaibhav owns that auth code, he needs to drop everything and fix it",
        "Alex: pinging him now",
        "Priya: Vaibhav also check the password reset flow while you're in there — if session tokens are busted the reset emails are probably broken too",
        "Alex: he's on it. btw separate thing — have you noticed the dashboard taking forever after login? like 8-10 seconds before anything shows up",
        "Priya: YES on two different machines, been like this for 3 days. Alex you wrote most of that initial data fetch right? can you dig into it",
        "Alex: yeah i'll take it. hey did Maya send the Figma handoff for the new onboarding flow yet?",
        "Priya: not yet, she said EOD today. we haven't started that sprint anyway so no blocker",
        "Alex: ok. oh — i showed the app to a potential customer yesterday and the first thing they asked was if they can export tickets to a spreadsheet",
        "Priya: that comes up all the time honestly. it's just a CSV dump. Vaibhav do you want to take that one? you've done export flows before, should be straightforward",
        "Alex: +1, medium priority, not urgent",
        "Priya: also while we're doing housekeeping — we're still on Node 14 on the backend workers. it's been EOL for over a year and security advisories keep coming in",
        "Alex: Raj can you own the Node 14 to 20 upgrade? it's mostly infra work and you know that environment best",
        "Priya: also the dashboard loading issue — still happening right? we haven't fixed it yet",
        "Alex: still slow, haven't had a chance to look yet",
        "Priya: ok switching gears — lunch? i'm starving",
        "Alex: WAIT. just got a DM from support. checkout is completely broken in prod. users hitting a 500 trying to complete a purchase. multiple reports coming in right now",
        "Priya: oh god. Raj you own the payments service — you're up",
        "Alex: he's on it. hey while we're here — on the dashboard it would be really useful to filter tickets by assignee. right now it's one big pile and it's hard to see your own work",
        "Priya: Vaibhav can you scope and build that? you're most familiar with the tickets data model",
        "Alex: Raj fixed it, payments are back. what a morning",
        "Priya: thank god. NOW can we get lunch"
    );

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
                "&scope=channels:history,groups:history,chat:write,app_mentions:read" +
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
    public void processCommand(Map<String, Object> event, String teamId) {
        try {
            String rawText = (String) event.get("text");
            if (rawText == null || rawText.isBlank()) return;

            String commandText = rawText.replaceAll("<@[A-Z0-9]+>", "").trim();
            if (commandText.isBlank()) return;

            String channel = (String) event.get("channel");
            String ts = (String) event.get("ts");

            Optional<Integration> integrationOpt = integrationService.getBySlackTeamId(teamId);
            if (integrationOpt.isEmpty()) return;

            Integration integration = integrationOpt.get();
            String botToken = integration.getAccessToken();
            UUID userId = integration.getUserId();

            String lower = commandText.toLowerCase();
            Matcher ticketMatcher = Pattern.compile("\\bT-\\d+\\b", Pattern.CASE_INSENSITIVE).matcher(commandText);
            boolean hasTicketId = ticketMatcher.find();
            String ticketId = hasTicketId ? ticketMatcher.group().toUpperCase() : null;

            if (lower.startsWith("test-run") || lower.startsWith("test run")) {
                runTestConversation(channel, botToken, userId);
                return;
            }

            if (hasTicketId && (lower.contains("delete") || lower.contains("remove"))) {
                handleDeleteCommand(channel, ts, botToken, userId, ticketId);
                return;
            }

            if (hasTicketId) {
                handleUpdateCommand(channel, ts, botToken, userId, commandText);
                return;
            }

            TicketAnalysis analysis = aiService.extractTicketFromCommand(commandText);

            String assignee = (analysis.getAssignee() != null && !analysis.getAssignee().isBlank())
                    ? analysis.getAssignee() : "admin";

            TicketResponse ticket = ticketService.createTicketFromSlack(
                    userId, analysis.getTitle(), analysis.getType(), analysis.getPriority(), assignee,
                    analysis.getDescription());

            postCommandConfirmation(channel, ts, botToken, ticket, analysis, assignee);

        } catch (Exception e) {
            log.error("Failed to process Slack command: {}", e.getMessage());
        }
    }

    private void handleUpdateCommand(String channel, String ts, String botToken,
                                      UUID userId, String commandText) throws Exception {
        SlackUpdateCommand cmd = aiService.extractUpdateCommand(commandText);

        if (cmd == null || cmd.getTicketNumber() == null || cmd.getField() == null || cmd.getValue() == null) {
            postToSlack(channel, ts, botToken, List.of(Map.of(
                    "type", "section",
                    "text", Map.of("type", "mrkdwn", "text",
                            "Couldn't parse that update. Please include the ticket ID (e.g. T-001) and what to change.")
            )));
            return;
        }

        try {
            TicketResponse ticket = ticketService.updateTicketFromSlack(
                    userId, cmd.getTicketNumber(), cmd.getField(), cmd.getValue());
            postToSlack(channel, ts, botToken, List.of(Map.of(
                    "type", "section",
                    "text", Map.of("type", "mrkdwn", "text",
                            ":pencil2: Updated *" + ticket.getTicketNumber() + "* — " + ticket.getTitle() + "\n" +
                            "*" + cmd.getField() + "* → *" + cmd.getValue() + "*")
            )));
        } catch (NoSuchElementException e) {
            postToSlack(channel, ts, botToken, List.of(Map.of(
                    "type", "section",
                    "text", Map.of("type", "mrkdwn", "text",
                            ":x: Ticket *" + cmd.getTicketNumber() + "* not found.")
            )));
        }
    }

    private void handleDeleteCommand(String channel, String ts, String botToken,
                                      UUID userId, String ticketId) throws Exception {
        try {
            TicketResponse ticket = ticketService.deleteTicketFromSlack(userId, ticketId);
            postToSlack(channel, ts, botToken, List.of(Map.of(
                    "type", "section",
                    "text", Map.of("type", "mrkdwn", "text",
                            ":wastebasket: Deleted *" + ticket.getTicketNumber() + "* — " + ticket.getTitle())
            )));
        } catch (NoSuchElementException e) {
            postToSlack(channel, ts, botToken, List.of(Map.of(
                    "type", "section",
                    "text", Map.of("type", "mrkdwn", "text",
                            ":x: Ticket *" + ticketId + "* not found.")
            )));
        }
    }

    @SuppressWarnings("unchecked")
    private void runTestConversation(String channel, String botToken, UUID userId) throws Exception {
        List<TicketResponse> openTickets = new ArrayList<>(ticketService.getOpenTicketsByUserId(userId));

        for (String line : TEST_SCRIPT) {
            // Post the line as a real top-level channel message (no thread)
            String messageTs = postChannelMessage(channel, botToken, line);

            // Analyse it exactly like a real incoming message
            TicketAnalysis analysis = aiService.analyzeMessage(line, openTickets);

            if ("PROPOSE".equals(analysis.getAction())) {
                String assignee = (analysis.getAssignee() != null && !analysis.getAssignee().isBlank())
                        ? analysis.getAssignee() : "admin";

                // Track proposed ticket so later messages can detect duplicates within the run
                TicketResponse mock = new TicketResponse();
                mock.setTicketNumber("T-SIM-" + (openTickets.size() + 1));
                mock.setTitle(analysis.getTitle());
                mock.setType(safeType(analysis.getType()));
                mock.setPriority(safePriority(analysis.getPriority()));
                mock.setAssignee(assignee);
                openTickets.add(mock);

                // Reply in that message's thread — identical to the normal proposal flow
                postProposal(channel, messageTs, botToken, analysis, userId);
            }
            // DUPLICATE and NO_ACTION → silent, same as real message processing

            Thread.sleep(1000);
        }
    }

    @SuppressWarnings("unchecked")
    private String postChannelMessage(String channel, String botToken, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(botToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("channel", channel, "text", text);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://slack.com/api/chat.postMessage",
                new HttpEntity<>(body, headers),
                Map.class);

        return (String) response.getBody().get("ts");
    }

    private TicketType safeType(String type) {
        try { return TicketType.valueOf(type); } catch (Exception e) { return TicketType.TASK; }
    }

    private TicketPriority safePriority(String priority) {
        try { return TicketPriority.valueOf(priority); } catch (Exception e) { return TicketPriority.MEDIUM; }
    }

    private void postCommandConfirmation(String channel, String threadTs, String botToken,
                                          TicketResponse ticket, TicketAnalysis analysis,
                                          String assignee) throws Exception {
        List<Map<String, Object>> blocks = new ArrayList<>();

        blocks.add(Map.of(
                "type", "section",
                "text", Map.of("type", "mrkdwn", "text",
                        ":white_check_mark: *Ticket created: " + ticket.getTicketNumber() + "*\n\n" +
                        "*" + ticket.getType() + " · " + ticket.getPriority() + "*\n" +
                        ticket.getTitle() + "\n:bust_in_silhouette: Assignee: *" + assignee + "*")
        ));

        if (analysis.getDescription() != null && !analysis.getDescription().isBlank()) {
            blocks.add(Map.of(
                    "type", "context",
                    "elements", List.of(Map.of("type", "mrkdwn", "text", analysis.getDescription()))
            ));
        }

        postToSlack(channel, threadTs, botToken, blocks);
    }

    @Async
    @SuppressWarnings("unchecked")
    public void processMessage(Map<String, Object> event, String teamId) {
        try {
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
        String description = (String) ticketData.get("description");

        TicketResponse ticket = ticketService.createTicketFromSlack(userId, title, type, priority, assignee, description);

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
