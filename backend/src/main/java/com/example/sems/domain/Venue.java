package com.example.sems.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private VenueType type;

    private boolean indoor;

    private String building;

    private String location;

    private int capacity;

    private LocalTime openTime;

    private LocalTime closeTime;

    @Enumerated(EnumType.STRING)
    private VenueStatus status;

    private String rules;

    private String imageUrls;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = VenueStatus.OPEN;
        }
    }
}
