package com.example.booking.repository;

import com.example.booking.entity.Reservation;
import com.example.booking.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("""
        select r from Reservation r
        where (:userId is null or r.user.id = :userId)
          and (:status is null or r.status = :status)
          and (:minPrice is null or r.price >= :minPrice)
          and (:maxPrice is null or r.price <= :maxPrice)
        """)
    Page<Reservation> search(@Param("userId") Long userId,
                             @Param("status") ReservationStatus status,
                             @Param("minPrice") BigDecimal minPrice,
                             @Param("maxPrice") BigDecimal maxPrice,
                             Pageable pageable);

    @Query("select count(r) from Reservation r where r.resource.id = :resourceId and r.status <> com.example.booking.enums.ReservationStatus.CANCELLED and r.startTime < :endTime and r.endTime > :startTime")
    long countOverlapping(@Param("resourceId") Long resourceId,
                          @Param("startTime") java.time.LocalDateTime startTime,
                          @Param("endTime") java.time.LocalDateTime endTime);

    @Query("select count(r) from Reservation r where r.id <> :reservationId and r.resource.id = :resourceId and r.status <> com.example.booking.enums.ReservationStatus.CANCELLED and r.startTime < :endTime and r.endTime > :startTime")
    long countOverlappingExcept(@Param("reservationId") Long reservationId,
                                @Param("resourceId") Long resourceId,
                                @Param("startTime") java.time.LocalDateTime startTime,
                                @Param("endTime") java.time.LocalDateTime endTime);
}
