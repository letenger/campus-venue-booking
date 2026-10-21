package com.campus.venue.service;

import com.campus.venue.common.BadRequestException;
import com.campus.venue.common.ConflictException;
import com.campus.venue.common.ForbiddenException;
import com.campus.venue.common.ResourceNotFoundException;
import com.campus.venue.common.Role;
import com.campus.venue.common.Status;
import com.campus.venue.domain.Booking;
import com.campus.venue.domain.BookingPurpose;
import com.campus.venue.domain.BookingStatus;
import com.campus.venue.domain.Equipment;
import com.campus.venue.domain.EquipmentBorrow;
import com.campus.venue.domain.NotificationType;
import com.campus.venue.domain.User;
import com.campus.venue.domain.Venue;
import com.campus.venue.domain.VenueStatus;
import com.campus.venue.domain.VenueType;
import com.campus.venue.dto.BookingDto;
import com.campus.venue.dto.BookingRequest;
import com.campus.venue.repository.BookingRepository;
import com.campus.venue.repository.EquipmentBorrowRepository;
import com.campus.venue.repository.EquipmentRepository;
import com.campus.venue.repository.UserRepository;
import com.campus.venue.repository.VenueRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BookingService}: conflict detection, the two-track approval rule,
 * decision flow, check-in / no-show handling, the nightly sweep and cancellation.
 * All repositories and collaborators are mocked, so no Spring context is started.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest extends ServiceTestSupport {

    private static final long STUDENT_ID = 42L;
    private static final long OTHER_STUDENT_ID = 99L;
    private static final long TEACHER_ID = 7L;
    private static final long VENUE_ID = 1L;
    private static final long BOOKING_ID = 100L;
    private static final long EQUIPMENT_ID = 9L;
    private static final LocalDate BOOKING_DATE = LocalDate.of(2026, 10, 26);

    @Mock
    private BookingRepository bookings;

    @Mock
    private VenueRepository venues;

    @Mock
    private UserRepository users;

    @Mock
    private EquipmentRepository equipment;

    @Mock
    private EquipmentBorrowRepository borrows;

    @Mock
    private NotificationService notifications;

    @Mock
    private AuditService audit;

    @InjectMocks
    private BookingService service;

    // ---------------------------------------------------------------- fixtures

    private Venue openVenue() {
        return Venue.builder()
                .id(VENUE_ID)
                .name("Badminton Court")
                .type(VenueType.BADMINTON)
                .capacity(20)
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(21, 0))
                .status(VenueStatus.OPEN)
                .build();
    }

    private BookingRequest request(LocalTime start, LocalTime end,
                                   BookingPurpose purpose, int attendees) {
        BookingRequest req = new BookingRequest();
        req.setVenueId(VENUE_ID);
        req.setBookingDate(BOOKING_DATE);
        req.setStartTime(start);
        req.setEndTime(end);
        req.setPurpose(purpose);
        req.setAttendeesCount(attendees);
        return req;
    }

    private Booking existingBooking(long id, long ownerId, BookingStatus status) {
        return Booking.builder()
                .id(id)
                .userId(ownerId)
                .venueId(VENUE_ID)
                .bookingDate(BOOKING_DATE)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .purpose(BookingPurpose.FREE_PLAY)
                .attendeesCount(2)
                .status(status)
                .build();
    }

    private void givenOpenVenue() {
        when(venues.findById(VENUE_ID)).thenReturn(Optional.of(openVenue()));
    }

    private void givenStudent() {
        User student = User.builder()
                .id(STUDENT_ID)
                .username("sam")
                .realName("Sam Carter")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    }

    private void givenTeachers() {
        User teacher = User.builder()
                .id(TEACHER_ID)
                .username("msLee")
                .role(Role.TEACHER)
                .status(Status.ACTIVE)
                .build();
        when(users.findByRole(Role.TEACHER)).thenReturn(List.of(teacher));
    }

    private void givenNoConflictingBookings() {
        when(bookings.findOverlapping(eq(VENUE_ID), eq(BOOKING_DATE), any(), any()))
                .thenReturn(List.of());
    }

    /**
     * Mirrors the findOverlapping JPQL contract: same venue and date, overlapping
     * window, and not already rejected or cancelled. This lets the overlap boundary
     * rules be exercised without a database.
     */
    private void givenExistingBookings(Booking... existing) {
        when(bookings.findOverlapping(eq(VENUE_ID), eq(BOOKING_DATE), any(), any()))
                .thenAnswer(invocation -> {
                    LocalTime start = invocation.getArgument(2);
                    LocalTime end = invocation.getArgument(3);
                    return List.of(existing).stream()
                            .filter(b -> b.getVenueId().equals(VENUE_ID))
                            .filter(b -> b.getBookingDate().equals(BOOKING_DATE))
                            .filter(b -> b.getStatus() != BookingStatus.REJECTED)
                            .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                            .filter(b -> b.getStartTime().isBefore(end))
                            .filter(b -> b.getEndTime().isAfter(start))
                            .toList();
                });
    }

    private void givenSaveAssignsId() {
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(BOOKING_ID);
            return saved;
        });
    }

    // ------------------------------------------------------------------ create

    @Nested
    class Create {

        @Test
        void shouldAutoApproveSmallFreePlay() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenNoConflictingBookings();
            givenSaveAssignsId();
            givenStudent();

            BookingDto created = service.create(
                    request(LocalTime.of(10, 0), LocalTime.of(11, 0),
                            BookingPurpose.FREE_PLAY, 2), STUDENT_ID);

            assertThat(created.status).isEqualTo(BookingStatus.APPROVED);
            assertThat(created.approvedBy).isEqualTo(STUDENT_ID);
            assertThat(created.approvedAt).isNotNull();
            verify(notifications).notify(eq(STUDENT_ID), eq(NotificationType.APPROVED),
                    eq("Booking confirmed"), any(), eq(BOOKING_ID));
            // trusted bookings never bother the teachers
            verify(users, never()).findByRole(any());
        }

        @Test
        void shouldQueueGroupOfTenForApproval() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenNoConflictingBookings();
            givenSaveAssignsId();
            givenStudent();
            givenTeachers();

            BookingDto created = service.create(
                    request(LocalTime.of(10, 0), LocalTime.of(11, 0),
                            BookingPurpose.FREE_PLAY, 10), STUDENT_ID);

            assertThat(created.status).isEqualTo(BookingStatus.PENDING);
            assertThat(created.approvedBy).isNull();
            assertThat(created.approvedAt).isNull();
            verify(notifications).notify(eq(STUDENT_ID), eq(NotificationType.BOOKING_CREATED),
                    any(), any(), eq(BOOKING_ID));
            verify(notifications).notify(eq(TEACHER_ID), eq(NotificationType.APPROVAL_REQUIRED),
                    any(), any(), eq(BOOKING_ID));
        }

        @Test
        void shouldAutoApproveGroupOfNine() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenNoConflictingBookings();
            givenSaveAssignsId();
            givenStudent();

            BookingDto created = service.create(
                    request(LocalTime.of(10, 0), LocalTime.of(11, 0),
                            BookingPurpose.FREE_PLAY, 9), STUDENT_ID);

            assertThat(created.status).isEqualTo(BookingStatus.APPROVED);
        }

        @ParameterizedTest
        @EnumSource(value = BookingPurpose.class,
                names = {"PE_CLASS", "CLUB_TRAINING", "EVENT", "EXAM"})
        void shouldRequireApprovalForManagedPurposes(BookingPurpose purpose) {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenNoConflictingBookings();
            givenSaveAssignsId();
            givenStudent();
            givenTeachers();

            BookingDto created = service.create(
                    request(LocalTime.of(10, 0), LocalTime.of(11, 0), purpose, 4), STUDENT_ID);

            assertThat(created.status).isEqualTo(BookingStatus.PENDING);
        }

        @ParameterizedTest
        @EnumSource(value = BookingPurpose.class, names = {"FREE_PLAY", "OTHER"})
        void shouldAutoApproveCasualPurposes(BookingPurpose purpose) {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenNoConflictingBookings();
            givenSaveAssignsId();
            givenStudent();

            BookingDto created = service.create(
                    request(LocalTime.of(10, 0), LocalTime.of(11, 0), purpose, 4), STUDENT_ID);

            assertThat(created.status).isEqualTo(BookingStatus.APPROVED);
        }

        @Test
        void shouldRequireApprovalWhenClassIsSet() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenNoConflictingBookings();
            givenSaveAssignsId();
            givenStudent();
            givenTeachers();

            BookingRequest req = request(LocalTime.of(10, 0), LocalTime.of(11, 0),
                    BookingPurpose.FREE_PLAY, 2);
            req.setClassId(5L);

            assertThat(service.create(req, STUDENT_ID).status).isEqualTo(BookingStatus.PENDING);
        }

        @Test
        void shouldRequireApprovalWhenClubIsSet() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenNoConflictingBookings();
            givenSaveAssignsId();
            givenStudent();
            givenTeachers();

            BookingRequest req = request(LocalTime.of(10, 0), LocalTime.of(11, 0),
                    BookingPurpose.FREE_PLAY, 2);
            req.setClubId(3L);

            assertThat(service.create(req, STUDENT_ID).status).isEqualTo(BookingStatus.PENDING);
        }

        @Test
        void throwsConflictExceptionWhenSlotsOverlap() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenExistingBookings(existingBooking(BOOKING_ID, OTHER_STUDENT_ID,
                    BookingStatus.APPROVED));

            assertThatThrownBy(() -> service.create(
                    request(LocalTime.of(10, 30), LocalTime.of(11, 30),
                            BookingPurpose.FREE_PLAY, 2), STUDENT_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("That slot is already booked");
            verify(bookings, never()).save(any());
        }

        @Test
        void shouldRejectWhenNewBookingContainsExistingOne() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenExistingBookings(existingBooking(BOOKING_ID, OTHER_STUDENT_ID,
                    BookingStatus.APPROVED));

            assertThatThrownBy(() -> service.create(
                    request(LocalTime.of(9, 0), LocalTime.of(13, 0),
                            BookingPurpose.FREE_PLAY, 2), STUDENT_ID))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void shouldTreatExactlyAdjacentSlotsAsFree() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenExistingBookings(existingBooking(BOOKING_ID, OTHER_STUDENT_ID,
                    BookingStatus.APPROVED));
            givenSaveAssignsId();
            givenStudent();

            // existing booking runs 10:00-11:00; 11:00-12:00 only touches it at the border
            BookingDto created = service.create(
                    request(LocalTime.of(11, 0), LocalTime.of(12, 0),
                            BookingPurpose.FREE_PLAY, 2), STUDENT_ID);

            assertThat(created.status).isEqualTo(BookingStatus.APPROVED);
        }

        @ParameterizedTest
        @EnumSource(value = BookingStatus.class,
                names = {"REJECTED", "CANCELLED"})
        void shouldNotConflictWithInactiveBookings(BookingStatus status) {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenExistingBookings(existingBooking(BOOKING_ID, OTHER_STUDENT_ID, status));
            givenSaveAssignsId();
            givenStudent();

            BookingDto created = service.create(
                    request(LocalTime.of(10, 0), LocalTime.of(11, 0),
                            BookingPurpose.FREE_PLAY, 2), STUDENT_ID);

            assertThat(created.status).isEqualTo(BookingStatus.APPROVED);
        }

        @Test
        void throwsBadRequestExceptionWhenEndTimeIsNotAfterStartTime() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();

            assertThatThrownBy(() -> service.create(
                    request(LocalTime.of(11, 0), LocalTime.of(10, 0),
                            BookingPurpose.FREE_PLAY, 2), STUDENT_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("End time must be after start time");
        }

        @Test
        void throwsBadRequestExceptionWhenStartingBeforeOpening() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();

            assertThatThrownBy(() -> service.create(
                    request(LocalTime.of(7, 0), LocalTime.of(9, 0),
                            BookingPurpose.FREE_PLAY, 2), STUDENT_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Requested time is outside the opening hours");
        }

        @Test
        void throwsBadRequestExceptionWhenEndingAfterClosing() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();

            assertThatThrownBy(() -> service.create(
                    request(LocalTime.of(20, 0), LocalTime.of(22, 0),
                            BookingPurpose.FREE_PLAY, 2), STUDENT_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Requested time is outside the opening hours");
        }

        @Test
        void throwsBadRequestExceptionWhenAttendeesExceedCapacity() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();

            assertThatThrownBy(() -> service.create(
                    request(LocalTime.of(10, 0), LocalTime.of(11, 0),
                            BookingPurpose.FREE_PLAY, 21), STUDENT_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("That many people do not fit in this venue");
        }

        @ParameterizedTest
        @EnumSource(value = VenueStatus.class, names = {"MAINTENANCE", "CLOSED"})
        void throwsBadRequestExceptionWhenVenueIsNotOpen(VenueStatus status) {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            Venue venue = openVenue();
            venue.setStatus(status);
            when(venues.findById(VENUE_ID)).thenReturn(Optional.of(venue));

            assertThatThrownBy(() -> service.create(
                    request(LocalTime.of(10, 0), LocalTime.of(11, 0),
                            BookingPurpose.FREE_PLAY, 2), STUDENT_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("This venue is not open for booking");
        }

        @Test
        void throwsResourceNotFoundExceptionWhenVenueMissing() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            when(venues.findById(VENUE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(
                    request(LocalTime.of(10, 0), LocalTime.of(11, 0),
                            BookingPurpose.FREE_PLAY, 2), STUDENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Venue not found");
        }

        @Test
        void shouldAcceptCapacitySizedGroupButRequireApproval() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenOpenVenue();
            givenNoConflictingBookings();
            givenSaveAssignsId();
            givenStudent();
            givenTeachers();

            // exactly at capacity is accepted, but a group that size always waits for a teacher
            BookingDto created = service.create(
                    request(LocalTime.of(10, 0), LocalTime.of(11, 0),
                            BookingPurpose.FREE_PLAY, 20), STUDENT_ID);

            assertThat(created.status).isEqualTo(BookingStatus.PENDING);
            verify(notifications).notify(eq(TEACHER_ID), eq(NotificationType.APPROVAL_REQUIRED),
                    any(), any(), eq(BOOKING_ID));
        }
    }

    // ------------------------------------------------------------------ cancel

    @Nested
    class Cancel {

        @Test
        void shouldCancelApprovedBookingAndReleaseTheSlot() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID,
                            BookingStatus.APPROVED)));
            givenSaveAssignsId();
            givenOpenVenue();
            givenStudent();
            when(borrows.findByBookingId(BOOKING_ID)).thenReturn(List.of());

            BookingDto cancelled = service.cancel(BOOKING_ID);

            assertThat(cancelled.status).isEqualTo(BookingStatus.CANCELLED);
            assertThat(cancelled.cancelledAt).isNotNull();
            assertThat(cancelled.canCancel).isFalse();
            verify(notifications).notify(eq(STUDENT_ID), eq(NotificationType.CANCELLED),
                    any(), any(), eq(BOOKING_ID));
            verify(audit).record(eq(STUDENT_ID), eq("CANCEL_BOOKING"), eq("BOOKING"),
                    eq(BOOKING_ID), isNull());
        }

        @Test
        void shouldGiveBackBorrowedEquipmentOnCancel() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID,
                            BookingStatus.APPROVED)));
            givenSaveAssignsId();
            givenOpenVenue();
            givenStudent();
            EquipmentBorrow borrow = EquipmentBorrow.builder()
                    .id(5L)
                    .bookingId(BOOKING_ID)
                    .equipmentId(EQUIPMENT_ID)
                    .qty(2)
                    .returned(false)
                    .build();
            when(borrows.findByBookingId(BOOKING_ID)).thenReturn(List.of(borrow));
            Equipment balls = Equipment.builder()
                    .id(EQUIPMENT_ID)
                    .total(5)
                    .available(4)
                    .build();
            when(equipment.findById(EQUIPMENT_ID)).thenReturn(Optional.of(balls));
            when(equipment.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

            service.cancel(BOOKING_ID);

            ArgumentCaptor<Equipment> saved = ArgumentCaptor.forClass(Equipment.class);
            verify(equipment).save(saved.capture());
            // 4 available + 2 returned, but never above the total stock of 5
            assertThat(saved.getValue().getAvailable()).isEqualTo(5);
            assertThat(borrow.isReturned()).isTrue();
            verify(borrows).save(borrow);
        }

        @Test
        void throwsForbiddenExceptionWhenCancellingSomeoneElsesBooking() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, OTHER_STUDENT_ID,
                            BookingStatus.APPROVED)));

            assertThatThrownBy(() -> service.cancel(BOOKING_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only the owner can cancel this booking");
        }

        @ParameterizedTest
        @EnumSource(value = BookingStatus.class,
                names = {"REJECTED", "CANCELLED", "COMPLETED", "NO_SHOW"})
        void throwsBadRequestExceptionWhenBookingCanNoLongerBeCancelled(BookingStatus status) {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID, status)));

            assertThatThrownBy(() -> service.cancel(BOOKING_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("This booking can no longer be cancelled");
        }
    }

    // ----------------------------------------------------------- decide (approve/reject)

    @Nested
    class Decide {

        private void givenPendingBooking() {
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID,
                            BookingStatus.PENDING)));
        }

        @Test
        void shouldApprovePendingBookingAsTeacher() {
            givenCurrentUser(TEACHER_ID, Role.TEACHER);
            givenPendingBooking();
            givenSaveAssignsId();
            givenOpenVenue();
            givenStudent();

            BookingDto approved = service.approve(BOOKING_ID);

            assertThat(approved.status).isEqualTo(BookingStatus.APPROVED);
            assertThat(approved.approvedBy).isEqualTo(TEACHER_ID);
            assertThat(approved.approvedAt).isNotNull();
            verify(notifications).notify(eq(STUDENT_ID), eq(NotificationType.APPROVED),
                    eq("Booking approved"), any(), eq(BOOKING_ID));
            verify(audit).record(eq(TEACHER_ID), eq("APPROVE_BOOKING"), eq("BOOKING"),
                    eq(BOOKING_ID), isNull());
        }

        @Test
        void shouldRejectPendingBookingWithReasonVisibleToStudent() {
            givenCurrentUser(TEACHER_ID, Role.TEACHER);
            givenPendingBooking();
            givenSaveAssignsId();
            givenOpenVenue();
            givenStudent();

            BookingDto rejected = service.reject(BOOKING_ID, "Reserved for the exam");

            assertThat(rejected.status).isEqualTo(BookingStatus.REJECTED);
            assertThat(rejected.rejectReason).isEqualTo("Reserved for the exam");
            verify(notifications).notify(eq(STUDENT_ID), eq(NotificationType.REJECTED),
                    any(), eq("Reason: Reserved for the exam"), eq(BOOKING_ID));
            verify(audit).record(eq(TEACHER_ID), eq("REJECT_BOOKING"), eq("BOOKING"),
                    eq(BOOKING_ID), eq("Reserved for the exam"));
        }

        @Test
        void throwsForbiddenExceptionWhenStudentApproves() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenPendingBooking();

            assertThatThrownBy(() -> service.approve(BOOKING_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only a teacher or an admin can do this");
            verify(bookings, never()).save(any());
        }

        @Test
        void throwsBadRequestExceptionWhenApprovingAlreadyDecidedBooking() {
            givenCurrentUser(TEACHER_ID, Role.TEACHER);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID,
                            BookingStatus.APPROVED)));

            assertThatThrownBy(() -> service.approve(BOOKING_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Only pending bookings can be decided");
        }

        @Test
        void throwsBadRequestExceptionWhenRejectingAlreadyRejectedBooking() {
            givenCurrentUser(TEACHER_ID, Role.TEACHER);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID,
                            BookingStatus.REJECTED)));

            assertThatThrownBy(() -> service.reject(BOOKING_ID, "Duplicate decision"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Only pending bookings can be decided");
        }
    }

    // ------------------------------------------------------------ check-in / no-show

    @Nested
    class Attendance {

        private void givenApprovedBooking() {
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID,
                            BookingStatus.APPROVED)));
        }

        @Test
        void shouldCheckInApprovedBooking() {
            givenCurrentUser(TEACHER_ID, Role.TEACHER);
            givenApprovedBooking();
            givenSaveAssignsId();
            givenOpenVenue();
            givenStudent();

            BookingDto checkedIn = service.checkIn(BOOKING_ID);

            assertThat(checkedIn.status).isEqualTo(BookingStatus.COMPLETED);
            assertThat(checkedIn.checkInAt).isNotNull();
            verify(audit).record(eq(TEACHER_ID), eq("CHECKIN_BOOKING"), eq("BOOKING"),
                    eq(BOOKING_ID), isNull());
        }

        @Test
        void throwsBadRequestExceptionWhenCheckingInPendingBooking() {
            givenCurrentUser(TEACHER_ID, Role.TEACHER);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID,
                            BookingStatus.PENDING)));

            assertThatThrownBy(() -> service.checkIn(BOOKING_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Only approved bookings can be checked in");
        }

        @Test
        void throwsForbiddenExceptionWhenStudentChecksIn() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            givenApprovedBooking();

            assertThatThrownBy(() -> service.checkIn(BOOKING_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only a teacher or an admin can do this");
        }

        @Test
        void shouldMarkApprovedBookingAsNoShow() {
            givenCurrentUser(TEACHER_ID, Role.TEACHER);
            givenApprovedBooking();
            givenSaveAssignsId();
            givenOpenVenue();
            givenStudent();

            BookingDto marked = service.markNoShow(BOOKING_ID);

            assertThat(marked.status).isEqualTo(BookingStatus.NO_SHOW);
            verify(notifications).notify(eq(STUDENT_ID), eq(NotificationType.NO_SHOW),
                    any(), any(), eq(BOOKING_ID));
        }

        @Test
        void throwsBadRequestExceptionWhenMarkingPendingBookingAsNoShow() {
            givenCurrentUser(TEACHER_ID, Role.TEACHER);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID,
                            BookingStatus.PENDING)));

            assertThatThrownBy(() -> service.markNoShow(BOOKING_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Only approved bookings can be marked no-show");
        }
    }

    // -------------------------------------------------------------------- sweep

    @Nested
    class Sweep {

        @Test
        void shouldSweepOverdueApprovedBookingsToNoShow() {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Booking overdue = Booking.builder()
                    .id(BOOKING_ID)
                    .userId(STUDENT_ID)
                    .venueId(VENUE_ID)
                    .bookingDate(yesterday)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 0))
                    .purpose(BookingPurpose.FREE_PLAY)
                    .attendeesCount(2)
                    .status(BookingStatus.APPROVED)
                    .build();
            when(bookings.findByStatus(BookingStatus.APPROVED)).thenReturn(List.of(overdue));

            service.sweepNoShow();

            assertThat(overdue.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
            verify(notifications).notify(eq(STUDENT_ID), eq(NotificationType.NO_SHOW),
                    any(), any(), eq(BOOKING_ID));
            verify(audit).record(isNull(), eq("AUTO_NOSHOW"), eq("BOOKING"), eq(BOOKING_ID), isNull());
        }

        @Test
        void shouldLeaveBookingsThatHaveNotEndedYetUntouched() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            Booking stillRunning = Booking.builder()
                    .id(BOOKING_ID)
                    .userId(STUDENT_ID)
                    .venueId(VENUE_ID)
                    .bookingDate(tomorrow)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 0))
                    .purpose(BookingPurpose.FREE_PLAY)
                    .attendeesCount(2)
                    .status(BookingStatus.APPROVED)
                    .build();
            when(bookings.findByStatus(BookingStatus.APPROVED)).thenReturn(List.of(stillRunning));

            service.sweepNoShow();

            assertThat(stillRunning.getStatus()).isEqualTo(BookingStatus.APPROVED);
            verify(bookings, never()).save(any());
            verifyNoInteractions(notifications);
        }
    }

    // ------------------------------------------------------------- read / update

    @Nested
    class ReadAndUpdate {

        @Test
        void throwsForbiddenExceptionWhenStudentReadsSomeoneElsesBooking() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, OTHER_STUDENT_ID,
                            BookingStatus.APPROVED)));

            assertThatThrownBy(() -> service.get(BOOKING_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("You can only read your own bookings");
        }

        @Test
        void shouldLetTeacherReadAnyBooking() {
            givenCurrentUser(TEACHER_ID, Role.TEACHER);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID,
                            BookingStatus.APPROVED)));
            givenOpenVenue();
            givenStudent();

            BookingDto read = service.get(BOOKING_ID);

            assertThat(read.venueName).isEqualTo("Badminton Court");
            assertThat(read.userName).isEqualTo("Sam Carter");
        }

        @Test
        void throwsResourceNotFoundExceptionWhenBookingMissing() {
            givenCurrentUser(TEACHER_ID, Role.TEACHER);
            when(bookings.findById(BOOKING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(BOOKING_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Booking not found");
        }

        @Test
        void shouldForceStudentsToSeeOnlyTheirOwnRows() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            when(bookings.search(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            service.list(null, null, null, null, false, Pageable.unpaged());

            ArgumentCaptor<Boolean> mine = ArgumentCaptor.forClass(Boolean.class);
            verify(bookings).search(isNull(), isNull(), isNull(), isNull(),
                    mine.capture(), eq(STUDENT_ID), any(Pageable.class));
            assertThat(mine.getValue()).isTrue();
        }

        @Test
        void shouldHonourTheMineFilterForTeachers() {
            givenCurrentUser(TEACHER_ID, Role.TEACHER);
            when(bookings.search(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            service.list(null, null, null, null, false, Pageable.unpaged());

            ArgumentCaptor<Boolean> mine = ArgumentCaptor.forClass(Boolean.class);
            verify(bookings).search(isNull(), isNull(), isNull(), isNull(),
                    mine.capture(), eq(TEACHER_ID), any(Pageable.class));
            assertThat(mine.getValue()).isFalse();
        }

        @Test
        void shouldUpdatePendingBookingByItsOwner() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID,
                            BookingStatus.PENDING)));
            givenSaveAssignsId();
            givenOpenVenue();
            givenStudent();

            BookingDto updated = service.update(BOOKING_ID,
                    request(LocalTime.of(14, 0), LocalTime.of(15, 0),
                            BookingPurpose.FREE_PLAY, 3));

            assertThat(updated.startTime).isEqualTo(LocalTime.of(14, 0));
            assertThat(updated.endTime).isEqualTo(LocalTime.of(15, 0));
            assertThat(updated.attendeesCount).isEqualTo(3);
            verify(audit).record(eq(STUDENT_ID), eq("UPDATE_BOOKING"), eq("BOOKING"),
                    eq(BOOKING_ID), isNull());
        }

        @Test
        void throwsBadRequestExceptionWhenUpdatingDecidedBooking() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, STUDENT_ID,
                            BookingStatus.APPROVED)));

            assertThatThrownBy(() -> service.update(BOOKING_ID,
                    request(LocalTime.of(14, 0), LocalTime.of(15, 0),
                            BookingPurpose.FREE_PLAY, 3)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Only pending bookings can be edited");
        }

        @Test
        void throwsForbiddenExceptionWhenAnotherStudentUpdates() {
            givenCurrentUser(STUDENT_ID, Role.STUDENT);
            when(bookings.findById(BOOKING_ID))
                    .thenReturn(Optional.of(existingBooking(BOOKING_ID, OTHER_STUDENT_ID,
                            BookingStatus.PENDING)));

            assertThatThrownBy(() -> service.update(BOOKING_ID,
                    request(LocalTime.of(14, 0), LocalTime.of(15, 0),
                            BookingPurpose.FREE_PLAY, 3)))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Only the owner can edit this booking");
        }
    }
}
