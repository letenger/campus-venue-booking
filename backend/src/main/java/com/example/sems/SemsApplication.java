package com.example.sems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Campus Sports Venue Booking System.
 *
 * Books courts and pitches for a school: students reserve an hour, teachers
 * approve what takes a venue from other people, admins keep the catalogue and
 * read utilisation reports.
 *
 * Stack: Spring Boot 4 + Java 25, Spring Security (JWT) + JPA/Hibernate + MySQL 8
 * (H2 for local/dev), layered Controller -> Service -> Repository.
 */
@SpringBootApplication
@EnableScheduling
public class SemsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SemsApplication.class, args);
    }
}
