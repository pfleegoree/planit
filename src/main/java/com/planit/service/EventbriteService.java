package com.planit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planit.model.Event;
import com.planit.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.Iterator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventbriteService {

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final EventRepository eventRepository;

    @Value("${eventbrite.base.url}")
    private String baseUrl;

    @Value("${eventbrite.token:}")
    private String eventbriteToken;

    public void fetchAndSaveEvents() {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("location.address", "Austin")
                .queryParam("location.within", "25mi")
                .queryParam("expand", "venue")
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(eventbriteToken);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        System.out.println("Calling Eventbrite: " + url);

        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            System.err.println("Eventbrite call failed: " + resp.getStatusCode());
            return;
        }

        try {
            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode events = root.path("events");

            if (!events.isArray()) return;

            for (Iterator<JsonNode> it = events.elements(); it.hasNext(); ) {
                JsonNode e = it.next();
                saveOneEventFromEventbriteNode(e);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveOneEventFromEventbriteNode(JsonNode e) {
        String ebId = e.path("id").asText(null);
        if (ebId == null) return;

        Optional<Event> maybeExisting =
                eventRepository.findByProviderAndExternalId("EVENTBRITE", ebId);

        Event event = maybeExisting.orElseGet(Event::new);

        // identity
        event.setProvider("EVENTBRITE");
        event.setExternalId(ebId);

        // core fields
        event.setTitle(safeText(e.path("name").path("text")));
        event.setUrl(safeText(e.path("url")));

        // Eventbrite category/genre mapping can be messy; keep simple for v1
        event.setCategory("Eventbrite");
        event.setGenre(safeText(e.path("category").path("name")));

        // venue
        JsonNode venue = e.path("venue");
        if (!venue.isMissingNode() && !venue.isNull()) {
            event.setVenueName(safeText(venue.path("name")));

            if (venue.hasNonNull("latitude")) {
                event.setLatitude(venue.path("latitude").asText(null));
            }
            if (venue.hasNonNull("longitude")) {
                event.setLongitude(venue.path("longitude").asText(null));
            }
        }

        // dates
        String startUtc = safeText(e.path("start").path("utc"));
        String endUtc   = safeText(e.path("end").path("utc"));

        event.setStartTime(normalizeUtc(startUtc));
        event.setEndTime(normalizeUtc(endUtc));

        // fallback end time if missing
        if (event.getStartTime() != null && event.getEndTime() == null) {
            try {
                Instant start = Instant.parse(event.getStartTime());
                event.setEndTime(start.plus(Duration.ofHours(2)).toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        eventRepository.save(event);
    }

    private String safeText(JsonNode node) {
        return (node == null || node.isNull() || node.isMissingNode()) ? null : node.asText(null);
    }

    private String normalizeUtc(String utcString) {
        if (utcString == null || utcString.isBlank()) return null;
        try {
            return Instant.parse(utcString).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
