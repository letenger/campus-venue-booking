package com.example.sems;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class SemsApplicationTests {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void fullBookingFlow() throws Exception {
        // seeded demo accounts from DataLoader
        String admin = login("admin", "admin123");
        String student = login("student", "student123");
        String teacher = login("teacher", "teacher123");

        // admin creates a venue
        String venueBody = objectMapper.writeValueAsString(Map.of(
                "name", "测试羽毛球场",
                "type", "BADMINTON",
                "indoor", true,
                "capacity", 4,
                "openTime", "08:00",
                "closeTime", "21:00",
                "status", "OPEN"));
        MvcResult created = auth(post("/api/venues").contentType(MediaType.APPLICATION_JSON).content(venueBody), admin)
                .andExpect(status().isOk())
                .andReturn();
        long venueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asLong();

        // availability returns slots
        auth(get("/api/venues/" + venueId + "/availability").param("from", LocalDate.now().plusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString()), student)
                .andExpect(status().isOk());

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // student: small free-play booking -> auto approved
        String bookingBody = objectMapper.writeValueAsString(Map.of(
                "venueId", venueId,
                "bookingDate", tomorrow.toString(),
                "startTime", "10:00",
                "endTime", "11:00",
                "purpose", "FREE_PLAY",
                "attendeesCount", 2));
        MvcResult b1 = auth(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(bookingBody), student)
                .andExpect(status().isOk())
                .andReturn();
        JsonNode b1Data = objectMapper.readTree(b1.getResponse().getContentAsString()).get("data");
        org.junit.jupiter.api.Assertions.assertEquals("APPROVED", b1Data.get("status").asText());

        // overlapping booking -> conflict 409
        auth(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(bookingBody), student)
                .andExpect(status().isConflict());

        // student: club booking -> needs approval (PENDING)
        String clubBody = objectMapper.writeValueAsString(Map.of(
                "venueId", venueId,
                "bookingDate", tomorrow.toString(),
                "startTime", "12:00",
                "endTime", "13:00",
                "purpose", "CLUB_TRAINING",
                "attendeesCount", 4));
        MvcResult b2 = auth(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(clubBody), student)
                .andExpect(status().isOk())
                .andReturn();
        long booking2Id = objectMapper.readTree(b2.getResponse().getContentAsString()).get("data").get("id").asLong();
        org.junit.jupiter.api.Assertions.assertEquals("PENDING",
                objectMapper.readTree(b2.getResponse().getContentAsString()).get("data").get("status").asText());

        // teacher approves
        auth(post("/api/bookings/" + booking2Id + "/approve"), teacher)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // utilization report (teacher) — locate the created venue by id (report is ordered by venue id)
        MvcResult rep = auth(get("/api/reports/utilization").param("from", tomorrow.minusDays(30).toString())
                        .param("to", tomorrow.plusDays(1).toString()), teacher)
                .andExpect(status().isOk())
                .andReturn();
        JsonNode repData = objectMapper.readTree(rep.getResponse().getContentAsString()).get("data");
        JsonNode createdVenueStat = null;
        for (JsonNode e : repData) {
            if (e.get("venueId").asLong() == venueId) {
                createdVenueStat = e;
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertNotNull(createdVenueStat, "created venue must appear in utilization report");
        org.junit.jupiter.api.Assertions.assertTrue(createdVenueStat.get("total").asLong() >= 1,
                "created venue should have at least one booking counted");

        // student cancels own booking
        auth(post("/api/bookings/" + b1Data.get("id").asLong() + "/cancel"), student)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("token").asText();
    }

    private org.springframework.test.web.servlet.ResultActions auth(MockHttpServletRequestBuilder builder, String token) throws Exception {
        return mockMvc.perform(builder.header("Authorization", "Bearer " + token));
    }
}
