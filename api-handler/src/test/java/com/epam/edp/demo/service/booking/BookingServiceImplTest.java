package com.epam.edp.demo.service.booking;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.bookings.CancelBookingRequest;
import com.epam.edp.demo.dto.bookings.BookingRequest;
import com.epam.edp.demo.dto.bookings.BookingCancellationResponse;
import com.epam.edp.demo.dto.bookings.BookingDetailsResponse;
import com.epam.edp.demo.dto.bookings.BookingListResponse;
import com.epam.edp.demo.dto.bookings.NewOldPackageOptions;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.booking.Guests;
import com.epam.edp.demo.entity.booking.PersonalDetails;
import com.epam.edp.demo.entity.booking.TravelAgentSnapshot;
import com.epam.edp.demo.entity.review.Review;
import com.epam.edp.demo.entity.tourModel.MealPlan;
import com.epam.edp.demo.entity.tourModel.MealSupplement;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.entity.tourModel.TourPackageOption;
import com.epam.edp.demo.entity.tourModel.TourType;
import com.epam.edp.demo.entity.staff.Staff;
import com.epam.edp.demo.exception.NoAvailableTravelAgentException;
import com.epam.edp.demo.exception.UserNotFoundException;
import com.epam.edp.demo.exception.booking.BookingAccessDeniedException;
import com.epam.edp.demo.exception.booking.BookingNotFoundException;
import com.epam.edp.demo.exception.booking.BookingStateTransitionException;
import com.epam.edp.demo.exception.tour.TourNotFoundException;
import com.epam.edp.demo.repository.auth.UserRepository;
import com.epam.edp.demo.repository.booking.BookingRepository;
import com.epam.edp.demo.repository.tour.TourRepository;
import com.epam.edp.demo.repository.staff.StaffRepository;
import com.epam.edp.demo.service.document.DocumentService;
import com.epam.edp.demo.service.reports.EventPublisherService;
import com.epam.edp.demo.service.review.ReviewService;
import com.epam.edp.demo.service.tour.TourService;
import com.epam.edp.demo.validation.booking.TourBookingValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TourBookingValidation validator;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @Mock
    private ReviewService reviewService;

    @Mock
    private TourService tourService;

    @Mock
    private DocumentService documentService;

    @Mock
    private EventPublisherService eventPublisherService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        lenient().when(documentService.generateRetrieveUrls(any(Booking.class))).thenReturn(new HashMap<>());
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        lenient().when(clock.instant()).thenReturn(
                LocalDate.of(2026, 6, 1)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
        );
    }


    @Test
    void getBookings_customer_populatesReviewWhenReviewExistsForBooking() {
        UserContext ctx = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        Booking booking = booking("booking-1");
        when(bookingRepository.getBookingsByCustomerId("cust-1")).thenReturn(List.of(booking));
        when(reviewService.getReviewsByUserId("cust-1")).thenReturn(List.of(
                review("booking-1", 5, "Amazing trip")));

        BookingListResponse response = bookingService.getBookings(ctx, null);

        assertThat(response.bookings()).hasSize(1);
        assertThat(response.bookings().get(0).review()).isNotNull();
        assertThat(response.bookings().get(0).review().rate()).isEqualTo(5);
        assertThat(response.bookings().get(0).review().reviewContent()).isEqualTo("Amazing trip");
    }

    @Test
    void getBookings_customer_leavesReviewNullWhenNoMatchingReview() {
        UserContext ctx = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        Booking booking = booking("booking-1");
        when(bookingRepository.getBookingsByCustomerId("cust-1")).thenReturn(List.of(booking));
        when(reviewService.getReviewsByUserId("cust-1")).thenReturn(List.of(
                review("some-other-booking", 4, "Not this booking")));

        BookingListResponse response = bookingService.getBookings(ctx, null);

        assertThat(response.bookings()).hasSize(1);
        assertThat(response.bookings().get(0).review()).isNull();
    }

    @Test
    void getBookings_travelAgent_leavesReviewNullWhenAgentHasNoReviews() {
        UserContext ctx = new UserContext("agent-1", "agent@example.com", Role.TRAVEL_AGENT);
        Booking booking = booking("booking-1");
        when(userRepository.findById("agent-1"))
                .thenReturn(Optional.of(User.builder().id("agent-1").email("agent@example.com").build()));
        when(bookingRepository.getBookingsByTravelAgentEmail("agent@example.com")).thenReturn(List.of(booking));
        when(reviewService.getReviewsByUserId("agent-1")).thenReturn(List.of());

        BookingListResponse response = bookingService.getBookings(ctx, null);

        assertThat(response.bookings()).hasSize(1);
        assertThat(response.bookings().get(0).review()).isNull();
    }

    @Test
    void getBookings_customer_propagatesExceptionWhenReviewLookupFails() {
        UserContext ctx = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        Booking booking = booking("booking-1");
        when(bookingRepository.getBookingsByCustomerId("cust-1")).thenReturn(List.of(booking));
        when(reviewService.getReviewsByUserId("cust-1"))
                .thenThrow(new RuntimeException("dynamo hiccup"));

        assertThatThrownBy(() -> bookingService.getBookings(ctx, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("dynamo hiccup");
    }

    @Test
    void getBookings_customer_filtersByStatusWhenProvided() {
        UserContext ctx = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.CONFIRMED);
        when(bookingRepository.getBookingsByCustomerIdFilterStatus("cust-1", BookingStatus.CONFIRMED))
                .thenReturn(List.of(booking));
        when(reviewService.getReviewsByUserId("cust-1")).thenReturn(List.of());

        BookingListResponse response = bookingService.getBookings(ctx, BookingStatus.CONFIRMED);

        assertThat(response.bookings()).hasSize(1);
        verify(bookingRepository).getBookingsByCustomerIdFilterStatus("cust-1", BookingStatus.CONFIRMED);
    }

    @Test
    void getBookings_travelAgent_filtersByStatusWhenProvided() {
        UserContext ctx = new UserContext("agent-1", "agent@example.com", Role.TRAVEL_AGENT);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.CONFIRMED);
        when(userRepository.findById("agent-1"))
                .thenReturn(Optional.of(User.builder().id("agent-1").email("agent@example.com").build()));
        when(bookingRepository.getBookingsByTravelAgentEmailFilterStatus("agent@example.com", BookingStatus.CONFIRMED))
                .thenReturn(List.of(booking));
        when(reviewService.getReviewsByUserId("agent-1")).thenReturn(List.of());

        BookingListResponse response = bookingService.getBookings(ctx, BookingStatus.CONFIRMED);

        assertThat(response.bookings()).hasSize(1);
        verify(bookingRepository).getBookingsByTravelAgentEmailFilterStatus("agent@example.com", BookingStatus.CONFIRMED);
    }

    @Test
    void getBookings_adminCannotAccessBookings() {
        UserContext ctx = new UserContext("admin-1", "admin@example.com", Role.ADMIN);

        assertThatThrownBy(() -> bookingService.getBookings(ctx, null))
                .isInstanceOf(BookingAccessDeniedException.class)
                .hasMessageContaining("cannot access bookings");
    }

    @Test
    void updateBookingStatuses_updatesConfirmedToStarted() {
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.CONFIRMED);
        booking.setStartDate(LocalDate.parse("2026-06-01"));
        booking.setDuration("7 days");
        when(clock.instant()).thenReturn(
                LocalDate.of(2026, 6, 3)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
        );
        bookingService.updateBookingStatuses(List.of(booking));

        assertThat(booking.getState()).isEqualTo(BookingStatus.STARTED);
        verify(bookingRepository).saveBookings(argThat(list ->
                list.size() == 1 && list.get(0).getState() == BookingStatus.STARTED));
        verify(eventPublisherService, never()).publish(any());
    }

    @Test
    void updateBookingStatuses_updatesStartedToFinishedAndPublishesEvent() {
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.STARTED);
        booking.setStartDate(LocalDate.parse("2026-06-01"));
        booking.setDuration("2 days");
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(LocalDate.of(2026, 6, 5).atStartOfDay(ZoneOffset.UTC).toInstant());;

        bookingService.updateBookingStatuses(List.of(booking));

        assertThat(booking.getState()).isEqualTo(BookingStatus.FINISHED);
        verify(bookingRepository).saveBookings(argThat(list ->
                list.size() == 1 && list.get(0).getState() == BookingStatus.FINISHED));
        verify(eventPublisherService).publish(any());
    }

    @Test
    void updateBookingStatuses_doesNotChangeCanceledBookings() {
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.CANCELED);

        bookingService.updateBookingStatuses(List.of(booking));

        assertThat(booking.getState()).isEqualTo(BookingStatus.CANCELED);
        verify(bookingRepository).saveBookings(List.of());
        verify(eventPublisherService, never()).publish(any());
    }

    @Test
    void confirmBooking_rejectsNonTravelAgentRole() throws NoSuchMethodException {
        // Role enforcement is delegated to Spring Security's @PreAuthorize, which runs at
        // the proxy layer and is not exercised when the method is called directly in a unit
        // test. Assert the security contract itself: only TRAVEL_AGENT may confirm a booking.
        PreAuthorize preAuthorize = BookingServiceImpl.class
                .getMethod("confirmBooking", UserContext.class, String.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('TRAVEL_AGENT')");
    }

    @Test
    void confirmBooking_rejectsTravelAgentNotAssignedToBooking() {
        UserContext agent = new UserContext("agent-1", "agent@example.com", Role.TRAVEL_AGENT);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.BOOKED);
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(java.util.Optional.of(booking));
        when(userRepository.findEmailById("agent-1")).thenReturn("different-agent@example.com");

        assertThatThrownBy(() -> bookingService.confirmBooking(agent, "booking-1"))
                .isInstanceOf(BookingAccessDeniedException.class)
                .hasMessageContaining("cannot confirm this booking");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void confirmBooking_confirmsBookingAndPublishesEvent() {
        UserContext agent = new UserContext("agent-1", "agent@example.com", Role.TRAVEL_AGENT);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.BOOKED);
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(java.util.Optional.of(booking));
        when(userRepository.findEmailById("agent-1")).thenReturn("agent@example.com");

        bookingService.confirmBooking(agent, "booking-1");

        assertThat(booking.getState()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).save(booking);
        verify(eventPublisherService).publish(any());
    }

    @Test
    void confirmBooking_throwsWhenBookingStateIsNotBooked() {
        UserContext agent = new UserContext("agent-1", "agent@example.com", Role.TRAVEL_AGENT);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.CONFIRMED);
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBooking(agent, "booking-1"))
                .isInstanceOf(BookingStateTransitionException.class)
                .hasMessageContaining("CONFIRMED");

        verify(userRepository, never()).findEmailById(any());
        verify(bookingRepository, never()).save(any());
        verify(eventPublisherService, never()).publish(any());
    }

    @Test
    void cancelBooking_customerCancelsOwnBookedBooking() {
        UserContext customer = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.BOOKED);
        booking.setStartDate(LocalDate.now().plusDays(20));
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(java.util.Optional.of(booking));

        BookingCancellationResponse response = bookingService.cancelBooking(customer, "booking-1", new CancelBookingRequest("customer request"));

        assertThat(response.message()).isEqualTo("Booking was cancelled successfully");
        verify(bookingRepository).cancelBooking(booking);
        verify(tourService).increaseTourOption(booking);
        verify(eventPublisherService).publish(any());
    }

    @Test
    void cancelBooking_throwsWhenStatusCannotBeCanceled() {
        UserContext customer = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.CANCELED);
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(java.util.Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(customer, "booking-1", new CancelBookingRequest("late")))
                .isInstanceOf(BookingStateTransitionException.class)
                .hasMessageContaining("cannot be canceled");
    }

    @Test
    void cancelBooking_throwsBookingNotFoundWhenMissing() {
        UserContext customer = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        when(bookingRepository.findByBookingId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(customer, "missing", new CancelBookingRequest("reason")))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void cancelBooking_customerCannotCancelAnotherCustomersBooking() {
        UserContext customer = new UserContext("cust-2", "cust2@example.com", Role.CUSTOMER);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.BOOKED);
        booking.setStartDate(LocalDate.now().plusDays(20));
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(customer, "booking-1", new CancelBookingRequest("reason")))
                .isInstanceOf(BookingAccessDeniedException.class)
                .hasMessageContaining("cannot cancel other customers booking");

        verify(bookingRepository, never()).cancelBooking(any());
    }

    @Test
    void cancelBooking_travelAgentCancelsAssignedBooking() {
        UserContext travelAgent = new UserContext("agent-user-id", "agent@example.com", Role.TRAVEL_AGENT);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.CONFIRMED);
        booking.setStartDate(LocalDate.now().plusDays(20));
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findEmailById("agent-user-id")).thenReturn("agent@example.com");

        BookingCancellationResponse response = bookingService.cancelBooking(
                travelAgent,
                "booking-1",
                new CancelBookingRequest("Client requested cancellation")
        );

        assertThat(response.message()).isEqualTo("Booking was cancelled successfully");
        verify(bookingRepository).cancelBookingByTravelAgent(booking, "Client requested cancellation");
        verify(tourService).increaseTourOption(booking);
        verify(eventPublisherService).publish(any());
    }

    @Test
    void cancelBooking_travelAgentCannotCancelOtherAgentsBooking() {
        UserContext travelAgent = new UserContext("agent-user-id", "agent@example.com", Role.TRAVEL_AGENT);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.CONFIRMED);
        booking.setStartDate(LocalDate.now().plusDays(20));
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findEmailById("agent-user-id")).thenReturn("another-agent@example.com");

        assertThatThrownBy(() -> bookingService.cancelBooking(travelAgent, "booking-1", new CancelBookingRequest("reason")))
                .isInstanceOf(BookingAccessDeniedException.class)
                .hasMessageContaining("cannot cancel this booking");

        verify(bookingRepository, never()).cancelBookingByTravelAgent(any(), any());
    }

    @Test
    void cancelBooking_adminCannotCancelBookings() {
        UserContext admin = new UserContext("admin-1", "admin@example.com", Role.ADMIN);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.BOOKED);
        booking.setStartDate(LocalDate.now().plusDays(20));
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(admin, "booking-1", new CancelBookingRequest("reason")))
                .isInstanceOf(BookingAccessDeniedException.class)
                .hasMessageContaining("cannot cancel bookings");

        verify(bookingRepository, never()).cancelBooking(any());
        verify(bookingRepository, never()).cancelBookingByTravelAgent(any(), any());
    }

    @Test
    void createBooking_persistsBookingWithMappedFieldsAndUpdatesQuantity() {
        UserContext customer = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        BookingRequest request = createBookingRequest();
        Tour tour = createTour("tour-1");
        TourPackageOption option = packageOption("7 days", 900);
        Staff travelAgent = new Staff("Agent Name", "agent@example.com", "+1", "telegram", Role.TRAVEL_AGENT);
        User user = User.builder().id("cust-1").firstName("Jane").lastName("Doe").email("cust@example.com").build();

        when(tourRepository.findTourById("tour-1")).thenReturn(Optional.of(tour));
        when(validator.validateBookingCreation(request, tour, "cust-1")).thenReturn(option);
        when(staffRepository.getAnyAvailableTravelAgent()).thenReturn(Optional.of(travelAgent));
        when(userRepository.findById("cust-1")).thenReturn(Optional.of(user));

        BookingDetailsResponse response = bookingService.createBooking(customer, request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        verify(tourRepository).updateBookedQuantity(tour, option, request.guests(), false);

        Booking saved = bookingCaptor.getValue();
        assertThat(saved.getBookingId()).isNotBlank();
        assertThat(saved.getUserId()).isEqualTo("cust-1");
        assertThat(saved.getUserName()).isEqualTo("Jane Doe");
        assertThat(saved.getState()).isEqualTo(BookingStatus.BOOKED);
        assertThat(saved.getTravelAgentEmail()).isEqualTo("agent@example.com");
        assertThat(saved.getMealPlan()).isEqualTo(MealPlan.AI);
        assertThat(saved.getGuests().getAdults()).isEqualTo(2);
        assertThat(saved.getGuests().getChildren()).isEqualTo(1);
        assertThat(saved.getTotalPrice()).isEqualTo(3330);
        assertThat(saved.getPersonalDetails().get(1).getLastName()).isEqualTo("Smith (2)");

        assertThat(response.freeCancellation()).isEqualTo("2026-07-05");
        assertThat(response.details()).contains("Summer Escape");
    }

    @Test
    void createBooking_throwsTourNotFoundWhenMissingTour() {
        UserContext customer = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        BookingRequest request = createBookingRequest();
        when(tourRepository.findTourById("tour-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(customer, request))
                .isInstanceOf(TourNotFoundException.class)
                .hasMessageContaining("tour-1");
    }

    @Test
    void createBooking_throwsNoAvailableTravelAgentWhenNoAgentAvailable() {
        UserContext customer = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        BookingRequest request = createBookingRequest();
        Tour tour = createTour("tour-1");
        TourPackageOption option = packageOption("7 days", 900);

        when(tourRepository.findTourById("tour-1")).thenReturn(Optional.of(tour));
        when(validator.validateBookingCreation(request, tour, "cust-1")).thenReturn(option);
        when(staffRepository.getAnyAvailableTravelAgent()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(customer, request))
                .isInstanceOf(NoAvailableTravelAgentException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void createBooking_throwsUserNotFoundWhenRequesterMissing() {
        UserContext customer = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        BookingRequest request = createBookingRequest();
        Tour tour = createTour("tour-1");
        TourPackageOption option = packageOption("7 days", 900);
        Staff travelAgent = new Staff("Agent Name", "agent@example.com", "+1", "telegram", Role.TRAVEL_AGENT);

        when(tourRepository.findTourById("tour-1")).thenReturn(Optional.of(tour));
        when(validator.validateBookingCreation(request, tour, "cust-1")).thenReturn(option);
        when(staffRepository.getAnyAvailableTravelAgent()).thenReturn(Optional.of(travelAgent));
        when(userRepository.findById("cust-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(customer, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("cust-1");
    }

    @Test
    void editBooking_updatesBookingAndAdjustsTourPackageQuantity() {
        UserContext agent = new UserContext("agent-1", "agent@example.com", Role.TRAVEL_AGENT);
        Booking currentBooking = booking("booking-1");
        currentBooking.setState(BookingStatus.BOOKED);
        currentBooking.setStartDate(LocalDate.parse("2026-08-01"));
        currentBooking.setDuration("7 days");
        currentBooking.setGuests(guests(2, 0));
        BookingRequest request = editBookingRequest();
        Tour tour = createTour("tour-1");
        tour.setMealSupplements(List.of(mealSupplement(MealPlan.HB, 20)));

        TourPackageOption newOption = packageOption("5 days", 1200);
        newOption.setStartDate(LocalDate.parse("2026-08-20"));
        TourPackageOption assumedOldOption = packageOption("7 days", 900);
        assumedOldOption.setStartDate(LocalDate.parse("2026-08-01"));
        NewOldPackageOptions validated = new NewOldPackageOptions(newOption, assumedOldOption);

        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(currentBooking));
        when(userRepository.findEmailById("agent-1")).thenReturn("agent@example.com");
        when(tourRepository.findTourById("tour-1")).thenReturn(Optional.of(tour));
        when(validator.validateBookingEditing(request, tour, currentBooking)).thenReturn(validated);

        BookingDetailsResponse response = bookingService.editBooking(agent, "booking-1", request);

        verify(bookingRepository).save(currentBooking);
        verify(tourRepository).updateBookedQuantity(
                eq(tour),
                eq(assumedOldOption),
                eq(newOption),
                argThat(g -> g.getAdults() == 2 && g.getChildren() == 0),
                argThat(g -> g.getAdults() == 1 && g.getChildren() == 2)
        );

        assertThat(currentBooking.getStartDate()).isEqualTo(LocalDate.parse("2026-08-20"));
        assertThat(currentBooking.getDuration()).isEqualTo("5 days");
        assertThat(currentBooking.getMealPlan()).isEqualTo(MealPlan.HB);
        assertThat(currentBooking.getGuests().getAdults()).isEqualTo(1);
        assertThat(currentBooking.getGuests().getChildren()).isEqualTo(2);
        assertThat(currentBooking.getTotalPrice()).isEqualTo(3900);
        assertThat(currentBooking.getPersonalDetails().get(1).getLastName()).isEqualTo("Miller (2)");

        assertThat(response.freeCancellation()).isEqualTo("2026-08-10");
        assertThat(response.details()).contains("edited your booking");
    }

    @Test
    void editBooking_throwsBookingNotFoundWhenMissing() {
        UserContext agent = new UserContext("agent-1", "agent@example.com", Role.TRAVEL_AGENT);
        when(bookingRepository.findByBookingId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.editBooking(agent, "missing", editBookingRequest()))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void editBooking_throwsAccessDeniedWhenTravelAgentIsNotAssigned() {
        UserContext agent = new UserContext("agent-1", "agent@example.com", Role.TRAVEL_AGENT);
        Booking currentBooking = booking("booking-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(currentBooking));
        when(userRepository.findEmailById("agent-1")).thenReturn("other-agent@example.com");

        assertThatThrownBy(() -> bookingService.editBooking(agent, "booking-1", editBookingRequest()))
                .isInstanceOf(BookingAccessDeniedException.class)
                .hasMessageContaining("cannot edit this booking");
    }

    @Test
    void editBooking_customerEditsOwnBooking() {
        UserContext customer = new UserContext("cust-1", "cust@example.com", Role.CUSTOMER);
        Booking currentBooking = booking("booking-1");
        currentBooking.setState(BookingStatus.BOOKED);
        currentBooking.setStartDate(LocalDate.parse("2026-08-01"));
        currentBooking.setDuration("7 days");
        currentBooking.setGuests(guests(2, 0));
        BookingRequest request = editBookingRequest();
        Tour tour = createTour("tour-1");
        tour.setMealSupplements(List.of(mealSupplement(MealPlan.HB, 20)));

        TourPackageOption newOption = packageOption("5 days", 1200);
        newOption.setStartDate(LocalDate.parse("2026-08-20"));
        TourPackageOption assumedOldOption = packageOption("7 days", 900);
        assumedOldOption.setStartDate(LocalDate.parse("2026-08-01"));
        NewOldPackageOptions validated = new NewOldPackageOptions(newOption, assumedOldOption);

        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(currentBooking));
        when(tourRepository.findTourById("tour-1")).thenReturn(Optional.of(tour));
        when(validator.validateBookingEditing(request, tour, currentBooking)).thenReturn(validated);

        BookingDetailsResponse response = bookingService.editBooking(customer, "booking-1", request);

        verify(userRepository, never()).findEmailById(any());
        verify(bookingRepository).save(currentBooking);
        verify(tourRepository).updateBookedQuantity(
                eq(tour),
                eq(assumedOldOption),
                eq(newOption),
                argThat(g -> g.getAdults() == 2 && g.getChildren() == 0),
                argThat(g -> g.getAdults() == 1 && g.getChildren() == 2)
        );

        assertThat(response.freeCancellation()).isEqualTo("2026-08-10");
        assertThat(response.details()).contains("edited your booking");
    }

    @Test
    void editBooking_customerCannotEditOtherCustomersBooking() {
        UserContext customer = new UserContext("cust-2", "cust2@example.com", Role.CUSTOMER);
        Booking currentBooking = booking("booking-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(currentBooking));

        assertThatThrownBy(() -> bookingService.editBooking(customer, "booking-1", editBookingRequest()))
                .isInstanceOf(BookingAccessDeniedException.class)
                .hasMessageContaining("cannot edit other customers booking");

        verify(userRepository, never()).findEmailById(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void editBooking_adminCannotEditBookings() {
        UserContext admin = new UserContext("admin-1", "admin@example.com", Role.ADMIN);
        Booking currentBooking = booking("booking-1");
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(currentBooking));

        assertThatThrownBy(() -> bookingService.editBooking(admin, "booking-1", editBookingRequest()))
                .isInstanceOf(BookingAccessDeniedException.class)
                .hasMessageContaining("cannot edit bookings");

        verify(userRepository, never()).findEmailById(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void editBooking_throwsAccessDeniedWhenFreeCancellationDatePassed() {
        UserContext agent = new UserContext("agent-1", "agent@example.com", Role.TRAVEL_AGENT);
        Booking currentBooking = booking("booking-1");
        currentBooking.setStartDate(LocalDate.parse("2026-05-15"));
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(currentBooking));
        when(userRepository.findEmailById("agent-1")).thenReturn("agent@example.com");

        assertThatThrownBy(() -> bookingService.editBooking(agent, "booking-1", editBookingRequest()))
                .isInstanceOf(BookingAccessDeniedException.class)
                .hasMessageContaining("Free cancellation date");
    }

    @Test
    void cancelBooking_travelAgentWithNullRequest_throwsValidationException() {
        UserContext travelAgent = new UserContext("agent-user-id", "agent@example.com", Role.TRAVEL_AGENT);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.BOOKED);
        booking.setStartDate(LocalDate.now().plusDays(20));
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findEmailById("agent-user-id")).thenReturn("agent@example.com");

        assertThatThrownBy(() -> bookingService.cancelBooking(travelAgent, "booking-1", null))
                .isInstanceOf(com.epam.edp.demo.exception.booking.BookingValidationException.class)
                .hasMessageContaining("Cancellation reason is required");
    }

    @Test
    void cancelBooking_travelAgentWithNullReason_throwsValidationException() {
        UserContext travelAgent = new UserContext("agent-user-id", "agent@example.com", Role.TRAVEL_AGENT);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.BOOKED);
        booking.setStartDate(LocalDate.now().plusDays(20));
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findEmailById("agent-user-id")).thenReturn("agent@example.com");

        assertThatThrownBy(() -> bookingService.cancelBooking(travelAgent, "booking-1", new CancelBookingRequest(null)))
                .isInstanceOf(com.epam.edp.demo.exception.booking.BookingValidationException.class)
                .hasMessageContaining("Cancellation reason is required");
    }

    @Test
    void cancelBooking_travelAgentWithBlankReason_throwsValidationException() {
        UserContext travelAgent = new UserContext("agent-user-id", "agent@example.com", Role.TRAVEL_AGENT);
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.BOOKED);
        booking.setStartDate(LocalDate.now().plusDays(20));
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(userRepository.findEmailById("agent-user-id")).thenReturn("agent@example.com");

        assertThatThrownBy(() -> bookingService.cancelBooking(travelAgent, "booking-1", new CancelBookingRequest("   ")))
                .isInstanceOf(com.epam.edp.demo.exception.booking.BookingValidationException.class)
                .hasMessageContaining("Cancellation reason is required");
    }

    @Test
    void updateBookingStatuses_doesNotChangeBookedBookingBeforeStart() {
        Booking booking = booking("booking-1");
        booking.setState(BookingStatus.BOOKED);
        booking.setStartDate(LocalDate.of(2026, 8, 1));
        booking.setDuration("7 days");
        when(clock.instant()).thenReturn(
                LocalDate.of(2026, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant());

        bookingService.updateBookingStatuses(List.of(booking));

        assertThat(booking.getState()).isEqualTo(BookingStatus.BOOKED);
        verify(bookingRepository).saveBookings(List.of());
        verify(eventPublisherService, never()).publish(any());
    }

    @Test
    void editBooking_throwsTourNotFoundWhenTargetTourMissing() {
        UserContext agent = new UserContext("agent-1", "agent@example.com", Role.TRAVEL_AGENT);
        Booking currentBooking = booking("booking-1");
        currentBooking.setStartDate(LocalDate.parse("2026-08-01"));
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(currentBooking));
        when(userRepository.findEmailById("agent-1")).thenReturn("agent@example.com");
        when(tourRepository.findTourById("tour-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.editBooking(agent, "booking-1", editBookingRequest()))
                .isInstanceOf(TourNotFoundException.class)
                .hasMessageContaining("tour-1");
    }

    private static Booking booking(String bookingId) {
        TravelAgentSnapshot snapshot = new TravelAgentSnapshot(
                "Agent Name", "agent@example.com", "+1", "messenger");
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .userId("cust-1")
                .userEmail("cust@example.com")
                .userName("Cust One")
                .tourId("tour-1")
                .state(BookingStatus.FINISHED)
                .tourImageUrl("https://example.com/img.png")
                .name("Tour Name")
                .destination("Paris")
                .tourType(TourType.RESORT)
                .startDate(LocalDate.parse("2026-05-01"))
                .duration("7 days")
                .mealPlan(MealPlan.AI)
                .personalDetails(List.of(new PersonalDetails("Cust", "One", null)))
                .totalPrice(1000)
                .travelAgent(snapshot)
                .travelAgentEmail("agent@example.com")
                .build();
        return booking;
    }

    private static Review review(String reviewId, int rate, String content) {
        Review review = new Review();
        review.setReviewId(reviewId);
        review.setRate(rate);
        review.setReviewContent(content);
        return review;
    }

    private static BookingRequest createBookingRequest() {
        Guests guests = new Guests();
        guests.setAdults(2);
        guests.setChildren(1);
        return new BookingRequest(
                "tour-1",
                LocalDate.parse("2026-07-15"),
                "7 days",
                MealPlan.AI,
                guests,
                List.of(
                        new PersonalDetails("Alex", "Smith", null),
                        new PersonalDetails("Alex", "Smith", null),
                        new PersonalDetails("Sam", "Johnson", null)
                )
        );
    }

    private static BookingRequest editBookingRequest() {
        return new BookingRequest(
                "tour-1",
                LocalDate.parse("2026-08-20"),
                "5 days",
                MealPlan.HB,
                guests(1, 2),
                List.of(
                        new PersonalDetails("Ava", "Miller", null),
                        new PersonalDetails("Ava", "Miller", null),
                        new PersonalDetails("Noah", "Brown", null)
                )
        );
    }

    private static Guests guests(int adults, int children) {
        Guests guests = new Guests();
        guests.setAdults(adults);
        guests.setChildren(children);
        return guests;
    }

    private static Tour createTour(String tourId) {
        Tour tour = new Tour();
        tour.setTourId(tourId);
        tour.setName("Summer Escape");
        tour.setDestination("Barcelona");
        tour.setTourType(TourType.RESORT);
        tour.setImageUrls(List.of("https://example.com/tour.png"));
        tour.setMealSupplements(List.of(mealSupplement(MealPlan.AI, 30)));
        return tour;
    }

    private static TourPackageOption packageOption(String duration, int price) {
        TourPackageOption option = new TourPackageOption();
        option.setDuration(duration);
        option.setPrice(price);
        option.setStartDate(LocalDate.parse("2026-07-15"));
        return option;
    }

    private static MealSupplement mealSupplement(MealPlan mealPlan, int pricePerDay) {
        MealSupplement supplement = new MealSupplement();
        supplement.setMealPlan(mealPlan);
        supplement.setPricePerDay(pricePerDay);
        return supplement;
    }
}
