package com.planit.controller;

import com.planit.service.EventbriteService;
import com.planit.service.TicketmasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final TicketmasterService ticketmasterService;
    private final EventbriteService eventbriteService;

    @PostMapping("/fetch-events")
    public Map<String, Object> fetchEvents() {
        ticketmasterService.fetchAndSaveEvents();
        return Map.of("status", "ok");
    }

    @PostMapping("/fetch-events/eventbrite")
    public ResponseEntity<String> fetchEventbrite(@RequestHeader("X-Admin-Token") String token) {
        if (!token.equals(System.getenv("ADMIN_TOKEN"))) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        eventbriteService.fetchAndSaveEvents();
        return ResponseEntity.ok("Eventbrite fetch triggered");
    }
}
