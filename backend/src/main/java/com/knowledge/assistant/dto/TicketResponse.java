package com.knowledge.assistant.dto;

import com.knowledge.assistant.model.TicketPriority;
import com.knowledge.assistant.model.TicketStatus;
import com.knowledge.assistant.model.TicketType;

import java.time.LocalDateTime;
import java.util.UUID;

public class TicketResponse {

    private UUID id;
    private String ticketNumber;
    private TicketType type;
    private String title;
    private String description;
    private TicketPriority priority;
    private String assignee;
    private TicketStatus status;
    private String branchName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TicketResponse() {}

    public TicketResponse(UUID id, String ticketNumber, TicketType type, String title,
                          String description, TicketPriority priority, String assignee,
                          TicketStatus status, String branchName,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.ticketNumber = ticketNumber;
        this.type = type;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignee = assignee;
        this.status = status;
        this.branchName = branchName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public TicketType getType() { return type; }
    public void setType(TicketType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
