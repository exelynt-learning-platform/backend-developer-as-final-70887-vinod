package com.example.booking.dto;

import com.example.booking.enums.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ReservationUpdateRequest(
        @NotNull(message = "Resource ID is required") Long resourceId,
        @NotNull(message = "Start time is required") LocalDateTime startTime,
        @NotNull(message = "End time is required") LocalDateTime endTime,
        ReservationStatus status
) {}
