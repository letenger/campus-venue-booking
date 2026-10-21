package com.campus.venue.repository;

import com.campus.venue.domain.Booking;
import com.campus.venue.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByVenueId(Long venueId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    List<Booking> findByVenueIdAndStatus(Long venueId, BookingStatus status);

    /**
     * Returns bookings for the same venue/date whose time ranges overlap the requested
     * window and are not already rejected/cancelled. Used for conflict detection.
     */
    @Query("""
           select b from Booking b
           where b.venueId = :venueId
             and b.bookingDate = :date
             and b.status not in (com.campus.venue.domain.BookingStatus.REJECTED,
                                  com.campus.venue.domain.BookingStatus.CANCELLED)
             and b.startTime < :end
             and b.endTime > :start
           """)
    List<Booking> findOverlapping(@Param("venueId") Long venueId,
                                  @Param("date") LocalDate date,
                                  @Param("start") LocalTime start,
                                  @Param("end") LocalTime end);

    @Query("""
           select b from Booking b
           where b.status = com.campus.venue.domain.BookingStatus.APPROVED
             and b.bookingDate = :date
           """)
    List<Booking> findApprovedOnDate(@Param("date") LocalDate date);

    @Query("""
           select count(b) from Booking b
           where b.venueId = :venueId
             and b.status = :status
             and b.bookingDate between :from and :to
           """)
    long countByVenueAndStatusAndDateBetween(@Param("venueId") Long venueId,
                                             @Param("status") BookingStatus status,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    @Query("""
           select count(b) from Booking b
           where b.venueId = :venueId
             and b.status = com.campus.venue.domain.BookingStatus.COMPLETED
             and b.bookingDate between :from and :to
           """)
    long countCompletedByVenueAndDateBetween(@Param("venueId") Long venueId,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    @Query("""
           select b from Booking b
           where (:venueId is null or b.venueId = :venueId)
             and (:status is null or b.status = :status)
             and (:from is null or b.bookingDate >= :from)
             and (:to is null or b.bookingDate <= :to)
             and (:mine is null or :mine = false or b.userId = :uid)
           """)
    Page<Booking> search(@Param("venueId") Long venueId,
                         @Param("status") BookingStatus status,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to,
                         @Param("mine") Boolean mine,
                         @Param("uid") Long uid,
                         Pageable pageable);

    @Query("""
           select count(b) from Booking b
           where b.status = :status
             and b.bookingDate between :from and :to
           """)
    long countByStatusAndDateBetween(@Param("status") BookingStatus status,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    List<Booking> findByBookingDateBetween(LocalDate from, LocalDate to);
}
