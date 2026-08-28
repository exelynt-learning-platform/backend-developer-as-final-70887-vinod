package com.example.booking;

import com.example.booking.dto.ReservationUpdateRequest;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.ReservationStatus;
import com.example.booking.enums.Role;
import com.example.booking.exception.AccessDeniedAppException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.ReservationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservations;

    @Mock
    private ResourceRepository resources;

    @Mock
    private UserRepository users;

    private ReservationService service;

    private User normalUser;
    private User adminUser;

    private Resource originalResource;
    private Resource anotherResource;

    private Reservation reservation;

    @BeforeEach
    void setUp() {

        service = new ReservationService(
                reservations,
                resources,
                users
        );

        normalUser = new User();
        setId(normalUser, 10L);
        normalUser.setUsername("user");
        normalUser.setRole(Role.USER);

        adminUser = new User();
        setId(adminUser, 99L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ADMIN);

        originalResource = resource(
                1L,
                "Room A",
                true,
                "500.00"
        );

        anotherResource = resource(
                2L,
                "Room B",
                true,
                "700.00"
        );

        reservation = new Reservation();

        reservation.setUser(normalUser);
        reservation.setResource(originalResource);

        reservation.setStartTime(
                LocalDateTime.now().plusDays(1)
        );

        reservation.setEndTime(
                LocalDateTime.now()
                        .plusDays(1)
                        .plusHours(1)
        );

        reservation.setPrice(
                originalResource.getPrice()
        );

        reservation.setStatus(
                ReservationStatus.PENDING
        );
    }

    @Test
    void normalUserCannotChangeReservationResource() {

        when(reservations.findById(1L))
                .thenReturn(Optional.of(reservation));

        when(users.findByUsername("user"))
                .thenReturn(Optional.of(normalUser));

        ReservationUpdateRequest request =
                new ReservationUpdateRequest(
                        2L,
                        LocalDateTime.now().plusDays(2),
                        LocalDateTime.now()
                                .plusDays(2)
                                .plusHours(1),
                        null
                );

        assertThrows(
                AccessDeniedAppException.class,
                () -> service.update(
                        1L,
                        request,
                        "user"
                )
        );

        verify(resources, never())
                .findById(2L);

        verify(reservations, never())
                .save(any(Reservation.class));
    }

    @Test
    void normalUserCanCancelOwnReservationWithoutChangingResource() {

        when(reservations.findById(1L))
                .thenReturn(Optional.of(reservation));

        when(users.findByUsername("user"))
                .thenReturn(Optional.of(normalUser));

        lenient().when(resources.findById(1L))
                .thenReturn(Optional.of(originalResource));

        lenient().when(
                reservations.countOverlappingExcept(
                        any(),
                        any(),
                        any(),
                        any()
                )
        ).thenReturn(0L);

        when(reservations.save(any(Reservation.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        LocalDateTime start =
                LocalDateTime.now().plusDays(2);

        LocalDateTime end =
                start.plusHours(1);

        ReservationUpdateRequest request =
                new ReservationUpdateRequest(
                        1L,
                        start,
                        end,
                        ReservationStatus.CANCELLED
                );

        service.update(
                1L,
                request,
                "user"
        );

        assertEquals(
                ReservationStatus.CANCELLED,
                reservation.getStatus()
        );

        assertEquals(
                originalResource,
                reservation.getResource()
        );

        verify(reservations)
                .save(reservation);
    }

    @Test
    void adminCanChangeReservationResourceAndStatus() {

        when(reservations.findById(1L))
                .thenReturn(Optional.of(reservation));

        when(users.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));

        when(resources.findById(2L))
                .thenReturn(Optional.of(anotherResource));

        lenient().when(
                reservations.countOverlappingExcept(
                        any(),
                        any(),
                        any(),
                        any()
                )
        ).thenReturn(0L);

        when(reservations.save(any(Reservation.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        LocalDateTime start =
                LocalDateTime.now().plusDays(2);

        LocalDateTime end =
                start.plusHours(2);

        ReservationUpdateRequest request =
                new ReservationUpdateRequest(
                        2L,
                        start,
                        end,
                        ReservationStatus.CONFIRMED
                );

        service.update(
                1L,
                request,
                "admin"
        );

        assertEquals(
                anotherResource,
                reservation.getResource()
        );

        assertEquals(
                ReservationStatus.CONFIRMED,
                reservation.getStatus()
        );

        assertEquals(
                new BigDecimal("700.00"),
                reservation.getPrice()
        );

        verify(reservations)
                .save(reservation);
    }

    private Resource resource(
            Long id,
            String name,
            boolean available,
            String price) {

        Resource resource = new Resource();

        setId(
                resource,
                id
        );

        resource.setName(name);
        resource.setDescription("Test resource");
        resource.setType("ROOM");
        resource.setAvailable(available);
        resource.setPrice(new BigDecimal(price));

        return resource;
    }

    private void setId(
            Object entity,
            Long id) {

        try {

            var field =
                    entity.getClass()
                            .getDeclaredField("id");

            field.setAccessible(true);
            field.set(entity, id);

        } catch (ReflectiveOperationException ex) {

            throw new IllegalStateException(ex);
        }
    }
}