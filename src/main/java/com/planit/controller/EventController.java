package com.planit.controller;

import com.planit.model.Event;
import com.planit.repository.EventRepository;
import com.planit.service.MindbodyService;
import com.planit.service.TicketmasterService;
// import com.planit.service.SeatGeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://YOUR-NETLIFY-SITE.netlify.app"
})
@RestController
@RequestMapping("/api")
public class EventController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketmasterService ticketmasterService;

    @Autowired
    private MindbodyService mindbodyService;

    // @Autowired
    // private SeatGeekService seatGeekService;

    @GetMapping("/events")
    public List<Event> getEvents(
            @RequestParam(value = "category", required = false) List<String> categories,
            @RequestParam(value = "genre", required = false) List<String> genres
    ) {
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

        if (categories == null && genres == null) {
            return eventRepository.findAll();
        }
        if (categories != null && genres != null) {
            return eventRepository.findByCategoryInAndGenreIn(categories, genres);
        }
        if (categories != null) {
            return eventRepository.findByCategoryIn(categories);
        }

        return eventRepository.findByGenreIn(genres);
    }

    @PostMapping("/fetch-ticketmaster")
    public ResponseEntity<String> fetchTicketmaster(@RequestHeader("X-Admin-Token") String token) {
        if (!token.equals(System.getenv("ADMIN_TOKEN"))) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        ticketmasterService.fetchAndSaveEvents();
        return ResponseEntity.ok("Ticketmaster fetch triggered");
    }

    @PostMapping("/fetch-mindbody")
    public ResponseEntity<String> fetchMindbody(@RequestHeader("X-Admin-Token") String token) {
        if (!token.equals(System.getenv("ADMIN_TOKEN"))) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        mindbodyService.fetchAndSaveEvents();
        return ResponseEntity.ok("Mindbody fetch triggered");
    }

    /*
    @PostMapping("/fetch-seatgeek")
    public ResponseEntity<String> fetchSeatGeek(@RequestHeader("X-Admin-Token") String token) {
        if (!token.equals(System.getenv("ADMIN_TOKEN"))) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        seatGeekService.fetchAndSaveEvents();
        return ResponseEntity.ok("SeatGeek fetch triggered");
    }
    */

    @PostMapping("/fetch-all-events")
    public ResponseEntity<String> fetchAllEvents(@RequestHeader("X-Admin-Token") String token) {
        if (!token.equals(System.getenv("ADMIN_TOKEN"))) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        ticketmasterService.fetchAndSaveEvents();
        mindbodyService.fetchAndSaveEvents();

        return ResponseEntity.ok("Ticketmaster + Mindbody fetch triggered");
    }
}
