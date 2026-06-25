package com.knowledge.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.assistant.dto.TicketAnalysis;
import com.knowledge.assistant.dto.TicketResponse;
import com.knowledge.assistant.model.Integration;
import com.knowledge.assistant.model.TicketPriority;
import com.knowledge.assistant.model.TicketType;
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
        "Priya: lol no i was buried in PRs til midnight what happened",
        "Alex: they lost in OT it was brutal. anyway how are we looking for the release today?",
        "Priya: i think we're mostly fine. Raj already deployed the hotfix from yesterday and CI is green",
        "Alex: nice. oh quick thing — did you see the support channel? there are like 5 users in a row saying they can't log in at all. the page just spins and never goes through. started maybe 2-3 hours ago",
        "Priya: yeah i just saw that. looks like it's affecting users who signed up before last Thursday — something in the auth migration broke their session tokens. this is pretty bad, Vaibhav owns that code, he should drop everything for it",
        "Alex: pinging him now",
        "Priya: also while we're at it — can someone check if the password reset flow is affected too? if their session tokens are broken the reset might be as well",
        "Alex: good call. Vaibhav said he's looking at it now. btw totally separate topic — have you noticed the dashboard taking ages to load? like after you log in it just hangs for a solid 8-10 seconds before anything shows up",
        "Priya: YES omg i thought it was just my laptop but i've seen it on two different machines. it's been like this for at least 3 days. definitely something wrong with the initial data fetch. makes the whole app feel broken on first load",
        "Alex: agreed. i'll add it to the board",
        "Priya: btw did the design handoff for the new onboarding flow come through? i haven't seen anything in Figma yet",
        "Alex: not yet, Maya said EOD today. shouldn't block us anyway since we haven't started that sprint",
        "Priya: ok cool. oh also — i was showing the app to a potential customer yesterday and they immediately asked if they can export tickets to a spreadsheet. we obviously don't have that. they use Excel for everything",
        "Alex: haha yeah that comes up all the time. honestly it's not that hard to add — just a CSV dump of the ticket list. we should build it, i'd say medium priority",
        "Priya: agreed, let's put it on the backlog",
        "Alex: hey while we're doing housekeeping — we're still running Node 14 on the backend workers. it hit end-of-life last year and we haven't upgraded. i keep seeing security advisories for it. we really need to get to Node 20",
        "Priya: ugh yes that's been on the backlog forever. ok i'll put it on the next sprint planning",
        "Alex: also the dashboard loading thing — it's still super slow right? like we haven't fixed that yet",
        "Priya: nope still slow. ok switching gears — lunch? i'm starving",
        "Alex: yeah in a sec. WAIT — just got a DM from support. checkout is completely broken in production. users are hitting a 500 when they try to complete a purchase. like right now. multiple reports coming in",
        "Priya: oh god. ok lunch is cancelled lol",
        "Alex: who owns the payment service? is it Vaibhav again?",
        "Priya: no i think it's Raj. pinging him. ok he's on it",
        "Alex: cool. hey random thought while this is happening — on the dashboard it would be really useful to be able to filter tickets by who they're assigned to. right now everything is just one big list and it's hard to see your own work",
        "Priya: yes please. i've wanted that since day one",
        "Alex: ok raj fixed the checkout thing. payments are back. what a morning",
        "Priya: thank god. ok NOW can we get lunch"
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

            if (commandText.toLowerCase().startsWith("test-run") || commandText.toLowerCase().startsWith("test run")) {
                runTestConversation(channel, ts, botToken, userId);
                return;
            }

            TicketAnalysis analysis = aiService.extractTicketFromCommand(commandText);

            String assignee = (analysis.getAssignee() != null && !analysis.getAssignee().isBlank())
                    ? analysis.getAssignee() : "admin";

            TicketResponse ticket = ticketService.createTicketFromSlack(
                    userId, analysis.getTitle(), analysis.getType(), analysis.getPriority(), assignee);

            postCommandConfirmation(channel, ts, botToken, ticket, analysis, assignee);

        } catch (Exception e) {
            log.error("Failed to process Slack command: {}", e.getMessage());
        }
    }

    private void runTestConversation(String channel, String ts, String botToken, UUID userId) throws Exception {
        // Post the opening header
        postToSlack(channel, ts, botToken, List.of(Map.of(
                "type", "section",
                "text", Map.of("type", "mrkdwn", "text",
                        ":test_tube: *Test run started* — replaying a 28-message conversation between Alex & Priya.\n" +
                        "Analysing each message. Results will appear below as they come in...")
        )));

        // Seed with real open tickets so the model can detect duplicates against existing board state
        List<TicketResponse> openTickets = new ArrayList<>(ticketService.getOpenTicketsByUserId(userId));

        int ticketCounter = 0;
        int dupCounter = 0;
        int noActionCounter = 0;

        for (int i = 0; i < TEST_SCRIPT.size(); i++) {
            String message = TEST_SCRIPT.get(i);
            TicketAnalysis analysis = aiService.analyzeMessage(message, openTickets);
            String action = analysis.getAction();

            if ("PROPOSE".equals(action)) {
                ticketCounter++;
                String assignee = (analysis.getAssignee() != null && !analysis.getAssignee().isBlank())
                        ? analysis.getAssignee() : "admin";

                // Add this proposed ticket to openTickets so subsequent messages can detect it as a duplicate
                TicketResponse mock = new TicketResponse();
                mock.setTicketNumber("T-TEST-" + ticketCounter);
                mock.setTitle(analysis.getTitle());
                mock.setType(safeType(analysis.getType()));
                mock.setPriority(safePriority(analysis.getPriority()));
                mock.setAssignee(assignee);
                openTickets.add(mock);

                List<Map<String, Object>> blocks = new ArrayList<>();
                blocks.add(Map.of(
                        "type", "section",
                        "text", Map.of("type", "mrkdwn", "text",
                                ":ticket: *[" + (i + 1) + "] " + message + "*\n\n" +
                                "`" + analysis.getType() + "` `" + analysis.getPriority() + "`  " +
                                analysis.getTitle() + "  :bust_in_silhouette: *" + assignee + "*")
                ));
                if (analysis.getDescription() != null && !analysis.getDescription().isBlank()) {
                    blocks.add(Map.of(
                            "type", "context",
                            "elements", List.of(Map.of("type", "mrkdwn", "text", analysis.getDescription()))
                    ));
                }
                postToSlack(channel, ts, botToken, blocks);

            } else if ("DUPLICATE".equals(action)) {
                dupCounter++;
                postToSlack(channel, ts, botToken, List.of(Map.of(
                        "type", "section",
                        "text", Map.of("type", "mrkdwn", "text",
                                ":twisted_rightwards_arrows: *[" + (i + 1) + "] Duplicate detected*\n" +
                                "_" + message + "_")
                )));
            } else {
                noActionCounter++;
            }
        }

        // Summary
        postToSlack(channel, ts, botToken, List.of(
                Map.of("type", "divider"),
                Map.of("type", "section",
                        "text", Map.of("type", "mrkdwn", "text",
                                ":white_check_mark: *Test run complete*\n\n" +
                                ":ticket: Tickets proposed: *" + ticketCounter + "*\n" +
                                ":twisted_rightwards_arrows: Duplicates caught: *" + dupCounter + "*\n" +
                                ":mute: No-action (casual): *" + noActionCounter + "*\n\n" +
                                "_No real tickets were created. This was a read-only simulation._"))
        ));
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
