package com.planit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String ticketmasterId;

    private String title;
    private String category;
    private String genre;
    private String startTime; // ISO string: 2026-02-20T01:30:00Z
    private String endTime;
    private String url;
    private String venueName;
    private String latitude;
    private String longitude;

}

