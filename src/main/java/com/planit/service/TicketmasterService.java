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
        String city = "Austin";
        String genre = "Rock";

        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("apikey", apiKey)
                .queryParam("city", city)
                .queryParam("classificationName", genre)
                .queryParam("size", 10)
                .toUriString();

        Map response = restTemplate.getForObject(uri, Map.class);

        if (response != null && response.containsKey("_embedded")) {
            List<Map<String, Object>> events = (List<Map<String, Object>>)
                    ((Map<String, Object>) response.get("_embedded")).get("events");

            List<Event> toSave = new ArrayList<>();

            for (Map<String, Object> e : events) {
                Event event = new Event();
                event.setTitle((String) e.get("name"));
                event.setGenre(genre);
                event.setCity(city);
                event.setUrl((String) e.get("url"));

                List<Map<String, Object>> venues = (List<Map<String, Object>>)
                        ((Map<String, Object>) ((List<?>) e.get("_embedded.venues")).get(0));
                if (venues != null && !venues.isEmpty()) {
                    event.setVenue((String) venues.get(0).get("name"));
                }

                Map<String, Object> dates = (Map<String, Object>) e.get("dates");
                Map<String, Object> start = (Map<String, Object>) dates.get("start");

                if (start != null) {
                    String dateTime = (String) start.get("dateTime");
                    if (dateTime != null) {
                        LocalDateTime startTime = LocalDateTime.parse(dateTime.replace("Z", ""));
                        event.setStartTime(startTime);
                        event.setEndTime(startTime.plusHours(2));
                    }
                }

                toSave.add(event);
            }

            eventRepository.saveAll(toSave);
        }
    }
}
