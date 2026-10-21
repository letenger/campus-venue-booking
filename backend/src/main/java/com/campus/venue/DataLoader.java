package com.campus.venue;

import com.campus.venue.common.Role;
import com.campus.venue.common.Status;
import com.campus.venue.domain.Equipment;
import com.campus.venue.domain.Venue;
import com.campus.venue.domain.VenueSchedule;
import com.campus.venue.domain.VenueStatus;
import com.campus.venue.domain.VenueType;
import com.campus.venue.domain.User;
import com.campus.venue.repository.EquipmentRepository;
import com.campus.venue.repository.UserRepository;
import com.campus.venue.repository.VenueRepository;
import com.campus.venue.repository.VenueScheduleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Seeds one account per role, six venues, their weekly timetable and a few equipment pools.
 * Runs only when the user table is empty, so it is safe on every start.
 *
 * <p>Logins: admin/admin123, teacher/teacher123, student/student123.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository users;
    private final VenueRepository venues;
    private final VenueScheduleRepository schedules;
    private final EquipmentRepository equipment;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository users, VenueRepository venues, VenueScheduleRepository schedules,
                      EquipmentRepository equipment, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.venues = venues;
        this.schedules = schedules;
        this.equipment = equipment;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }

        user("admin", "admin123", "Dana Whitfield", Role.ADMIN);
        user("teacher", "teacher123", "Mei Lin", Role.TEACHER);
        user("student", "student123", "Amy Chen", Role.STUDENT);

        Venue badminton = venue("Badminton Court 2", VenueType.BADMINTON, true, "Sports Hall", "Court 2", 4,
                LocalTime.of(8, 0), LocalTime.of(21, 0), "Non-marking shoes only");
        Venue squash = venue("Squash Court B1", VenueType.SQUASH, true, "Sports Hall", "Basement", 2,
                LocalTime.of(9, 0), LocalTime.of(22, 0), "Racket required, borrow from the desk");
        Venue tennisIndoor = venue("Indoor Tennis 1", VenueType.TENNIS_INDOOR, true, "Tennis Centre", "Hall A", 4,
                LocalTime.of(8, 0), LocalTime.of(22, 0), "Lights metered separately");
        Venue tennisOutdoor = venue("Outdoor Tennis 2", VenueType.TENNIS_OUTDOOR, false, "Tennis Centre", "Court 2", 4,
                LocalTime.of(7, 0), LocalTime.of(20, 0), "Closed when it rains");
        Venue football = venue("Football Pitch", VenueType.FOOTBALL, false, "Main Field", "Centre", 22,
                LocalTime.of(8, 0), LocalTime.of(19, 0), "Matches take priority over training");
        Venue volleyball = venue("Volleyball Court", VenueType.VOLLEYBALL, false, "Main Field", "East side", 12,
                LocalTime.of(8, 0), LocalTime.of(19, 0), "Used by PE classes most mornings");

        for (Venue v : List.of(badminton, squash, tennisIndoor, tennisOutdoor, football, volleyball)) {
            for (int day = DayOfWeek.MONDAY.getValue(); day <= DayOfWeek.FRIDAY.getValue(); day++) {
                weekly(v, day, v.getOpenTime(), v.getCloseTime());
            }
            weekly(v, DayOfWeek.SATURDAY.getValue(), LocalTime.of(9, 0), LocalTime.of(12, 0));
        }

        equipment.save(Equipment.builder()
                .name("Badminton racket").venueId(badminton.getId()).total(10).available(10).build());
        equipment.save(Equipment.builder()
                .name("Squash racket").venueId(squash.getId()).total(4).available(4).build());
        equipment.save(Equipment.builder()
                .name("Tennis ball tube").venueId(tennisIndoor.getId()).total(20).available(20).build());
    }

    private User user(String username, String rawPassword, String realName, Role role) {
        return users.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .realName(realName)
                .role(role)
                .status(Status.ACTIVE)
                .build());
    }

    private Venue venue(String name, VenueType type, boolean indoor, String building, String location,
                        int capacity, LocalTime open, LocalTime close, String rules) {
        return venues.save(Venue.builder()
                .name(name).type(type).indoor(indoor).building(building).location(location)
                .capacity(capacity).openTime(open).closeTime(close)
                .status(VenueStatus.OPEN).rules(rules).build());
    }

    private void weekly(Venue venue, int weekday, LocalTime from, LocalTime to) {
        schedules.save(VenueSchedule.builder()
                .venueId(venue.getId()).weekday(weekday).startTime(from).endTime(to).build());
    }
}
