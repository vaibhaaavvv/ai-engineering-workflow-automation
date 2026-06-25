package com.knowledge.assistant.dto;

import com.knowledge.assistant.model.TicketPriority;
import com.knowledge.assistant.model.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateTicketRequest {

    @NotNull(message = "Ticket type is required")
    private TicketType type;

    @NotBlank(message = "Title is required")
    private String title;

    private TicketPriority priority;

    @NotBlank(message = "Assignee (GitHub username) is required")
    private String assignee;

    private String description;

    public CreateTicketRequest() {}

    public TicketType getType() { return type; }
    public void setType(TicketType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
