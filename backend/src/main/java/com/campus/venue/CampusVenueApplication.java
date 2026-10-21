package com.campus.venue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Campus sports venue booking for middle and high schools.
 *
 * <p>Students book a court (volleyball, badminton, football, indoor or outdoor tennis,
 * squash, table tennis), teachers approve the requests that take a venue away from
 * others, and admins manage the catalogue and read utilisation reports.
 *
 * <p>Stack: Spring Boot 4, Java 25, Spring Security with a hand-rolled HS256 JWT,
 * Spring Data JPA, H2 in dev and MySQL 8 in prod. Layered as
 * Controller -> Service -> Repository.
 */
@SpringBootApplication
@EnableScheduling
public class CampusVenueApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusVenueApplication.class, args);
    }
}
