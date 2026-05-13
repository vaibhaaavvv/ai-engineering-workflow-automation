package com.knowledge.assistant.controller;

import com.knowledge.assistant.service.IntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;

    @Autowired
    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getStatus(Authentication auth) {
        return ResponseEntity.ok(integrationService.getStatus(auth.getName()));
    }
}
