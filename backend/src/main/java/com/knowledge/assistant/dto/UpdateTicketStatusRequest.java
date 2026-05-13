package com.knowledge.assistant.dto;

import com.knowledge.assistant.model.TicketStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateTicketStatusRequest {

    @NotNull(message = "Status is required")
    private TicketStatus status;

    public UpdateTicketStatusRequest() {}

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
}
