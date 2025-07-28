package com.planit.controller;

import com.planit.model.Event;
import com.planit.repository.EventRepository;      // ← import this
import com.planit.service.TicketmasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EventController {

    @Autowired
    private EventRepository eventRepository;      // ← inject it

    @Autowired
    private TicketmasterService ticketmasterService;

    // existing endpoint:
    @GetMapping("/events")
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    // new “fetch now” trigger:
    @GetMapping("/fetch-events")
    public ResponseEntity<String> fetchNow() {
        ticketmasterService.fetchAndSaveEvents();
        return ResponseEntity.ok("Ticketmaster fetch triggered");
    }
}
