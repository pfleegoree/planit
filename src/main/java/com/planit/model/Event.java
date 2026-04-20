package com.planit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_provider_external_id", columnNames = {"provider", "external_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Keeping temporarily during migration if code still uses it
    private String ticketmasterId;

    @Column(name = "external_id")
    private String externalId;

    @Column(nullable = false)
    private String provider;

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