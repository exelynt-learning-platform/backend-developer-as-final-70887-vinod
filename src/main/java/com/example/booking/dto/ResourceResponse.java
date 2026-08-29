package com.example.booking.dto;

import com.example.booking.entity.Resource;
import java.math.BigDecimal;

public record ResourceResponse(Long id, String name, String description, String type, BigDecimal price, boolean available) {
    public static ResourceResponse from(Resource r) {
        return new ResourceResponse(r.getId(), r.getName(), r.getDescription(), r.getType(), r.getPrice(), r.isAvailable());
    }
}
