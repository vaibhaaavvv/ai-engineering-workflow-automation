package com.knowledge.assistant.controller;

import com.knowledge.assistant.dto.CreateTicketRequest;
import com.knowledge.assistant.dto.TicketResponse;
import com.knowledge.assistant.dto.UpdateTicketStatusRequest;
import com.knowledge.assistant.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicket(request, auth.getName()));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getTickets(Authentication auth) {
        return ResponseEntity.ok(ticketService.getTickets(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable UUID id,
            Authentication auth) {
        return ResponseEntity.ok(ticketService.getTicket(id, auth.getName()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            Authentication auth) {
        return ResponseEntity.ok(ticketService.updateStatus(id, request.getStatus(), auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(
            @PathVariable UUID id,
            Authentication auth) {
        ticketService.deleteTicket(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
