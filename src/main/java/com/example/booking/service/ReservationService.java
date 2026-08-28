package com.example.booking.service;

import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.dto.ReservationUpdateRequest;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.enums.Role;
import com.example.booking.exception.AccessDeniedAppException;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ReservationService {
    private final ReservationRepository reservations;
    private final ResourceRepository resources;
    private final UserRepository users;

    public ReservationService(ReservationRepository reservations, ResourceRepository resources, UserRepository users) {
        this.reservations = reservations; this.resources = resources; this.users = users;
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request, String username) {
        User user = currentUser(username);
        Resource resource = resource(request.resourceId());
        validateTimes(request.startTime(), request.endTime());
        ensureAvailable(resource, request.startTime(), request.endTime(), null);

        Reservation r = new Reservation();
        r.setUser(user);
        r.setResource(resource);
        r.setStartTime(request.startTime());
        r.setEndTime(request.endTime());
        r.setPrice(resource.getPrice());
        r.setStatus(ReservationStatus.PENDING);
        return ReservationResponse.from(reservations.save(r));
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> search(String username, ReservationStatus status, BigDecimal minPrice,
                                             BigDecimal maxPrice, int page, int size, String sortBy, String direction) {
        User user = currentUser(username);
        if (page < 0) throw new BadRequestException("page must be >= 0");
        if (size < 1 || size > 100) throw new BadRequestException("size must be between 1 and 100");
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) throw new BadRequestException("minPrice cannot be greater than maxPrice");
        String safeSort = switch (sortBy == null ? "startTime" : sortBy) {
            case "id", "startTime", "endTime", "price", "status" -> sortBy;
            default -> throw new BadRequestException("Invalid sortBy. Allowed: id, startTime, endTime, price, status");
        };
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, safeSort));
        Long userId = user.getRole() == Role.ADMIN ? null : user.getId();
        return reservations.search(userId, status, minPrice, maxPrice, pageable).map(ReservationResponse::from);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id, String username) {
        Reservation r = find(id);
        checkOwnership(r, username);
        return ReservationResponse.from(r);
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationUpdateRequest request, String username) {
        Reservation r = find(id);
        User user = currentUser(username);
        boolean admin = user.getRole() == Role.ADMIN;
        if (!admin && !r.getUser().getId().equals(user.getId())) throw new AccessDeniedAppException("You can access only your own reservations");
        validateTimes(request.startTime(), request.endTime());
        Resource resource = resource(request.resourceId());
        ensureAvailable(resource, request.startTime(), request.endTime(), r.getId());
        r.setResource(resource);
        r.setStartTime(request.startTime());
        r.setEndTime(request.endTime());
        r.setPrice(resource.getPrice());
        if (admin && request.status() != null) r.setStatus(request.status());
        else if (!admin && request.status() == ReservationStatus.CANCELLED) r.setStatus(ReservationStatus.CANCELLED);
        return ReservationResponse.from(reservations.save(r));
    }

    @Transactional
    public void delete(Long id, String username) {
        Reservation r = find(id);
        User user = currentUser(username);
        if (user.getRole() != Role.ADMIN && !r.getUser().getId().equals(user.getId())) throw new AccessDeniedAppException("You can delete only your own reservations");
        reservations.delete(r);
    }

    private void checkOwnership(Reservation r, String username) {
        User user = currentUser(username);
        if (user.getRole() != Role.ADMIN && !r.getUser().getId().equals(user.getId())) throw new AccessDeniedAppException("You can access only your own reservations");
    }

    private User currentUser(String username) { return users.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found")); }
    private Reservation find(Long id) { return reservations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id)); }
    private Resource resource(Long id) { return resources.findById(id).orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id)); }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) throw new BadRequestException("startTime must be before endTime");
        if (!start.isAfter(LocalDateTime.now())) throw new BadRequestException("startTime must be in the future");
    }

    private void ensureAvailable(Resource resource, LocalDateTime start, LocalDateTime end, Long reservationId) {
        if (!resource.isAvailable()) throw new BadRequestException("Resource is not available");
        long count = reservationId == null
                ? reservations.countOverlapping(resource.getId(), start, end)
                : reservations.countOverlappingExcept(reservationId, resource.getId(), start, end);
        if (count > 0) throw new BadRequestException("Resource is already booked for the requested time range");
    }
}
