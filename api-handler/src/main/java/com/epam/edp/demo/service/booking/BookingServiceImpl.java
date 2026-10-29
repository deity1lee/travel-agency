package com.epam.edp.demo.service.booking;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.reports.BookingStatusChangedEvent;
import com.epam.edp.demo.dto.bookings.*;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.entity.booking.*;
import com.epam.edp.demo.entity.review.Review;
import com.epam.edp.demo.entity.tourModel.MealSupplement;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.entity.tourModel.TourPackageOption;
import com.epam.edp.demo.entity.staff.Staff;
import com.epam.edp.demo.exception.*;
import com.epam.edp.demo.exception.booking.BookingAccessDeniedException;
import com.epam.edp.demo.exception.booking.BookingNotFoundException;
import com.epam.edp.demo.exception.booking.BookingStateTransitionException;
import com.epam.edp.demo.exception.booking.BookingValidationException;
import com.epam.edp.demo.exception.tour.TourNotFoundException;
import com.epam.edp.demo.repository.booking.BookingRepository;
import com.epam.edp.demo.repository.auth.UserRepository;
import com.epam.edp.demo.repository.tour.TourRepository;
import com.epam.edp.demo.repository.staff.StaffRepository;
import com.epam.edp.demo.service.document.DocumentService;
import com.epam.edp.demo.service.reports.EventMapper;
import com.epam.edp.demo.service.reports.EventPublisherService;
import com.epam.edp.demo.service.review.ReviewService;
import com.epam.edp.demo.service.tour.TourService;
import com.epam.edp.demo.util.BookingCalculationsUtils;
import com.epam.edp.demo.validation.booking.TourBookingValidation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TourBookingValidation validator;
    private final StaffRepository staffRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final ReviewService reviewService;
    private final TourService tourService;
    private final DocumentService documentService;
    private final EventPublisherService eventPublisherService;


    public BookingServiceImpl(BookingRepository bookingRepository,
                              TourRepository tourRepository,
                              UserRepository userRepository,
                              TourBookingValidation validator,
                              StaffRepository staffRepository,
                              Clock clock,
                              ReviewService reviewService,
                              TourService tourService,
                              DocumentService documentService,
                              EventPublisherService eventPublisherService) {
        this.bookingRepository = bookingRepository;
        this.tourRepository = tourRepository;
        this.userRepository = userRepository;
        this.validator = validator;
        this.staffRepository = staffRepository;
        this.clock = clock;
        this.reviewService = reviewService;
        this.tourService = tourService;
        this.documentService = documentService;
        this.eventPublisherService = eventPublisherService;
    }

    @Override
    public BookingListResponse getBookings(UserContext userContext, BookingStatus status) {
        log.info("Getting Bookings for user {}", userContext);

        List<Booking> bookings;
        log.info("Getting bookings for user {} with role {}", userContext.userId(), userContext.role());
        if (userContext.role() == Role.CUSTOMER) {
            bookings = getBookingsByCustomerId(userContext.userId(), status);
        } else if (userContext.role() == Role.TRAVEL_AGENT) {
            User user = userRepository.findById(userContext.userId())
                    .orElseThrow(() -> new UserNotFoundException(String.format("User with id %s not found", userContext.userId())));
            bookings = getBookingsByTravelAgentEmail(user.getEmail(), status);
        } else {
            String message = String.format("%s cannot access bookings", userContext.role());
            log.info("{}", message);
            throw new BookingAccessDeniedException(message);
        }

        updateBookingStatuses(bookings);

        // todo (for Taron): check if travel agent GET bookings needs to include booking reviews
        Map<String, Review> reviewsByUserId = reviewService.getReviewsByUserId(userContext.userId()).stream()
                .collect(Collectors.toMap(Review::getReviewId, r -> r, (a, b) -> a));

        List<BookingDto> bookingDtos = bookings.stream()
                .map(bookingDto ->
                        toBookingDTO(
                                bookingDto,
                                reviewsByUserId.get(bookingDto.getBookingId())))
                .toList();

        return new BookingListResponse(bookingDtos);
    }

    private List<Booking> getBookingsByCustomerId(String customerId, BookingStatus bookingStatus) {
        List<Booking> bookings;
        if (bookingStatus != null) {
            bookings = bookingRepository.getBookingsByCustomerIdFilterStatus(customerId, bookingStatus);
        } else {
            bookings = bookingRepository.getBookingsByCustomerId(customerId);
        }
        return bookings;
    }

    private List<Booking> getBookingsByTravelAgentEmail(String travelAgentEmail, BookingStatus bookingStatus) {
        List<Booking> bookings;
        if (bookingStatus != null) {
            bookings = bookingRepository.getBookingsByTravelAgentEmailFilterStatus(travelAgentEmail, bookingStatus);
        } else {
            bookings = bookingRepository.getBookingsByTravelAgentEmail(travelAgentEmail);
        }
        return bookings;
    }

    @Override
    public BookingDetailsResponse createBooking(UserContext userContext, BookingRequest bookingRequestDto) {
        String userId = userContext.userId();
        log.info("Booking tour with id {} for user {}", bookingRequestDto.tourId(), userId);

        log.info("Getting tour details for tour {}", bookingRequestDto.tourId());
        Tour tour = tourRepository.findTourById(bookingRequestDto.tourId())
                .orElseThrow(() -> new TourNotFoundException(String.format("Tour not found for id %s", bookingRequestDto.tourId())));

        TourPackageOption validatedTourPackageOption = validator.validateBookingCreation(bookingRequestDto, tour, userId);
        enumerateDuplicationsForGuestNames(bookingRequestDto.personalDetails());
        Staff travelAgent = staffRepository.getAnyAvailableTravelAgent()
                .orElseThrow(() -> new NoAvailableTravelAgentException("A Travel Agent is currently not available."));

        TravelAgentSnapshot snapshot = new TravelAgentSnapshot();
        snapshot.setEmail(travelAgent.getEmail());
        snapshot.setMessenger(travelAgent.getMessenger());
        snapshot.setName(travelAgent.getName());
        snapshot.setPhone(travelAgent.getPhone());

        Guests guests = new Guests();
        guests.setAdults(bookingRequestDto.guests().getAdults());
        guests.setChildren(bookingRequestDto.guests().getChildren());

        log.info("Getting user details for user {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(String.format("User with id %s not found", userId)));

        Booking booking = Booking.builder()
                .bookingId(UUID.randomUUID().toString())
                .userId(userId)
                .userName(String.join(" ", user.getFirstName(), user.getLastName()))
                .userEmail(user.getEmail())
                .tourId(bookingRequestDto.tourId())
                .state(BookingStatus.BOOKED)
                .tourImageUrl(tour.getImageUrls().get(0))
                .name(tour.getName())
                .destination(tour.getDestination())
                .tourType(tour.getTourType())
                .startDate(bookingRequestDto.date())
                .duration(bookingRequestDto.duration())
                .mealPlan(bookingRequestDto.mealPlan())
                .guests(guests)
                .personalDetails(bookingRequestDto.personalDetails())
                .totalPrice(BookingCalculationsUtils.calculateBookingPrice(
                        bookingRequestDto.guests().getAdults() + bookingRequestDto.guests().getChildren(),
                        validatedTourPackageOption.getPrice(),
                        Integer.parseInt(validatedTourPackageOption.getDuration().trim().split("\\s+")[0]),
                        tour.getMealSupplements().stream()
                                .filter(ms -> ms.getMealPlan() == bookingRequestDto.mealPlan())
                                .map(MealSupplement::getPricePerDay)
                                .findFirst()
                                .orElse(0)
                ))
                .travelAgent(snapshot)
                .travelAgentEmail(travelAgent.getEmail())
                .build();

        log.info("Saving booking {}", booking.getBookingId());
        bookingRepository.save(booking);

        log.info("updating tour option for tour {}", bookingRequestDto.tourId());
        tourRepository.updateBookedQuantity(tour, validatedTourPackageOption, bookingRequestDto.guests(), false);

        log.info("Returning CreateBookingResponseDetailsDTO for tour {}", bookingRequestDto.tourId());
        return new BookingDetailsResponse(
                BookingCalculationsUtils.calculateFreeCancellationDate(bookingRequestDto.date()).toString(),
                BookingCalculationsUtils.formatBookingCreateResponseDetails(bookingRequestDto, tour.getName(), snapshot.getName())
        );
    }

    @Override
    public BookingCancellationResponse cancelBooking(UserContext userContext, String bookingId, CancelBookingRequest request) {
        Role role = userContext.role();
        log.info("Canceling Booking {} by {}", bookingId, role);

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking with id %s was not found", bookingId)));

        BookingStatus status = booking.effectiveStatus();
        if (status != BookingStatus.BOOKED && status != BookingStatus.CONFIRMED) {
            log.info("Booking {} with status {} cannot be canceled", bookingId, status);
            throw new BookingStateTransitionException(String.format("Booking %s with status %s cannot be canceled", bookingId, status));
        }

        double cancellationFee = BookingCalculationsUtils.calculateCancellationFee(booking.getStartDate(), booking.getTotalPrice());
        booking.setCharged(cancellationFee);
        log.info("User {} will be charged {} for canceling booking {}", booking.getPersonalDetails().get(0).getFirstName(), cancellationFee, bookingId);

        String userId = userContext.userId();
        if (role == Role.CUSTOMER) {
            if (!booking.getUserId().equals(userId)) {
                throw new BookingAccessDeniedException("Customer cannot cancel other customers booking");
            }
            bookingRepository.cancelBooking(booking);
        } else if (role == Role.TRAVEL_AGENT) {
            String travelAgentEmail = userRepository.findEmailById(userId);
            if (!booking.getTravelAgent().getEmail().equals(travelAgentEmail)) {
                throw new BookingAccessDeniedException(String.format("Travel Agent %s cannot cancel this booking", travelAgentEmail));
            }
            if (request == null || request.reason() == null || request.reason().isBlank()) {
                throw new BookingValidationException("Cancellation reason is required for travel agent");
            }
            bookingRepository.cancelBookingByTravelAgent(booking, request.reason());
        } else {
            String message = String.format("%s cannot cancel bookings", role);
            throw new BookingAccessDeniedException(message);
        }

        tourService.increaseTourOption(booking);
        sendStateChangeEvent(booking);

        return new BookingCancellationResponse(
                "Booking was cancelled successfully",
                cancellationFee
        );
    }

    @Override
    public BookingDetailsResponse editBooking(UserContext userContext, String bookingId, BookingRequest bookingRequestDto) {
        String userId = userContext.userId();
        log.info("User {} is editing booking with id {}", userId, bookingId);
        Booking currentBooking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking with id %s was not found", bookingId)));

        Role role = userContext.role();
        if (role == Role.CUSTOMER) {
            if (!currentBooking.getUserId().equals(userId)) {
                throw new BookingAccessDeniedException("Customer cannot edit other customers booking");
            }
        } else if (role == Role.TRAVEL_AGENT) {
            String travelAgentEmail = userRepository.findEmailById(userId);
            if (!currentBooking.getTravelAgent().getEmail().equals(travelAgentEmail)) {
                log.info("Travel agent {} cannot edit this booking", userId);
                throw new BookingAccessDeniedException(String.format("Travel Agent %s cannot edit this booking", travelAgentEmail));
            }
        } else {
            String message = String.format("%s cannot edit bookings", role);
            throw new BookingAccessDeniedException(message);
        }

        LocalDate curFreeCancellationDate = BookingCalculationsUtils.calculateFreeCancellationDate(currentBooking.getStartDate());
        if (curFreeCancellationDate.isBefore(LocalDate.now(clock))) {
            throw new BookingAccessDeniedException(String.format("Cannot edit booking. Free cancellation date (%s) has passed.", curFreeCancellationDate));
        }
        Guests oldGuests = currentBooking.getGuests();

        log.info("Getting tour details for tour {}", bookingRequestDto.tourId());
        Tour tour = tourRepository.findTourById(bookingRequestDto.tourId())
                .orElseThrow(() -> new TourNotFoundException(String.format("Tour not found for id %s", bookingRequestDto.tourId())));

        NewOldPackageOptions validatedData = validator.validateBookingEditing(bookingRequestDto, tour, currentBooking);
        Guests updatedGuests = new Guests();

        updatedGuests.setAdults(bookingRequestDto.guests().getAdults());
        updatedGuests.setChildren(bookingRequestDto.guests().getChildren());

        enumerateDuplicationsForGuestNames(bookingRequestDto.personalDetails());

        TourPackageOption updatedPackageOption = validatedData.newTourPackageOption();

        currentBooking.setStartDate(updatedPackageOption.getStartDate());
        currentBooking.setDuration(updatedPackageOption.getDuration());
        currentBooking.setMealPlan(bookingRequestDto.mealPlan());
        currentBooking.setGuests(updatedGuests);
        currentBooking.setPersonalDetails(bookingRequestDto.personalDetails());

        int totalPrice = BookingCalculationsUtils.calculateBookingPrice(
                updatedGuests.getTotalGuests(),
                updatedPackageOption.getPrice(),
                Integer.parseInt(updatedPackageOption.getDuration().trim().split("\\s+")[0]),
                tour.getMealSupplements().stream()
                        .filter(ms -> ms.getMealPlan() == bookingRequestDto.mealPlan())
                        .map(MealSupplement::getPricePerDay)
                        .findFirst()
                        .orElse(0)
        );
        currentBooking.setTotalPrice(totalPrice);

        log.info("Saving updated booking {}", currentBooking.getBookingId());
        bookingRepository.save(currentBooking);

        log.info("updating tour {} option for edited booking {}", tour.getTourId(), currentBooking.getBookingId());
        tourRepository.updateBookedQuantity(tour, validatedData.assumedOldTourPackageOption(), updatedPackageOption, oldGuests, updatedGuests);

        log.info("Returning CreateBookingResponseDetailsDTO for updated booking {}", currentBooking.getUserId());
        String newFreeCancellationDate = BookingCalculationsUtils.calculateFreeCancellationDate(currentBooking.getStartDate().toString());
        String responseDetails = BookingCalculationsUtils.formatBookingEditResponseDetails(bookingRequestDto, tour.getName());

        return new BookingDetailsResponse(
                newFreeCancellationDate,
                responseDetails
        );
    }

    @PreAuthorize("hasRole('TRAVEL_AGENT')")
    public void confirmBooking(UserContext userContext, String bookingId) {
        String userId = userContext.userId();
        log.info("Attempting to confirm booking {} for travel agent id {}", bookingId, userId);
        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking with id %s was not found", bookingId)));

        log.info("Booking {} found", bookingId);
        if (booking.getState() != BookingStatus.BOOKED) {
            log.info("Booking in state {} cannot be confirmed", booking.getState());
            throw new BookingStateTransitionException(String.format("Booking in state %s cannot be confirmed", booking.getState()));
        }

        String travelAgentEmail = userRepository.findEmailById(userId);
        if (!booking.getTravelAgent().getEmail().equals(travelAgentEmail)) {
            log.info("Booking is not attached to travel agent {}", travelAgentEmail);
            throw new BookingAccessDeniedException(String.format("Travel Agent %s cannot confirm this booking", travelAgentEmail));
        }

        booking.setState(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        log.info("Booking {} confirmed", bookingId);
        sendStateChangeEvent(booking);
    }

    public void updateBookingStatuses(List<Booking> bookings) {
        log.info("Updating status of Bookings {}", bookings);

        List<Booking> updated = new ArrayList<>();

        for (Booking booking : bookings) {
            BookingStatus status = getEffectiveStatus(booking);

            if (status != booking.getState()) {
                booking.setState(status);
                if (status == BookingStatus.FINISHED) {
                    sendStateChangeEvent(booking);
                }
                updated.add(booking);
            }
        }

        log.info("Saving status of Bookings {}", bookings);
        bookingRepository.saveBookings(updated);
    }

    private BookingStatus getEffectiveStatus(Booking booking) {
        log.info("Getting effective status of Booking {}", booking.getBookingId());
        BookingStatus status = booking.getState();
        if (status == BookingStatus.FINISHED || status == BookingStatus.CANCELED) {
            return status;
        }

        LocalDate today = LocalDate.now(clock);

        LocalDate startDate = booking.getStartDate();

        String[] parts = booking.getDuration().trim().split(" ");
        int duration = Integer.parseInt(parts[0]);

        LocalDate endDate = startDate.plusDays(duration);

        if (!today.isBefore(endDate) && status == BookingStatus.STARTED) {
            return BookingStatus.FINISHED;
        } else if (!today.isBefore(startDate) && status == BookingStatus.CONFIRMED) {
            return BookingStatus.STARTED;
        }

        log.info("Returning effective status of Booking {}", booking.getBookingId());
        return status;
    }

    public BookingDto toBookingDTO(Booking booking, Review review) {
        log.info("getting document retrieval Urls");
        Map<String, String> documentUrls = documentService.generateRetrieveUrls(booking);
        List<PersonalDetailsResponse> personalDetailsResponses = toPersonalDetailsResponse(booking.getPersonalDetails(), documentUrls);

        BookingReviewDto reviewDto = null;
        if (review != null){
             reviewDto = new BookingReviewDto(
                    review.getRate(),
                    review.getReviewContent()
            );
        }

        return new BookingDto(
                booking.getBookingId(),
                booking.getTourId(),
                booking.getState().toString(),
                booking.getTourImageUrl(),
                booking.getName(),
                booking.getDestination(),
                booking.getTourType(),
                new BookedTourDetailsDto(
                        booking.getStartDate().toString(),
                        booking.getDuration(),
                        booking.getMealPlan().toString(),
                        "$" + booking.getTotalPrice()
                ),
                booking.getTravelAgent(),
                new BookingResponseCustomerDto(
                        booking.getUserName(),
                        booking.getUserEmail(),
                        personalDetailsResponses,
                        documentUrls.get(booking.getPaymentDocumentId())
                ),
                BookingCalculationsUtils.calculateFreeCancellationDate(booking.getStartDate()),
                booking.getCanceledBy(),
                booking.getCancelReason(),
                reviewDto
        );
    }

    private List<PersonalDetailsResponse> toPersonalDetailsResponse(List<PersonalDetails> personalDetails, Map<String, String> documentUrls) {

        return personalDetails.stream()
                .map(pd ->
                        new PersonalDetailsResponse(
                                pd.getFirstName(),
                                pd.getLastName(),
                                documentUrls.get(pd.getPassportDocumentId())
                        )
                )
                .collect(Collectors.toList());
    }

    private void enumerateDuplicationsForGuestNames(List<PersonalDetails> personalDetails) {
        log.info("enumerating duplications for guests {}", personalDetails);
        Map<String, List<PersonalDetails>> grouped =
                personalDetails.stream()
                        .collect(Collectors.groupingBy(
                                pd -> pd.getFirstName().trim() + "|" + pd.getLastName().trim()
                        ));
        grouped.values().stream()
                .filter(list -> list.size() > 1)
                .forEach(duplicates -> {
                    for (int i = 0; i < duplicates.size(); i++) {
                        PersonalDetails pd = duplicates.get(i);
                        pd.setLastName(pd.getLastName() + " (" + (i + 1) + ")");
                    }
                });
    }


    private void sendStateChangeEvent(Booking booking) {
        log.info("sending booking change status event for booking={}, state={}", booking.getBookingId(), booking.getState());
        BookingStatusChangedEvent event = EventMapper.toBookingStateTransitionEvent(booking);

        log.info("publishing SQSEvent: {}", event);
        eventPublisherService.publish(event);
    }
}
