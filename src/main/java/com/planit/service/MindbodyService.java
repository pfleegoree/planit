package com.planit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.planit.model.Event;
import com.planit.repository.EventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class MindbodyService {

    private final EventRepository eventRepository;
    private final RestClient restClient;

    @Value("${mindbody.api.key:}")
    private String apiKey;

    @Value("${mindbody.site.id:-99}")
    private String siteId;

    public MindbodyService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.mindbodyonline.com/public/v6")
                .defaultHeader("User-Agent", "PlanIT")
                .build();
    }

    public void fetchAndSaveEvents() {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/class/classes")
                        .queryParam("StartDateTime", OffsetDateTime.now().toString())
                        .queryParam("EndDateTime", OffsetDateTime.now().plusDays(14).toString())
                        .build())
                .header("Api-Key", apiKey)
                .header("SiteId", siteId)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("Classes")) {
            System.out.println("No Mindbody classes found");
            return;
        }

        for (JsonNode mindbodyClass : response.get("Classes")) {
            String mbId = mindbodyClass.path("Id").asText(null);
            if (mbId == null) continue;

            Optional<Event> maybeExisting =
                    eventRepository.findByProviderAndExternalId("MINDBODY", mbId);

            Event event = maybeExisting.orElseGet(Event::new);

            // identity fields
            event.setProvider("MINDBODY");
            event.setExternalId(mbId);

            event.setTitle(mindbodyClass.path("ClassDescription").path("Name").asText("Mindbody Class"));
            event.setCategory("Fitness");
            event.setGenre("Wellness");

            event.setVenueName(
                    mindbodyClass.path("Location").path("Name").asText("Mindbody Studio")
            );

            // store as ISO string to match the rest of the codebase
            String startRaw = mindbodyClass.path("StartDateTime").asText(null);
            String endRaw   = mindbodyClass.path("EndDateTime").asText(null);

            event.setStartTime(startRaw != null ? OffsetDateTime.parse(startRaw).toInstant().toString() : null);
            event.setEndTime(endRaw   != null ? OffsetDateTime.parse(endRaw).toInstant().toString()   : null);

            event.setUrl(null);

            eventRepository.save(event);
        }

        System.out.println("Mindbody classes saved");
    }
}
