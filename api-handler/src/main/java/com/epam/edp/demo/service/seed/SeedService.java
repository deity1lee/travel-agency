package com.epam.edp.demo.service.seed;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.auth.SignUpRequest;
import com.epam.edp.demo.dto.bookings.BookingRequest;
import com.epam.edp.demo.dto.bookings.CancelBookingRequest;
import com.epam.edp.demo.dto.reports.BookingStatusChangedEvent;
import com.epam.edp.demo.dto.review.CreateReviewRequestDTO;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.review.Review;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.entity.staff.Staff;
import com.epam.edp.demo.exception.seed.SeedingException;
import com.epam.edp.demo.repository.auth.UserRepository;
import com.epam.edp.demo.repository.booking.BookingRepository;
import com.epam.edp.demo.repository.review.ReviewRepository;
import com.epam.edp.demo.repository.tour.TourRepository;
import com.epam.edp.demo.repository.staff.StaffRepository;
import com.epam.edp.demo.service.auth.AuthService;
import com.epam.edp.demo.service.booking.BookingServiceImpl;
import com.epam.edp.demo.service.reports.EventMapper;
import com.epam.edp.demo.service.reports.EventPublisherService;
import com.epam.edp.demo.service.review.ReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Slf4j
public class SeedService {

    private static final int MAX_BOOKING_ATTEMPTS = 5;
    private final ObjectMapper objectMapper;
    private final AuthService signUpService;
    private final BookingServiceImpl bookingService;
    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final TourRepository tourRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisherService eventPublisherService;

    public SeedService(ObjectMapper objectMapper,
                       AuthService authService,
                       BookingServiceImpl bookingService,
                       ReviewService reviewService,
                       UserRepository userRepository,
                       StaffRepository staffRepository,
                       TourRepository tourRepository,
                       ReviewRepository reviewRepository,
                       BookingRepository bookingRepository,
                       PasswordEncoder passwordEncoder,
                       EventPublisherService eventPublisherService
) {
        this.bookingService = bookingService;
        this.signUpService = authService;
        this.reviewService = reviewService;
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.tourRepository = tourRepository;
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.eventPublisherService = eventPublisherService;
    }

    public void seed() {
        seedReferenceData();
        seedUsersAndBookings();
    }

    private void seedReferenceData() {
        if (travelAgentsSeeded()) {
            log.info("Reference data already present; skipping staff, reviews and tours");
            return;
        }
        log.info("Seeding staff, reviews and tours");
        // seed staff
        List<Staff> staff = readStaff();
        seedStaff(staff);

        // seed reviews
        List<Review> reviews = readReviews();
        seedReviews(reviews);

        // seed tours
        Map<String, Double> avgRatingByTour = reviews.stream()
                .collect(Collectors.groupingBy(
                        Review::getTourId,
                        Collectors.averagingInt(Review::getRate)
                ));

        Map<String, Long> ratingSumByTour = reviews.stream()
                .collect(Collectors.groupingBy(
                        Review::getTourId,
                        Collectors.summingLong(Review::getRate)
                ));

        List<Tour> tours = readTours();
        tours.forEach(tour -> {
            tour.setRating(
                    BigDecimal.valueOf(avgRatingByTour.getOrDefault(tour.getTourId(), 0.0))
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue()
            );
            tour.setRatingSum(ratingSumByTour.getOrDefault(tour.getTourId(), 0L));
        });

        seedTours(tours);
    }

    private void seedUsersAndBookings() {
        if (bookingsSeeded()) {
            log.info("Bookings already present; skipping users and bookings");
            return;
        }
        log.info("Seeding user data and bookings");
        // seed user data
        List<SignUpRequest> userData = readUserSignUpRequests();
        seedUsers(userData);

        // seed bookings
        List<BookingRequest> bookingData = readBookingRequests();
        seedBookings(bookingData);

        List<Booking> bookings = bookingRepository.findAll();

        confirmOldBookings(bookings);
        bookingService.updateBookingStatuses(bookings);
        putReviews(bookings);
        bookingService.updateBookingStatuses(bookings);
        confirmNewBookings(bookings);

        cancelBookings(bookings);
    }

    private boolean bookingsSeeded() {
        return bookingRepository.anyExists();
    }

