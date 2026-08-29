package com.example.booking.dto;

import com.example.booking.entity.Reservation;
import com.example.booking.enums.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long userId,
        String username,
        Long resourceId,
        String resourceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal price,
        ReservationStatus status
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(), r.getUser().getId(), r.getUser().getUsername(),
                r.getResource().getId(), r.getResource().getName(),
                r.getStartTime(), r.getEndTime(), r.getPrice(), r.getStatus());
    }
}
