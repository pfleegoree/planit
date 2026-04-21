package com.planit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planit.model.Event;
import com.planit.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.Iterator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SeatGeekService {

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final EventRepository eventRepository;

    @Value("${seatgeek.base.url}")
    private String baseUrl;

    @Value("${seatgeek.client.id:}")
    private String clientId;

    public void fetchAndSaveEvents() {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("client_id", clientId)
                .queryParam("venue.city", "Austin")
                .queryParam("per_page", 50)
                .build()
                .toUriString();

        System.out.println("Calling SeatGeek: " + url.replace(clientId, "CLIENT_ID_REMOVED"));

        ResponseEntity<String> resp = rest.getForEntity(url, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            System.err.println("SeatGeek call failed: " + resp.getStatusCode());
            return;
        }

        try {
            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode events = root.path("events");
            if (!events.isArray()) return;

            for (Iterator<JsonNode> it = events.elements(); it.hasNext(); ) {
                JsonNode e = it.next();
                saveOneEventFromSeatGeekNode(e);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveOneEventFromSeatGeekNode(JsonNode e) {
        String sgId = e.path("id").asText(null);
        if (sgId == null) return;

        Optional<Event> maybeExisting =
                eventRepository.findByProviderAndExternalId("SEATGEEK", sgId);

        Event event = maybeExisting.orElseGet(Event::new);

        // identity fields
        event.setProvider("SEATGEEK");
        event.setExternalId(sgId);

        // basic fields
        event.setTitle(safeText(e.path("title")));
        event.setUrl(safeText(e.path("url")));

        // venue
        JsonNode venue = e.path("venue");
        event.setVenueName(safeText(venue.path("name")));

        JsonNode location = venue.path("location");
        if (location.hasNonNull("lat")) {
            event.setLatitude(location.path("lat").asText());
        }
        if (location.hasNonNull("lon")) {
            event.setLongitude(location.path("lon").asText());
        }

        // dates
        if (e.hasNonNull("datetime_utc")) {
            String start = e.path("datetime_utc").asText();
            event.setStartTime(start);
            event.setEndTime(Instant.parse(start).plus(Duration.ofHours(2)).toString());
        }

        // category
        JsonNode taxonomies = e.path("taxonomies");
        if (taxonomies.isArray() && taxonomies.size() > 0) {
            event.setCategory(safeText(taxonomies.get(0).path("name")));
        }

        // genre
        JsonNode performers = e.path("performers");
        if (performers.isArray() && performers.size() > 0) {
            event.setGenre(safeText(performers.get(0).path("type")));
        }

        eventRepository.save(event);
    }

    private String safeText(JsonNode node) {
        return (node == null || node.isNull() || node.isMissingNode()) ? null : node.asText(null);
    }
}
