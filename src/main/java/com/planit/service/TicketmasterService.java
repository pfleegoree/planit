package com.planit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planit.model.Event;
import com.planit.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.Iterator;
import java.util.Optional;

@Service
public class TicketmasterService {

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private EventRepository eventRepository;

    @Value("${ticketmaster.base.url}")
    private String baseUrl;

    @Value("${ticketmaster.api.key}")
    private String tmApiKey;

    // Call this from your controller /fetch-events
    public void fetchAndSaveEvents() {

        // Build URL safely (encodes params)
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("apikey", tmApiKey)
                .queryParam("city", "Austin")          // example param - change or remove as needed
                .queryParam("size", 50)                // optional: how many events per page
                .queryParam("countryCode", "US")       // optional
                .build()
                .toUriString();

        System.out.println("Calling TM: " + url.replace(tmApiKey, "API_KEY_REMOVED"));

        ResponseEntity<String> resp = rest.getForEntity(url, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            // log and return
            System.err.println("Ticketmaster call failed: " + resp.getStatusCode());
            return;
        }

        try {
            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode events = root.path("_embedded").path("events");
            if (!events.isArray()) return;

            for (Iterator<JsonNode> it = events.elements(); it.hasNext(); ) {
                JsonNode e = it.next();
                saveOneEventFromTmNode(e);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            // log properly
        }
    }

    private void saveOneEventFromTmNode(JsonNode e) {
        String tmId = e.path("id").asText(null);
        if (tmId == null) return;

        Optional<Event> maybeExisting = eventRepository.findByTicketmasterId(tmId);
        Event event = maybeExisting.orElseGet(Event::new);

        // map basic fields
        event.setTicketmasterId(tmId);
        event.setTitle(safeText(e.path("name")));
        event.setUrl(safeText(e.path("url")));

        // classification -> category/genre (take first classification if present)
        JsonNode classifications = e.path("classifications");
        if (classifications.isArray() && classifications.size() > 0) {
            JsonNode c = classifications.get(0);
            event.setCategory(safeText(c.path("segment").path("name")));
            event.setGenre(safeText(c.path("genre").path("name")));
        }

        // venue info (first embedded venue)
        JsonNode venues = e.path("_embedded").path("venues");
        if (venues.isArray() && venues.size() > 0) {
            JsonNode v = venues.get(0);
            event.setVenueName(safeText(v.path("name")));

            // coordinates if present
            JsonNode loc = v.path("location");
            if (loc != null && !loc.isMissingNode() && !loc.isNull()) {
                // If your Event stores lat/long as Strings:
                if (loc.hasNonNull("latitude")) {
                    event.setLatitude(loc.path("latitude").asText(null));   // change to asDouble() if your entity expects double
                }
                if (loc.hasNonNull("longitude")) {
                    event.setLongitude(loc.path("longitude").asText(null));  // change to asDouble() if your entity expects double
                }
            }
        }

        // dates -> start (prefer ISO dateTime; fall back to localDate + localTime + timezone)
        JsonNode startNode = e.path("dates").path("start");
        Instant startInstant = null;
        if (startNode.hasNonNull("dateTime")) {
            String dt = startNode.path("dateTime").asText(null);
            if (dt != null) {
                try {
                    startInstant = Instant.parse(dt);
                } catch (Exception ex) {
                    // occasionally dateTime might include offset rather than Z, Instant.parse handles offsets too.
                    ex.printStackTrace();
                }
            }
        } else if (startNode.hasNonNull("localDate")) {
            String localDate = startNode.path("localDate").asText(null);
            String localTime = startNode.hasNonNull("localTime") ? startNode.path("localTime").asText("00:00:00") : "00:00:00";
            String tz = e.path("dates").path("timezone").asText("UTC");
            if (localDate != null) {
                try {
                    LocalDate ld = LocalDate.parse(localDate);
                    LocalTime lt = LocalTime.parse(localTime);
                    ZonedDateTime zdt = ZonedDateTime.of(ld, lt, ZoneId.of(tz));
                    startInstant = zdt.toInstant();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (startInstant != null) {
            // store as an ISO instant (with Z). This is recommended.
            event.setStartTime(startInstant.toString()); // e.g. 2026-02-20T01:30:00Z

            // end time: TM often doesn't provide end -> use end if available, else +2 hours default
            Instant endInstant = null;
            JsonNode endNode = e.path("dates").path("end");
            if (endNode != null && endNode.hasNonNull("dateTime")) {
                try {
                    endInstant = Instant.parse(endNode.path("dateTime").asText());
                } catch (Exception ignore) {}
            }
            if (endInstant == null) {
                endInstant = startInstant.plus(Duration.ofHours(2));
            }
            event.setEndTime(endInstant.toString());
        } else {
            // no usable start -> clear or skip saving as you prefer
            event.setStartTime(null);
            event.setEndTime(null);
        }

        // persist
        eventRepository.save(event);
    }

    private String safeText(JsonNode node) {
        return (node == null || node.isNull() || node.isMissingNode()) ? null : node.asText(null);
    }
}
