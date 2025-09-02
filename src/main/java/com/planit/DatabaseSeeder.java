package com.planit;

import com.planit.model.Event;
import com.planit.repository.EventRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;

@Configuration
@Profile("dev")
public class DatabaseSeeder {

    @Bean
    CommandLineRunner initDatabase(EventRepository repository) {
        return args -> {
            // Avoid reseeding if DB already has events
            if (repository.count() > 0) {
                System.out.println("Database already contains events — skipping seeder.");
                return;
            }

            // Choose zone (use system zone or inject via props)
            ZoneId zone = ZoneId.systemDefault();

            // Today's date in that zone
            LocalDate today = LocalDate.now(zone);

            // First day of week according to current locale
            DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
            LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek));

            // Create one event per day at 19:00 - 22:00
            for (int d = 0; d < 7; d++) {
                LocalDate day = startOfWeek.plusDays(d);

                LocalTime startTimeLocal = LocalTime.of(19, 0); // 7 PM local
                LocalTime endTimeLocal   = LocalTime.of(22, 0); // 10 PM local

                ZonedDateTime startZdt = ZonedDateTime.of(day, startTimeLocal, zone);
                ZonedDateTime endZdt   = ZonedDateTime.of(day, endTimeLocal,   zone);

                Instant startInstant = startZdt.toInstant();
                Instant endInstant   = endZdt.toInstant();

                Event event = new Event();
                event.setTicketmasterId("seed-" + day.toString()); // unique-ish id per day
                event.setTitle("Seeded Event — " + day.getDayOfWeek());
                event.setCategory("Music");
                event.setGenre("Rock");
                event.setVenueName("Stubbs BBQ");

                // store ISO instant strings (matches your model)
                event.setStartTime(startInstant.toString()); // e.g. 2025-08-02T00:00:00Z
                event.setEndTime(endInstant.toString());

                // example coords as strings (your model uses Strings)
                event.setLatitude("30.266");
                event.setLongitude("-97.740");

                event.setUrl("https://ticketmaster.com/event/seed-" + day.toString());

                repository.save(event);
            }

            System.out.println("Seeded events for week starting " + startOfWeek);
        };
    }
}
