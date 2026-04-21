package com.planit.controller;

import com.planit.service.SeatGeekService;
import com.planit.service.TicketmasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final TicketmasterService ticketmasterService;
    private final SeatGeekService seatGeekService;

    @PostMapping("/fetch-events")
    public Map<String, Object> fetchEvents() {
        ticketmasterService.fetchAndSaveEvents();
        return Map.of("status", "ok");
    }

    @PostMapping("/fetch-events/seatgeek")
    public ResponseEntity<String> fetchSeatGeek(@RequestHeader("X-Admin-Token") String token) {
        if (!token.equals(System.getenv("ADMIN_TOKEN"))) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        seatGeekService.fetchAndSaveEvents();
        return ResponseEntity.ok("SeatGeek fetch triggered");
    }
}
