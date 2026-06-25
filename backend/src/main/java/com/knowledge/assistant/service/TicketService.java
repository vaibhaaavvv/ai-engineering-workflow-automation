package com.knowledge.assistant.service;

import com.knowledge.assistant.dto.CreateTicketRequest;
import com.knowledge.assistant.dto.TicketResponse;
import com.knowledge.assistant.model.*;
import com.knowledge.assistant.repository.TicketRepository;
import com.knowledge.assistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Autowired
    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        int seq = ticketRepository.findMaxSequenceNumberByUserId(user.getId()) + 1;
        String ticketNumber = String.format("T-%03d", seq);
        String branchName = generateBranchName(ticketNumber, request.getTitle());

        Ticket ticket = new Ticket();
        ticket.setUserId(user.getId());
        ticket.setSequenceNumber(seq);
        ticket.setTicketNumber(ticketNumber);
        ticket.setType(request.getType());
        ticket.setTitle(request.getTitle());
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM);
        ticket.setAssignee(request.getAssignee());
        ticket.setDescription(request.getDescription());
        ticket.setStatus(TicketStatus.TO_DO);
        ticket.setBranchName(branchName);

        return toResponse(ticketRepository.save(ticket));
    }

    public List<TicketResponse> getOpenTicketsByUserId(UUID userId) {
        return ticketRepository.findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, TicketStatus.COMPLETED)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TicketResponse> getTickets(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TicketResponse getTicket(UUID id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Ticket ticket = ticketRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Ticket not found"));

        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateStatus(UUID id, TicketStatus status, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Ticket ticket = ticketRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Ticket not found"));

        ticket.setStatus(status);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse createTicketFromSlack(UUID userId, String title, String type,
                                                String priority, String assignee, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle(title);
        request.setType(TicketType.valueOf(type));
        request.setPriority(TicketPriority.valueOf(priority));
        request.setAssignee(assignee != null && !assignee.isBlank() ? assignee : "admin");
        request.setDescription(description != null && !description.isBlank()
                ? description
                : generateFallbackDescription(title, type));

        return createTicket(request, user.getEmail());
    }

    @Transactional
    public TicketResponse updateTicketFromSlack(UUID userId, String ticketNumber, String field, String value) {
        Ticket ticket = ticketRepository.findByTicketNumberAndUserId(ticketNumber.toUpperCase(), userId)
                .orElseThrow(() -> new NoSuchElementException("Ticket " + ticketNumber + " not found"));

        switch (field.toUpperCase()) {
            case "ASSIGNEE"  -> ticket.setAssignee(value);
            case "STATUS"    -> ticket.setStatus(TicketStatus.valueOf(value));
            case "TITLE"     -> ticket.setTitle(value);
            case "PRIORITY"  -> ticket.setPriority(TicketPriority.valueOf(value));
            default          -> throw new IllegalArgumentException("Unknown field: " + field);
        }

        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse deleteTicketFromSlack(UUID userId, String ticketNumber) {
        Ticket ticket = ticketRepository.findByTicketNumberAndUserId(ticketNumber.toUpperCase(), userId)
                .orElseThrow(() -> new NoSuchElementException("Ticket " + ticketNumber + " not found"));
        TicketResponse response = toResponse(ticket);
        ticketRepository.delete(ticket);
        return response;
    }

    @Transactional
    public void deleteTicket(UUID id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Ticket ticket = ticketRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Ticket not found"));

        ticketRepository.delete(ticket);
    }

    private String generateFallbackDescription(String title, String type) {
        return switch (type.toUpperCase()) {
            case "BUG"     -> title + ". This issue needs investigation to identify the root cause and a fix applied to restore expected behaviour.";
            case "FEATURE" -> title + ". This capability does not currently exist and needs to be designed and implemented.";
            case "TASK"    -> title + ". This technical work item needs to be completed as part of ongoing maintenance or improvement.";
            default        -> title + ". Details and acceptance criteria to be added by the assignee.";
        };
    }

    private String generateBranchName(String ticketNumber, String title) {
        String[] words = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .split("\\s+");

        StringBuilder sb = new StringBuilder(ticketNumber.toLowerCase());
        int count = 0;
        for (String word : words) {
            if (!word.isEmpty() && count < 3) {
                sb.append("-").append(word);
                count++;
            }
        }
        return sb.toString();
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getType(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getStatus(),
                ticket.getBranchName(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
