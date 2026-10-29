package com.epam.edp.demo.service.seed;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.bookings.BookingRequest;
import com.epam.edp.demo.dto.bookings.CancelBookingRequest;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.booking.TravelAgentSnapshot;
import com.epam.edp.demo.exception.seed.SeedingException;
import com.epam.edp.demo.repository.auth.UserRepository;
import com.epam.edp.demo.repository.booking.BookingRepository;
import com.epam.edp.demo.repository.review.ReviewRepository;
import com.epam.edp.demo.repository.tour.TourRepository;
import com.epam.edp.demo.repository.staff.StaffRepository;
import com.epam.edp.demo.service.auth.AuthService;
import com.epam.edp.demo.service.booking.BookingServiceImpl;
import com.epam.edp.demo.service.reports.EventPublisherService;
import com.epam.edp.demo.service.review.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SeedServiceTest {

    private ObjectMapper objectMapper;
    private AuthService authService;
    private BookingServiceImpl bookingService;
    private ReviewService reviewService;
    private UserRepository userRepository;
    private StaffRepository staffRepository;
    private TourRepository tourRepository;
    private ReviewRepository reviewRepository;
    private BookingRepository bookingRepository;
    private PasswordEncoder passwordEncoder;
    private SeedService service;
    private EventPublisherService eventPublisherService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        authService = mock(AuthService.class);
        bookingService = mock(BookingServiceImpl.class);
        reviewService = mock(ReviewService.class);
        userRepository = mock(UserRepository.class);
        staffRepository = mock(StaffRepository.class);
        tourRepository = mock(TourRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        bookingRepository = mock(BookingRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        eventPublisherService = mock(EventPublisherService.class);

        service = new SeedService(
                objectMapper,
                authService, bookingService, reviewService, userRepository,
                staffRepository, tourRepository, reviewRepository, bookingRepository,
                passwordEncoder, eventPublisherService);
    }

    private static java.util.List<User> sevenCustomers() {
        java.util.List<User> users = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            users.add(customer("u-" + i, "u" + i + "@example.com"));
        }
        return users;
    }

    @Test
    void seed_freshDatabase_seedsReferenceDataAndUsersAndBookingsInOneCall() {
        when(staffRepository.staffExists(anyString(), eq(Role.TRAVEL_AGENT))).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.getAll()).thenReturn(sevenCustomers());
        stubBookings(List.of());

        service.seed();

        // reference data
        verify(staffRepository).saveAll(anyList());
        verify(tourRepository).saveAll(anyList());
        verify(reviewRepository).saveAll(anyList());
        // users and bookings, all in the same call
        verify(userRepository, atLeastOnce()).save(any(User.class));
        verify(bookingService, atLeastOnce())
                .createBooking(any(UserContext.class), any(BookingRequest.class));
    }

    @Test
    void seed_whenEverythingAlreadySeeded_isNoOp() {
        when(staffRepository.staffExists(anyString(), eq(Role.TRAVEL_AGENT))).thenReturn(true);
        when(bookingRepository.anyExists()).thenReturn(true);

        service.seed();

        verify(staffRepository, never()).saveAll(anyList());
        verify(userRepository, never()).save(any());
        verify(bookingService, never()).createBooking(any(), any());
        verify(bookingRepository, never()).findAll();
    }

    @Test
    void seed_skipsUserSaveForUsersThatAlreadyExist() {
        // A re-run must not crash on users that already exist.
        when(staffRepository.staffExists(anyString(), eq(Role.TRAVEL_AGENT))).thenReturn(true);
        when(bookingRepository.anyExists()).thenReturn(false);
        when(userRepository.getAll()).thenReturn(sevenCustomers());
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        stubBookings(List.of());

        service.seed();

        verify(userRepository, never()).save(any(User.class));
        // bookings are still attempted even though users already existed
        verify(bookingService, atLeastOnce())
                .createBooking(any(UserContext.class), any(BookingRequest.class));
    }

    @Test
    void seed_secondPass_savesEveryUserAndCreatesBookingForEachRequest() {
        when(staffRepository.staffExists(anyString(), eq(Role.TRAVEL_AGENT))).thenReturn(true);
        when(userRepository.getAll()).thenReturn(sevenCustomers());
        stubBookings(List.of());

        service.seed();

        verify(userRepository, atLeastOnce()).save(any(User.class));
        verify(bookingService, atLeastOnce())
                .createBooking(any(UserContext.class), any(BookingRequest.class));
    }

    @Test
    void seed_secondPass_updatesBookingStatusesTwice() {
        when(staffRepository.staffExists(anyString(), eq(Role.TRAVEL_AGENT))).thenReturn(true);
        when(userRepository.getAll()).thenReturn(sevenCustomers());
        stubBookings(List.of());

        service.seed();

        verify(bookingService, times(2)).updateBookingStatuses(any());
    }

    @Test
    void seed_secondPass_skipsCancellationFlow_whenNoBookedBookingsRemain() {
        when(staffRepository.staffExists(anyString(), eq(Role.TRAVEL_AGENT))).thenReturn(true);
        when(userRepository.getAll()).thenReturn(sevenCustomers());
        stubBookings(List.of());

        service.seed();

        verify(bookingService, never())
                .cancelBooking(any(), anyString(), any(CancelBookingRequest.class));
    }

    @Test
    void seed_skipsBookingSeeding_whenNoCustomerUsersExist() {
        // e.g. every sign-up failed (captcha) so no CUSTOMER users are present:
        // seeding must skip bookings cleanly, not index out of bounds.
        when(staffRepository.staffExists(anyString(), eq(Role.TRAVEL_AGENT))).thenReturn(true);
        when(userRepository.getAll()).thenReturn(List.of());
        stubBookings(List.of());

        service.seed();

        verify(bookingService, never())
                .createBooking(any(UserContext.class), any(BookingRequest.class));
    }

    private void stubBookings(List<Booking> bookings) {
        when(bookingRepository.findAll()).thenReturn(bookings);
    }

    private static User customer(String id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .role(Role.CUSTOMER)
                .build();
    }

    private static User travelAgentUser(String email) {
        return User.builder()
                .id("agent-id-" + email)
                .email(email)
                .role(Role.TRAVEL_AGENT)
                .build();
    }

    private static Booking booking(String id, LocalDate startDate, BookingStatus state, String travelAgentEmail) {
        return Booking.builder()
                .bookingId(id)
                .userId("u-1")
                .tourId("tour-1")
                .state(state)
                .name("Trip")
                .destination("Somewhere")
                .startDate(startDate)
                .duration("7 days")
                .travelAgentEmail(travelAgentEmail)
                .travelAgent(new TravelAgentSnapshot("Agent", travelAgentEmail, "555", "tg"))
                .build();
    }
}
