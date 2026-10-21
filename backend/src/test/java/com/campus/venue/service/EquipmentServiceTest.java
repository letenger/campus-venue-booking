package com.campus.venue.service;

import com.campus.venue.common.BadRequestException;
import com.campus.venue.common.ForbiddenException;
import com.campus.venue.common.ResourceNotFoundException;
import com.campus.venue.common.Role;
import com.campus.venue.domain.Booking;
import com.campus.venue.domain.BookingPurpose;
import com.campus.venue.domain.BookingStatus;
import com.campus.venue.domain.Equipment;
import com.campus.venue.domain.EquipmentBorrow;
import com.campus.venue.dto.EquipmentBorrowRequest;
import com.campus.venue.dto.EquipmentDto;
import com.campus.venue.repository.BookingRepository;
import com.campus.venue.repository.EquipmentBorrowRepository;
import com.campus.venue.repository.EquipmentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EquipmentService}: borrowing against approved bookings only,
 * stock bookkeeping and the return path.
 */
@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest extends ServiceTestSupport {

    private static final long STUDENT_ID = 42L;
    private static final long OTHER_STUDENT_ID = 99L;
    private static final long TEACHER_ID = 7L;
    private static final long BOOKING_ID = 100L;
    private static final long EQUIPMENT_ID = 9L;
    private static final long BORROW_ID = 5L;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private EquipmentBorrowRepository equipmentBorrowRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private EquipmentService service;

    private Booking booking(long ownerId, BookingStatus status) {
        return Booking.builder()
                .id(BOOKING_ID)
                .userId(ownerId)
                .venueId(1L)
                .bookingDate(LocalDate.of(2026, 10, 26))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .purpose(BookingPurpose.FREE_PLAY)
                .attendeesCount(2)
                .status(status)
                .build();
    }

    private Equipment equipment(int total, int available) {
        return Equipment.builder()
                .id(EQUIPMENT_ID)
                .name("Basketball")
                .venueId(1L)
                .total(total)
                .available(available)
                .build();
    }

    private EquipmentBorrowRequest borrowRequest(int qty) {
        EquipmentBorrowRequest req = new EquipmentBorrowRequest();
        req.setBookingId(BOOKING_ID);
        req.setEquipmentId(EQUIPMENT_ID);
        req.setQty(qty);
        return req;
    }

    @Test
    void shouldBorrowForOwnApprovedBookingAndDecrementStock() {
        givenCurrentUser(STUDENT_ID, Role.STUDENT);
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(STUDENT_ID, BookingStatus.APPROVED)));
        when(equipmentRepository.findById(EQUIPMENT_ID))
                .thenReturn(Optional.of(equipment(10, 4)));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipmentDto borrowed = service.borrow(borrowRequest(3));

        assertThat(borrowed.available).isEqualTo(1);
        ArgumentCaptor<EquipmentBorrow> saved = ArgumentCaptor.forClass(EquipmentBorrow.class);
        verify(equipmentBorrowRepository).save(saved.capture());
        assertThat(saved.getValue().getQty()).isEqualTo(3);
        assertThat(saved.getValue().getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(saved.getValue().getEquipmentId()).isEqualTo(EQUIPMENT_ID);
        assertThat(saved.getValue().isReturned()).isFalse();
    }

    @Test
    void shouldLetTeacherBorrowForSomeoneElsesBooking() {
        givenCurrentUser(TEACHER_ID, Role.TEACHER);
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(OTHER_STUDENT_ID, BookingStatus.APPROVED)));
        when(equipmentRepository.findById(EQUIPMENT_ID))
                .thenReturn(Optional.of(equipment(10, 4)));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.borrow(borrowRequest(1)).available).isEqualTo(3);
    }

    @Test
    void throwsBadRequestExceptionWhenBookingIsNotApproved() {
        givenCurrentUser(STUDENT_ID, Role.STUDENT);
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(STUDENT_ID, BookingStatus.PENDING)));

        assertThatThrownBy(() -> service.borrow(borrowRequest(1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Equipment can only be borrowed for approved bookings");
    }

    @Test
    void throwsForbiddenExceptionWhenBorrowingForSomeoneElsesBooking() {
        givenCurrentUser(STUDENT_ID, Role.STUDENT);
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(OTHER_STUDENT_ID, BookingStatus.APPROVED)));

        assertThatThrownBy(() -> service.borrow(borrowRequest(1)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Not allowed to borrow for this booking");
    }

    @Test
    void throwsBadRequestExceptionWhenNotEnoughEquipmentAvailable() {
        givenCurrentUser(STUDENT_ID, Role.STUDENT);
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(STUDENT_ID, BookingStatus.APPROVED)));
        when(equipmentRepository.findById(EQUIPMENT_ID))
                .thenReturn(Optional.of(equipment(10, 2)));

        assertThatThrownBy(() -> service.borrow(borrowRequest(3)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Not enough equipment available");
    }

    @Test
    void throwsResourceNotFoundExceptionWhenBookingForBorrowMissing() {
        givenCurrentUser(STUDENT_ID, Role.STUDENT);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.borrow(borrowRequest(1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Booking not found");
    }

    @Test
    void throwsResourceNotFoundExceptionWhenEquipmentMissing() {
        givenCurrentUser(STUDENT_ID, Role.STUDENT);
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(STUDENT_ID, BookingStatus.APPROVED)));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.borrow(borrowRequest(1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Equipment not found");
    }

    @Test
    void shouldReturnEquipmentAndRestoreAvailability() {
        EquipmentBorrow borrow = EquipmentBorrow.builder()
                .id(BORROW_ID)
                .bookingId(BOOKING_ID)
                .equipmentId(EQUIPMENT_ID)
                .qty(2)
                .returned(false)
                .build();
        when(equipmentBorrowRepository.findById(BORROW_ID)).thenReturn(Optional.of(borrow));
        when(equipmentRepository.findById(EQUIPMENT_ID))
                .thenReturn(Optional.of(equipment(10, 3)));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(equipmentBorrowRepository.save(any(EquipmentBorrow.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        EquipmentDto returned = service.returnEquipment(BORROW_ID);

        assertThat(returned.available).isEqualTo(5);
        assertThat(borrow.isReturned()).isTrue();
        assertThat(borrow.getReturnedAt()).isNotNull();
    }

    @Test
    void throwsBadRequestExceptionWhenBorrowAlreadyReturned() {
        EquipmentBorrow borrow = EquipmentBorrow.builder()
                .id(BORROW_ID)
                .bookingId(BOOKING_ID)
                .equipmentId(EQUIPMENT_ID)
                .qty(2)
                .returned(true)
                .build();
        when(equipmentBorrowRepository.findById(BORROW_ID)).thenReturn(Optional.of(borrow));

        assertThatThrownBy(() -> service.returnEquipment(BORROW_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Already returned");
    }

    @Test
    void throwsResourceNotFoundExceptionWhenBorrowRecordMissing() {
        when(equipmentBorrowRepository.findById(BORROW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.returnEquipment(BORROW_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Borrow record not found");
    }
}
