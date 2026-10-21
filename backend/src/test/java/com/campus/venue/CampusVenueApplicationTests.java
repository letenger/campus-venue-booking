package com.campus.venue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class CampusVenueApplicationTests {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper json;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(wac).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    void contextLoads() {
    }

    /**
     * The whole story in one run: an admin adds a venue, a student books it twice
     * (once free play, once a club slot that needs a decision), a teacher approves,
     * the reports count it, and the student cancels.
     */
    @Test
    void fullBookingFlow() throws Exception {
        String admin = login("admin", "admin123");
        String student = login("student", "student123");
        String teacher = login("teacher", "teacher123");

        String venueBody = json.writeValueAsString(Map.of(
                "name", "Test Badminton Court",
                "type", "BADMINTON",
                "indoor", true,
                "capacity", 4,
                "openTime", "08:00",
                "closeTime", "21:00",
                "status", "OPEN"));
        MvcResult created = mvc.perform(withToken(post("/api/venues")
                        .contentType(MediaType.APPLICATION_JSON).content(venueBody), admin))
                .andExpect(status().isOk())
                .andReturn();
        long venueId = body(created).get("id").asLong();

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        mvc.perform(withToken(get("/api/venues/" + venueId + "/availability")
                .param("from", tomorrow.toString()).param("to", tomorrow.toString()), student))
                .andExpect(status().isOk());

        // free play for two people is small enough to be trusted
        String freePlay = json.writeValueAsString(Map.of(
                "venueId", venueId,
                "bookingDate", tomorrow.toString(),
                "startTime", "10:00",
                "endTime", "11:00",
                "purpose", "FREE_PLAY",
                "attendeesCount", 2));
        MvcResult first = mvc.perform(withToken(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON).content(freePlay), student))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode firstBooking = body(first);
        assertEquals("APPROVED", firstBooking.get("status").asText());

        mvc.perform(withToken(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON).content(freePlay), student))
                .andExpect(status().isConflict());

        // a club training slot has to wait for a teacher
        String clubTraining = json.writeValueAsString(Map.of(
                "venueId", venueId,
                "bookingDate", tomorrow.toString(),
                "startTime", "12:00",
                "endTime", "13:00",
                "purpose", "CLUB_TRAINING",
                "attendeesCount", 4));
        MvcResult second = mvc.perform(withToken(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON).content(clubTraining), student))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode clubBooking = body(second);
        assertEquals("PENDING", clubBooking.get("status").asText());

        mvc.perform(withToken(post("/api/bookings/" + clubBooking.get("id").asLong() + "/approve"), teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // the report is ordered by venue id, so find our venue instead of assuming position
        MvcResult report = mvc.perform(withToken(get("/api/reports/utilization")
                        .param("from", tomorrow.minusDays(30).toString())
                        .param("to", tomorrow.plusDays(1).toString()), teacher))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode venueStat = null;
        for (JsonNode row : body(report)) {
            if (row.get("venueId").asLong() == venueId) {
                venueStat = row;
                break;
            }
        }
        assertNotNull(venueStat, "the new venue must show up in the utilisation report");
        assertTrue(venueStat.get("total").asLong() >= 1, "the approved booking must be counted");

        mvc.perform(withToken(post("/api/bookings/" + firstBooking.get("id").asLong() + "/cancel"), student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    private String login(String username, String password) throws Exception {
        String credentials = json.writeValueAsString(Map.of("username", username, "password", password));
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).get("token").asText();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
