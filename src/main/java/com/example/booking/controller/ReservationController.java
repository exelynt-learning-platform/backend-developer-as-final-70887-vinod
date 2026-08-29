package com.example.booking.controller;

import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.dto.ReservationUpdateRequest;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/reservations")
@Tag(name = "Reservations")
public class ReservationController {
    private final ReservationService service;
    public ReservationController(ReservationService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Create a reservation; user identity comes from JWT")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request, Authentication authentication) {
        ReservationResponse created = service.create(request, authentication.getName());
        return ResponseEntity.created(URI.create("/reservations/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List reservations with filtering, pagination and sorting")
    public Page<ReservationResponse> search(
            Authentication authentication,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return service.search(authentication.getName(), status, minPrice, maxPrice, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public ReservationResponse getById(@PathVariable Long id, Authentication authentication) { return service.getById(id, authentication.getName()); }

    @PutMapping("/{id}")
    public ReservationResponse update(@PathVariable Long id, @Valid @RequestBody ReservationUpdateRequest request, Authentication authentication) {
        return service.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        service.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
