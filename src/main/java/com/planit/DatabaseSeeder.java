package com.planit;

import com.planit.model.Event;
import com.planit.repository.EventRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DatabaseSeeder {

    @Bean
    CommandLineRunner initDatabase(EventRepository repository) {
        return args -> {
            Event event = new Event();
            event.setTitle("Rock the Night");
            event.setGenre("Rock");
            event.setCity("Austin");
            event.setVenue("Stubbs BBQ");
            event.setStartTime(LocalDateTime.of(2025, 8, 1, 19, 0));
            event.setEndTime(LocalDateTime.of(2025, 8, 1, 22, 0));
            event.setUrl("https://ticketmaster.com/event/rock-the-night");

            repository.save(event);
        };
    }
}

