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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ReservationService {

    private final ReservationRepository reservations;
    private final ResourceRepository resources;
    private final UserRepository users;

    public ReservationService(
            ReservationRepository reservations,
            ResourceRepository resources,
            UserRepository users) {

        this.reservations = reservations;
        this.resources = resources;
        this.users = users;
    }

    @Transactional
    public ReservationResponse create(
            ReservationRequest request,
            String username) {

        User user = currentUser(username);

        Resource resource = resource(request.resourceId());

        validateTimes(
                request.startTime(),
                request.endTime()
        );

        ensureAvailable(
                resource,
                request.startTime(),
                request.endTime(),
                null
        );

        Reservation r = new Reservation();

        r.setUser(user);
        r.setResource(resource);
        r.setStartTime(request.startTime());
        r.setEndTime(request.endTime());
        r.setPrice(resource.getPrice());
        r.setStatus(ReservationStatus.PENDING);

        return ReservationResponse.from(
                reservations.save(r)
        );
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> search(
            String username,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction) {

        User user = currentUser(username);

        if (page < 0) {
            throw new BadRequestException(
                    "page must be >= 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new BadRequestException(
                    "size must be between 1 and 100"
            );
        }

        if (minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {

            throw new BadRequestException(
                    "minPrice cannot be greater than maxPrice"
            );
        }

        String requestedSort =
                sortBy == null ? "startTime" : sortBy;

        String safeSort = switch (requestedSort) {

            case "id",
                 "startTime",
                 "endTime",
                 "price",
                 "status" -> requestedSort;

            default ->
                    throw new BadRequestException(
                            "Invalid sortBy. Allowed: id, startTime, endTime, price, status"
                    );
        };

        Sort.Direction dir =
                "desc".equalsIgnoreCase(direction)
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(dir, safeSort)
                );

        Long userId =
                user.getRole() == Role.ADMIN
                        ? null
                        : user.getId();

        return reservations
                .search(
                        userId,
                        status,
                        minPrice,
                        maxPrice,
                        pageable
                )
                .map(ReservationResponse::from);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(
            Long id,
            String username) {

        Reservation reservation = find(id);

        checkOwnership(
                reservation,
                username
        );

        return ReservationResponse.from(
                reservation
        );
    }

    @Transactional
    public ReservationResponse update(
            Long id,
            ReservationUpdateRequest request,
            String username) {

        Reservation reservation = find(id);

        User user = currentUser(username);

        boolean admin =
                user.getRole() == Role.ADMIN;

        checkOwnership(
                reservation,
                user
        );

        /*
         * Normal users are allowed to cancel
         * their reservation, but they cannot
         * change it to another status.
         */
        if (!admin
                && request.status() != null
                && request.status() != ReservationStatus.CANCELLED) {

            throw new AccessDeniedAppException(
                    "Users can only cancel their own reservations"
            );
        }

        /*
         * Critical authorization fix:
         * A normal user cannot move an existing
         * reservation to another resource.
         *
         * Only ADMIN can change the resource.
         */
        if (!admin
                && !reservation.getResource()
                        .getId()
                        .equals(request.resourceId())) {

            throw new AccessDeniedAppException(
                    "Users cannot change the resource of an existing reservation"
            );
        }

        validateTimes(
                request.startTime(),
                request.endTime()
        );

        Resource resource =
                resource(request.resourceId());

        ensureAvailable(
                resource,
                request.startTime(),
                request.endTime(),
                reservation.getId()
        );

        reservation.setResource(resource);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setPrice(resource.getPrice());

        if (admin && request.status() != null) {

            reservation.setStatus(
                    request.status()
            );

        } else if (!admin
                && request.status() == ReservationStatus.CANCELLED) {

            reservation.setStatus(
                    ReservationStatus.CANCELLED
            );
        }

        return ReservationResponse.from(
                reservations.save(reservation)
        );
    }

    @Transactional
    public void delete(
            Long id,
            String username) {

        Reservation reservation = find(id);

        User user = currentUser(username);

        checkOwnership(
                reservation,
                user
        );

        reservations.delete(reservation);
    }

    private void checkOwnership(
            Reservation reservation,
            String username) {

        checkOwnership(
                reservation,
                currentUser(username)
        );
    }

    private void checkOwnership(
            Reservation reservation,
            User user) {

        if (user.getRole() != Role.ADMIN
                && !reservation.getUser()
                        .getId()
                        .equals(user.getId())) {

            throw new AccessDeniedAppException(
                    "You can access only your own reservations"
            );
        }
    }

    private User currentUser(
            String username) {

        return users
                .findByUsername(username)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );
    }

    private Reservation find(Long id) {

        return reservations
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Reservation not found: " + id
                        )
                );
    }

    private Resource resource(Long id) {

        return resources
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Resource not found: " + id
                        )
                );
    }

    private void validateTimes(
            LocalDateTime start,
            LocalDateTime end) {

        if (!start.isBefore(end)) {
            throw new BadRequestException(
                    "startTime must be before endTime"
            );
        }

        if (!start.isAfter(LocalDateTime.now())) {
            throw new BadRequestException(
                    "startTime must be in the future"
            );
        }
    }

    private void ensureAvailable(
            Resource resource,
            LocalDateTime start,
            LocalDateTime end,
            Long reservationId) {

        if (!resource.isAvailable()) {
            throw new BadRequestException(
                    "Resource is not available"
            );
        }

        long count =
                reservationId == null
                        ? reservations.countOverlapping(
                                resource.getId(),
                                start,
                                end
                        )
                        : reservations.countOverlappingExcept(
                                reservationId,
                                resource.getId(),
                                start,
                                end
                        );

        if (count > 0) {
            throw new BadRequestException(
                    "Resource is already booked for the requested time range"
            );
        }
    }
}