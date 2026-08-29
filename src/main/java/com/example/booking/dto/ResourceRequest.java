package com.example.booking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ResourceRequest(
        @NotBlank(message = "Name is required") @Size(max = 120) String name,
        @NotBlank(message = "Description is required") @Size(max = 1000) String description,
        @NotBlank(message = "Type is required") @Size(max = 60) String type,
        @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be greater than 0") @Digits(integer = 10, fraction = 2) BigDecimal price,
        boolean available
) {}
