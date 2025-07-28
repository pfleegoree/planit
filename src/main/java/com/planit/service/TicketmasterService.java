package com.planit.service;

import com.planit.model.Event;
import com.planit.repository.EventRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TicketmasterService {

    @Value("${ticketmaster.api.key}")
    private String apiKey;

    @Value("${ticketmaster.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final EventRepository eventRepository;

    public TicketmasterService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @PostConstruct
    public void fetchAndSaveEvents() {
        try {
            String uri = UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .queryParam("apikey", apiKey)
                    .queryParam("city", "Austin")
                    .queryParam("classificationName", "Rock")
                    .queryParam("size", 10)
                    .toUriString();

            // Log raw response for debugging
            Map<String, Object> tmResponse = restTemplate.getForObject(uri, Map.class);
            System.out.println("🎟️ Raw TM response: " + tmResponse);

            if (tmResponse != null && tmResponse.containsKey("_embedded")) {
                Map<String,Object> embeddedRoot = (Map<String,Object>) tmResponse.get("_embedded");
                List<Map<String,Object>> events = (List<Map<String,Object>>) embeddedRoot.get("events");

                List<Event> toSave = new ArrayList<>();
                for (Map<String,Object> e : events) {
                    Event event = new Event();
                    event.setTitle((String) e.get("name"));
                    event.setGenre("Rock");
                    event.setCity("Austin");
                    event.setUrl((String) e.get("url"));

                    // ← Defensively parse venue
                    Map<String,Object> embedded = (Map<String,Object>) e.get("_embedded");
                    if (embedded != null && embedded.get("venues") instanceof List) {
                        List<Map<String,Object>> venues = (List<Map<String,Object>>) embedded.get("venues");
                        if (!venues.isEmpty()) {
                            event.setVenue((String) venues.get(0).get("name"));
                        }
                    }

                    // ← Defensively parse dates
                    Map<String,Object> dates = (Map<String,Object>) e.get("dates");
                    if (dates != null && dates.get("start") instanceof Map) {
                        Map<String,Object> start = (Map<String,Object>) dates.get("start");
                        String dateTime = (String) start.get("dateTime");
                        if (dateTime != null) {
                            LocalDateTime startTime = LocalDateTime.parse(dateTime.replace("Z",""));
                            event.setStartTime(startTime);
                            event.setEndTime(startTime.plusHours(2));
                        }
                    }

                    toSave.add(event);
                }
                eventRepository.saveAll(toSave);
            }
        } catch (Exception ex) {
            System.err.println("❌ TM fetch failed: " + ex.getMessage());
        }
    }
}