    private List<Tour> readTours() {
        List<Tour> tours = List.of();
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("seed/tours.json")
        ) {
            tours = objectMapper.readValue(
                    is,
                    new TypeReference<>() {
                    }
            );

        } catch (IOException e) {
            log.warn("IO exception, failed to read tour seed data: ", e);
        }

        return tours;
    }

    private List<Review> readReviews() {
        List<Review> reviews = List.of();
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("seed/reviews.json")
        ) {
            reviews = objectMapper.readValue(
                    is,
                    new TypeReference<>() {
                    }
            );

        } catch (IOException e) {
            log.warn("IO exception, failed to read review seed data: ", e);
        }

        return reviews;
    }

    private List<CreateReviewRequestDTO> readBookingReviews() {
        List<CreateReviewRequestDTO> reviews = List.of();
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("seed/booking_reviews.json")
        ) {
            reviews = objectMapper.readValue(
                    is,
                    new TypeReference<>() {
                    }
            );

        } catch (IOException e) {
            log.warn("IO exception, failed to read review seed data: ", e);
        }

        return reviews;
    }

    private List<Staff> readStaff() {
        List<Staff> staff = List.of();
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("seed/staff.json")
        ) {
            staff = objectMapper.readValue(
                    is,
                    new TypeReference<>() {
                    }
            );

        } catch (IOException e) {
            log.warn("IO exception, failed to read staff seed data: ", e);
        }

        return staff;
    }

    private List<SignUpRequest> readUserSignUpRequests() {
        List<SignUpRequest> userSignUpRequests = List.of();
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("seed/users_sign_up.json")
        ) {
            userSignUpRequests = objectMapper.readValue(
                    is,
                    new TypeReference<>() {
                    }
            );

        } catch (IOException e) {
            log.warn("IO exception, failed to read user sign up seed data: ", e);
        }

        return userSignUpRequests;
    }

    private List<BookingRequest> readBookingRequests() {
        List<BookingRequest> bookingRequests = List.of();
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("seed/booking_requests.json")
        ) {
            bookingRequests = objectMapper.readValue(
                    is,
                    new TypeReference<>() {
                    }
            );

        } catch (IOException e) {
            log.warn("IO exception, failed to read booking seed data: ", e);
        }

        return bookingRequests;
    }

    private void seedTours(List<Tour> tours) {
        log.info("seeding tours, total={}", tours.size());
        tourRepository.saveAll(tours);
        log.info("Done seeding tours");
    }

    private void seedReviews(List<Review> reviews) {
        log.info("seeding reviews, total={}", reviews.size());
        reviewRepository.saveAll(reviews);
        log.info("Done seeding reviews");
    }

    private void seedStaff(List<Staff> staff) {
        log.info("seeding travel agents and admins, total={}", staff.size());
        staffRepository.saveAll(staff);
        log.info("Done seeding travel agents and admins");
    }

    private void seedUsers(List<SignUpRequest> userData) {
        log.info("seeding users, total={}", userData.size());
        int created = 0;
        int skipped = 0;
        int failed = 0;
        for (SignUpRequest data : userData) {
            String email = data.email().trim().toLowerCase();
            if (userRepository.existsByEmail(email)) {
                log.info("User {} already exists; skipping", email);
                skipped++;
                continue;
            }
            try {
                User user = User.builder()
                        .id(UUID.randomUUID().toString())
                        .email(email)
                        .password(passwordEncoder.encode(data.password()))
                        .firstName(data.firstName())
                        .lastName(data.lastName())
                        .role(signUpService.findRole(email.trim().toLowerCase()))
                        .build();
                userRepository.save(user);
                created++;
            } catch (Exception e) {
                failed++;
                log.warn("Failed to seed user {}: {} (continuing)", email, e.getMessage());
            }
        }
        log.info("finished seeding users, created={} skipped(existing)={} failed={} total={}",
                created, skipped, failed, userData.size());
    }

    private void seedBookings(List<BookingRequest> bookingData) {
        log.info("seeding bookings, total={}", bookingData.size());
        List<User> customers = userRepository.getAll().stream()
                .filter(user -> user.getRole() == Role.CUSTOMER)
                .toList();

        if (customers.isEmpty()) {
            log.warn("No customer users available; skipping booking seeding ({} bookings not created)",
                    bookingData.size());
            return;
        }

        int created = 0;
        for (BookingRequest data : bookingData) {
            Exception lastError = null;
            boolean booked = false;
            for (int attempt = 1; attempt <= MAX_BOOKING_ATTEMPTS && !booked; attempt++) {
                User user = getRandomCustomer(customers);
                UserContext userContext = new UserContext(
                        user.getId(),
                        user.getEmail(),
                        user.getRole()
                );
                try {
                    bookingService.createBooking(userContext, data);
                    booked = true;
                    created++;
                } catch (Exception e) {
                    lastError = e;
                }
            }
            if (!booked) {
                log.warn("Skipping booking for tour {} on {} after {} attempts: {}",
                        data.tourId(), data.date(), MAX_BOOKING_ATTEMPTS,
                        lastError.getMessage());
            }
        }
        log.info("finished seeding bookings, created={}/{}", created, bookingData.size());
    }

    private User getRandomCustomer(List<User> customers) {
        return customers.get(ThreadLocalRandom.current().nextInt(customers.size()));
    }

    private boolean travelAgentsSeeded() {
        String travelAgentEmail = "tyroneboyer@gmail.com";
        return staffRepository.staffExists(travelAgentEmail, Role.TRAVEL_AGENT);
    }

    private void confirmOldBookings(List<Booking> bookings) {
        LocalDate now = LocalDate.now();

        List<Booking> list = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getStartDate().isAfter(now)) {
                continue;
            }
            list.add(booking);
        }

        confirmBookings(list);
    }

    private void confirmNewBookings(List<Booking> bookings) {
        LocalDate now = LocalDate.now();

        int confirm = 0;
        List<Booking> list = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getStartDate().isBefore(now)) {
                continue;
            }
            if (booking.getState() == BookingStatus.BOOKED) {
                if (confirm == 0) {
                    list.add(booking);
                    confirm = 1;
                } else if (confirm == 1) {
                    confirm = 2;
                } else {
                    confirm = 0;
                }
            }
        }

        confirmBookings(list);
    }

    private void confirmBookings(List<Booking> bookings) {
        for (Booking booking : bookings) {
            booking.setState(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            BookingStatusChangedEvent event = EventMapper.toBookingStateTransitionEvent(booking);
            log.debug("publishing SQSEvent: {}", event);
            eventPublisherService.publish(event);
        }
    }

    private void cancelBookings(List<Booking> bookings) {
        int cancel = 0;
        boolean ta = true;

        for (Booking booking : bookings) {
            if (booking.getState() != BookingStatus.BOOKED) {
                continue;
            }

            if (cancel == 0) {
                cancelBooking(booking, ta);
                cancel = 1;
                ta ^= true;
            } else if (cancel == 1) {
                cancel = 2;
            } else {
                cancel = 0;
            }
        }
    }

    private void cancelBooking(Booking booking, boolean ta) {
        User user;
        if (ta) {
            user = userRepository.findByEmail(booking.getTravelAgent().getEmail())
                    .orElseThrow(() -> new SeedingException(String.format("Travel Agent with email %s not found in seeded data", booking.getTravelAgent().getEmail())));
        } else {
            user = userRepository.findById(booking.getUserId())
                    .orElseThrow(() -> new SeedingException(String.format("User with id %s not found in seeded data", booking.getUserId())));
        }
        UserContext userContext = new UserContext(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
        bookingService.cancelBooking(
                userContext,
                booking.getBookingId(),
                ta ? new CancelBookingRequest("Customer Emergency") : null
        );
    }

    private void putReviews(List<Booking> bookings) {
        List<CreateReviewRequestDTO> reviews = readBookingReviews();
        Iterator<CreateReviewRequestDTO> iterator = reviews.iterator();

        for (Booking booking : bookings) {
            if (booking.getState() != BookingStatus.STARTED) {
                continue;
            }
            if (iterator.hasNext()) {
                log.info("putting review for booking {}", booking.getBookingId());
                reviewService.createReview(booking.getBookingId(), booking.getUserId(), iterator.next());
            } else {
                log.info("No reviews left to put");
            }
        }
    }

}
