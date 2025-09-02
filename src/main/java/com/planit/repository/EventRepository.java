package com.planit.repository;

import com.planit.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCategoryIn(List<String> categories);
    List<Event> findByGenreIn(List<String> genres);
    List<Event> findByCategoryInAndGenreIn(List<String> categories, List<String> genres);
    Optional<Event> findByTicketmasterId(String ticketmasterId);
}
