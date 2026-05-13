package com.knowledge.assistant.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/slack")
public class SlackController {

    @PostMapping("/events")
    public ResponseEntity<?> handleEvents(@RequestBody Map<String, Object> payload) {
        String type = (String) payload.get("type");

        if ("url_verification".equals(type)) {
            String challenge = (String) payload.get("challenge");
            return ResponseEntity.ok(Map.of("challenge", challenge));
        }

        return ResponseEntity.ok(Map.of());
    }
}
