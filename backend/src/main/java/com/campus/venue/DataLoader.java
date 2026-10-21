package com.campus.venue;

import com.campus.venue.common.Role;
import com.campus.venue.common.Status;
import com.campus.venue.domain.Booking;
import com.campus.venue.domain.BookingPurpose;
import com.campus.venue.domain.BookingStatus;
import com.campus.venue.domain.Club;
import com.campus.venue.domain.Equipment;
import com.campus.venue.domain.SchoolClass;
import com.campus.venue.domain.User;
import com.campus.venue.domain.Venue;
import com.campus.venue.domain.VenueSchedule;
import com.campus.venue.domain.VenueStatus;
import com.campus.venue.domain.VenueType;
import com.campus.venue.repository.BookingRepository;
import com.campus.venue.repository.ClubRepository;
import com.campus.venue.repository.EquipmentRepository;
import com.campus.venue.repository.SchoolClassRepository;
import com.campus.venue.repository.UserRepository;
import com.campus.venue.repository.VenueRepository;
import com.campus.venue.repository.VenueScheduleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
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
    private final BookingRepository bookings;
    private final SchoolClassRepository classes;
    private final ClubRepository clubs;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository users, VenueRepository venues, VenueScheduleRepository schedules,
                      EquipmentRepository equipment, BookingRepository bookings,
                      SchoolClassRepository classes, ClubRepository clubs, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.venues = venues;
        this.schedules = schedules;
        this.equipment = equipment;
        this.bookings = bookings;
        this.classes = classes;
        this.clubs = clubs;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }

        User admin = user("admin", "admin123", "Dana Whitfield", Role.ADMIN, null);
        User teacher = user("teacher", "teacher123", "Mei Lin", Role.TEACHER, null);

        SchoolClass tenA = classes.save(SchoolClass.builder()
                .name("Grade 10A").grade("Grade 10").headTeacherId(teacher.getId()).build());
        SchoolClass tenB = classes.save(SchoolClass.builder()
                .name("Grade 10B").grade("Grade 10").headTeacherId(teacher.getId()).build());
        SchoolClass elevenC = classes.save(SchoolClass.builder()
                .name("Grade 11C").grade("Grade 11").headTeacherId(teacher.getId()).build());

        User amy = user("student", "student123", "Amy Chen", Role.STUDENT, tenA.getId());
        User jordan = user("jordan", "student123", "Jordan Pike", Role.STUDENT, tenB.getId());
        User nina = user("nina", "student123", "Nina Rahman", Role.STUDENT, elevenC.getId());

        Club badmintonClub = clubs.save(Club.builder()
                .name("Badminton Club").type("Sport").leaderUserId(jordan.getId()).build());
        clubs.save(Club.builder()
                .name("Tennis Academy").type("Sport").leaderUserId(amy.getId()).build());

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

        seedBookings(amy, jordan, nina, badmintonClub, tenB, badminton, squash, tennisIndoor,
                tennisOutdoor, football, volleyball, teacher);
    }

    /**
     * A week of history and a couple of requests in the approval queue, so a fresh
     * checkout has something to look at instead of five empty screens.
     */
    private void seedBookings(User amy, User jordan, User nina, Club badmintonClub, SchoolClass tenB,
                              Venue badminton, Venue squash, Venue tennisIndoor, Venue tennisOutdoor,
                              Venue football, Venue volleyball, User teacher) {
        LocalDate today = LocalDate.now();

        booking(amy, badminton, today, 17, 18, BookingPurpose.FREE_PLAY, 2, BookingStatus.APPROVED);
        booking(amy, tennisIndoor, today.plusDays(1), 16, 17, BookingPurpose.FREE_PLAY, 2, BookingStatus.APPROVED);
        booking(amy, badminton, today.minusDays(2), 10, 11, BookingPurpose.FREE_PLAY, 2, BookingStatus.COMPLETED);

        Booking club = booking(jordan, football, today.plusDays(1), 15, 17,
                BookingPurpose.CLUB_TRAINING, 18, BookingStatus.PENDING);
        club.setClubId(badmintonClub.getId());

        booking(nina, squash, today.plusDays(2), 12, 13, BookingPurpose.EXAM, 2, BookingStatus.PENDING);

        Booking rejected = booking(jordan, tennisIndoor, today.minusDays(1), 18, 19,
                BookingPurpose.PE_CLASS, 12, BookingStatus.REJECTED);
        rejected.setClassId(tenB.getId());
        rejected.setRejectReason("The hall is set up for the music concert that evening.");
        rejected.setApprovedBy(teacher.getId());

        booking(amy, tennisOutdoor, today.minusDays(4), 9, 10, BookingPurpose.FREE_PLAY, 1, BookingStatus.NO_SHOW);
        booking(amy, volleyball, today.plusDays(3), 10, 11, BookingPurpose.FREE_PLAY, 6, BookingStatus.CANCELLED);
    }

    private Booking booking(User owner, Venue venue, LocalDate date, int from, int to,
                            BookingPurpose purpose, int people, BookingStatus status) {
        boolean decided = status != BookingStatus.PENDING;
        return bookings.save(Booking.builder()
                .userId(owner.getId())
                .venueId(venue.getId())
                .bookingDate(date)
                .startTime(LocalTime.of(from, 0))
                .endTime(LocalTime.of(to, 0))
                .purpose(purpose)
                .attendeesCount(people)
                .status(status)
                .approvedBy(status == BookingStatus.APPROVED || status == BookingStatus.COMPLETED ? owner.getId() : null)
                .approvedAt(decided ? Instant.now() : null)
                .checkInAt(status == BookingStatus.COMPLETED ? Instant.now() : null)
                .build());
    }

    private User user(String username, String rawPassword, String realName, Role role, Long classId) {
        return users.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .realName(realName)
                .role(role)
                .classId(classId)
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
