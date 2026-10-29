package com.epam.edp.demo.validation.booking;

import com.epam.edp.demo.dto.bookings.BookingRequest;
import com.epam.edp.demo.dto.bookings.NewOldPackageOptions;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.entity.tourModel.TourPackageOption;
import com.epam.edp.demo.exception.booking.BookingValidationException;
import com.epam.edp.demo.exception.NoAvailableSpotsException;
import com.epam.edp.demo.repository.booking.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class TourBookingValidation {

    private final BookingRepository bookingRepository;

    public TourBookingValidation(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public TourPackageOption validateBookingCreation(BookingRequest requestDto, Tour tour, String userId) {
        log.info("Validating tour for booking on {}", requestDto.date());

        validateMealPlan(requestDto, tour);
        validatePackageIsNotBookedMoreThanOnce(requestDto, userId);

        TourPackageOption tourPackageOption = validateTourPackageExistenceAndAvailability(requestDto, tour);
        return tourPackageOption;
    }

    public NewOldPackageOptions validateBookingEditing(BookingRequest editBookingRequest, Tour tour, Booking currentBooking) {
        log.info("Validating booking editing for booking {}", currentBooking.getBookingId());

        validateMealPlan(editBookingRequest, tour);

        NewOldPackageOptions newOldPackageOptions = validateTourPackageExistenceAndAvailabilityForEditing(editBookingRequest, tour, currentBooking);
        return newOldPackageOptions;
    }

    private void validatePackageIsNotBookedMoreThanOnce(BookingRequest request, String userId) {

        List<Booking> bookings = bookingRepository.getBookingsByCustomerIdFilterStatus(userId, BookingStatus.BOOKED);

        bookings.stream()
                .filter(b -> b.getTourId().equals(request.tourId()))
                .filter(b -> b.getStartDate().equals(request.date()))
                .findFirst()
                .ifPresent(pd -> {
                    throw new BookingValidationException("You already booked this tour for this date");
                });
    }

    private TourPackageOption validateTourPackageExistenceAndAvailability(BookingRequest request, Tour tour) {
        List<TourPackageOption> matches = tour.getTourPackageOptions().stream()
                .filter(option ->
                        option.getStartDate().equals(request.date()) &&
                                option.getDuration().equals(request.duration())
                )
                .toList();

        if (matches.isEmpty()) {
            throw new BookingValidationException("No package found for given date and duration");
        }

        return matches.stream()
                .filter(option ->
                        option.getAvailableAdultSpots() >= request.guests().getAdults() &&
                                option.getAvailableChildSpots() >= request.guests().getChildren()
                )
                .findFirst()
                .orElseThrow(() -> new NoAvailableSpotsException("No available spots for selected package"));
    }

    private NewOldPackageOptions validateTourPackageExistenceAndAvailabilityForEditing(BookingRequest editBookingRequest,
                                                                                       Tour tour,
                                                                                       Booking currentBooking
    ) {
        log.info("Validating tour package for editing booking {}", currentBooking.getBookingId());
        LocalDate bookingDate = editBookingRequest.date();
        String duration = editBookingRequest.duration();

        int replaceableAdultsCount;
        int replaceableChildrenCount;
        boolean isNew = false;
        if (!currentBooking.getStartDate().equals(bookingDate) || !currentBooking.getDuration().equals(duration)) {
            log.info("update is for new booking date or duration");
            isNew = true;
            replaceableAdultsCount = 0;
            replaceableChildrenCount = 0;
        } else {
            log.info("update is for old booking date and duration");
            replaceableAdultsCount = currentBooking.getGuests().getAdults();
            replaceableChildrenCount = currentBooking.getGuests().getChildren();
        }

        TourPackageOption candidateCurrentOption = null;
        if (isNew) {
            log.info("Finding current package option");
            List<TourPackageOption> currentBookingPackage = tour.getTourPackageOptions().stream()
                    .filter(option ->
                            option.getStartDate().equals(currentBooking.getStartDate()) &&
                                    option.getDuration().equals(currentBooking.getDuration())
                    ).filter(option ->
                            option.getAvailableAdultSpots() + replaceableAdultsCount <= tour.getGuestQuantity().getAdultsMaxValue() &&
                                    option.getAvailableChildSpots() + replaceableChildrenCount <= tour.getGuestQuantity().getChildrenMaxValue()
                    ).toList();

            candidateCurrentOption = currentBookingPackage.stream()
                    .max(Comparator.comparing(TourPackageOption::getAvailableAdultSpots))
                    .orElseThrow(() -> new BookingValidationException("Package not found, please contact your travel agent"));
            log.info("Found candidate current package option {}", candidateCurrentOption);
        }

        log.info("Finding package option for tour {} with startDate={}, duration={}, replaceableAdultsCount={}, replaceableChildrenCount={}, maxAdultsValue={}, maxChildrenValue={}, newAdultCount={}, newChildCount={}",
                tour.getTourId(),
                bookingDate,
                duration,
                replaceableAdultsCount,
                replaceableChildrenCount,
                tour.getGuestQuantity().getAdultsMaxValue(),
                tour.getGuestQuantity().getChildrenMaxValue(),
                editBookingRequest.guests().getAdults(),
                editBookingRequest.guests().getChildren());

        List<TourPackageOption> candidateNewPackageOptions = tour.getTourPackageOptions().stream()
                .filter(option ->
                        option.getStartDate().equals(bookingDate) &&
                                option.getDuration().equals(duration)
                ).filter(option ->
                        option.getAvailableAdultSpots() + replaceableAdultsCount <= tour.getGuestQuantity().getAdultsMaxValue() &&
                                option.getAvailableChildSpots() + replaceableChildrenCount <= tour.getGuestQuantity().getChildrenMaxValue()
                ).filter(option ->
                        option.getAvailableAdultSpots() + replaceableAdultsCount >= editBookingRequest.guests().getAdults() &&
                                option.getAvailableChildSpots() + replaceableChildrenCount >= editBookingRequest.guests().getChildren()
                ).toList();

        log.info("Found {} candidates for new package option", candidateNewPackageOptions.size());
        if (candidateNewPackageOptions.isEmpty()) {
            throw new BookingValidationException("No package found for given date, duration, adult/child count");
        }
        return new NewOldPackageOptions(
                candidateNewPackageOptions.get(0),
                candidateCurrentOption
        );
    }

    private void validateMealPlan(BookingRequest request, Tour tour) {
        boolean exists = tour.getMealSupplements().stream()
                .anyMatch(m -> m.getMealPlan() == request.mealPlan());

        if (!exists) {
            throw new BookingValidationException("Meal plan " + request.mealPlan() + " is not available for this tour");
        }
    }
}
