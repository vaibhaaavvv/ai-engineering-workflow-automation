package com.knowledge.assistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketAnalysis {

    private String action; // PROPOSE or NO_ACTION
    private String title;
    private String type;
    private String priority;
    private String description;
    private String assignee; // name extracted from message, defaults to "admin"

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
}
