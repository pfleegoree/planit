package com.planit.controller;

import com.planit.service.TicketmasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final TicketmasterService ticketmasterService;

    @PostMapping("/fetch-events")
    public Map<String, Object> fetchEvents() {
        ticketmasterService.fetchAndSaveEvents();
        return Map.of("status", "ok");
    }
}
