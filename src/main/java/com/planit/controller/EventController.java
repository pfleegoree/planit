package com.planit.controller;

import com.planit.model.Event;
import com.planit.repository.EventRepository;
import com.planit.service.TicketmasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EventController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketmasterService ticketmasterService;

    @GetMapping("/events")
    public List<Event> getEvents(
            // supports ?category=Music or ?category=Music&category=Sports or ?category=All
            @RequestParam(value="category", required=false) List<String> categories,
            // supports ?genre=Rock or ?genre=Rock&genre=Jazz or ?genre=Rock,Jazz
            @RequestParam(value="genre", required=false) List<String> genres
    ) {
        // normalize and drop any "All" placeholders
        if (categories != null) {
            categories = categories.stream()
                    .filter(s -> s != null && !s.trim().isEmpty() && !s.equalsIgnoreCase("all"))
                    .collect(Collectors.toList());
            if (categories.isEmpty()) categories = null;
        }
        if (genres != null) {
            genres = genres.stream()
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .collect(Collectors.toList());
            if (genres.isEmpty()) genres = null;
        }

        // decide which repository call to make
        if (categories == null && genres == null) {
            return eventRepository.findAll();
        }
        if (categories != null && genres != null) {
            return eventRepository.findByCategoryInAndGenreIn(categories, genres);
        }
        if (categories != null) {
            return eventRepository.findByCategoryIn(categories);
        }
        // only genres provided
        return eventRepository.findByGenreIn(genres);
    }

    @GetMapping("/fetch-events")
    public ResponseEntity<String> fetchNow() {
        ticketmasterService.fetchAndSaveEvents();
        return ResponseEntity.ok("Ticketmaster fetch triggered");
    }
}
