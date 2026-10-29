package com.epam.edp.demo.validation.booking;

import com.epam.edp.demo.dto.bookings.BookingRequest;
import com.epam.edp.demo.dto.bookings.NewOldPackageOptions;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.booking.Guests;
import com.epam.edp.demo.entity.booking.PersonalDetails;
import com.epam.edp.demo.entity.tourModel.GuestQuantity;
import com.epam.edp.demo.entity.tourModel.MealPlan;
import com.epam.edp.demo.entity.tourModel.MealSupplement;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.entity.tourModel.TourPackageOption;
import com.epam.edp.demo.exception.NoAvailableSpotsException;
import com.epam.edp.demo.exception.booking.BookingValidationException;
import com.epam.edp.demo.repository.booking.BookingRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourBookingValidationTest {

    private static final LocalDate START_DATE = LocalDate.of(2027, 3, 1);
    private static final String DURATION = "12 days";

    private static Guests guests(int adults, int children) {
        Guests g = new Guests();
        g.setAdults(adults);
        g.setChildren(children);
        return g;
    }

    private static PersonalDetails details(String first, String last) {
        return new PersonalDetails(first, last, null);
    }

    private static List<PersonalDetails> validDetails(int n) {
        List<PersonalDetails> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(details("Guest", "Smith"));
        }
        return out;
    }

    private static BookingRequest request(int adults, int children, MealPlan mealPlan,
                                          String duration, LocalDate date,
                                          List<PersonalDetails> personalDetails) {
        return new BookingRequest("tour-1", date, duration, mealPlan,
                guests(adults, children), personalDetails);
    }

    private static TourPackageOption packageOption(int adultSpots, int childSpots) {
        TourPackageOption option = new TourPackageOption();
        option.setStartDate(START_DATE);
        option.setDuration(DURATION);
        option.setPrice(1000);
        option.setAvailableAdultSpots(adultSpots);
        option.setAvailableChildSpots(childSpots);
        return option;
    }

    private static Tour tour(TourPackageOption option, MealPlan... plans) {
        Tour t = new Tour();
        t.setTourId("tour-1");
        t.setTourPackageOptions(List.of(option));
        GuestQuantity gq = new GuestQuantity();
        gq.setAdultsMaxValue(10);
        gq.setChildrenMaxValue(5);
        t.setGuestQuantity(gq);
        List<MealSupplement> supplements = new ArrayList<>();
        for (MealPlan plan : plans) {
            MealSupplement s = new MealSupplement();
            s.setMealPlan(plan);
            s.setPricePerDay(10);
            supplements.add(s);
        }
        t.setMealSupplements(supplements);
        return t;
    }

    @Test
    void validateBookingCreation_returnsMatchingPackageAndMealPlan_onValidInput() {
        BookingRepository repository = mock(BookingRepository.class);
        when(repository.getBookingsByCustomerIdFilterStatus("user-1", BookingStatus.BOOKED))
                .thenReturn(List.of());
        TourBookingValidation validation = new TourBookingValidation(repository);

        TourPackageOption option = packageOption(2, 1);
        Tour tour = tour(option, MealPlan.BB);
        BookingRequest req = request(2, 1, MealPlan.BB, DURATION, START_DATE, validDetails(3));

        TourPackageOption result =
                validation.validateBookingCreation(req, tour, "user-1");

        assertThat(result).isEqualTo(option);
    }

    @Test
    void validateBookingCreation_throws_whenNoPackageMatchesDateOrDuration() {
        BookingRepository repository = mock(BookingRepository.class);
        TourBookingValidation validation = new TourBookingValidation(repository);

        Tour tour = tour(packageOption(2, 0), MealPlan.BB);
        BookingRequest req = request(1, 0, MealPlan.BB, "7 days", START_DATE, validDetails(1));

        assertThatThrownBy(() -> validation.validateBookingCreation(req, tour, "user-1"))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("No package found");
    }

    @Test
    void validateBookingCreation_throwsNoAvailableSpots_whenAdultSpotsInsufficient() {
        BookingRepository repository = mock(BookingRepository.class);
        TourBookingValidation validation = new TourBookingValidation(repository);

        Tour tour = tour(packageOption(0, 5), MealPlan.BB);
        BookingRequest req = request(2, 0, MealPlan.BB, DURATION, START_DATE, validDetails(2));

        assertThatThrownBy(() -> validation.validateBookingCreation(req, tour, "user-1"))
                .isInstanceOf(NoAvailableSpotsException.class)
                .hasMessageContaining("No available spots");
    }

    @Test
    void validateBookingCreation_throws_whenMealPlanNotOfferedByTour() {
        BookingRepository repository = mock(BookingRepository.class);
        TourBookingValidation validation = new TourBookingValidation(repository);

        Tour tour = tour(packageOption(2, 0), MealPlan.BB);
        BookingRequest req = request(1, 0, MealPlan.HB, DURATION, START_DATE, validDetails(1));

        assertThatThrownBy(() -> validation.validateBookingCreation(req, tour, "user-1"))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("is not available for this tour");
    }

    @Test
    void validateBookingCreation_throws_whenUserAlreadyBookedSameTourAndDate() {
        BookingRepository repository = mock(BookingRepository.class);
        Booking existing = Booking.builder()
                .bookingId("existing")
                .tourId("tour-1")
                .state(BookingStatus.BOOKED)
                .startDate(START_DATE)
                .build();
        when(repository.getBookingsByCustomerIdFilterStatus("user-1", BookingStatus.BOOKED))
                .thenReturn(List.of(existing));
        TourBookingValidation validation = new TourBookingValidation(repository);

        Tour tour = tour(packageOption(2, 0), MealPlan.BB);
        BookingRequest req = request(1, 0, MealPlan.BB, DURATION, START_DATE, validDetails(1));

        assertThatThrownBy(() -> validation.validateBookingCreation(req, tour, "user-1"))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("already booked this tour");
    }

    @Test
    void validateBookingCreation_allowsBookingSameTour_whenPriorBookingIsOnDifferentDate() {
        BookingRepository repository = mock(BookingRepository.class);
        Booking existing = Booking.builder()
                .bookingId("existing")
                .tourId("tour-1")
                .state(BookingStatus.BOOKED)
                .startDate(START_DATE.minusMonths(1))
                .build();
        when(repository.getBookingsByCustomerIdFilterStatus("user-1", BookingStatus.BOOKED))
                .thenReturn(List.of(existing));
        TourBookingValidation validation = new TourBookingValidation(repository);

        Tour tour = tour(packageOption(2, 0), MealPlan.BB);
        BookingRequest req = request(1, 0, MealPlan.BB, DURATION, START_DATE, validDetails(1));

        validation.validateBookingCreation(req, tour, "user-1");
    }

    @Test
    void validateBookingEditing_unchangedDateAndDuration_returnsNewOptionAndNullOldOption() {
        BookingRepository repository = mock(BookingRepository.class);
        TourBookingValidation validation = new TourBookingValidation(repository);

        TourPackageOption option = packageOption(5, 5);
        Tour tour = tour(option, MealPlan.BB);
        Booking current = Booking.builder()
                .bookingId("b-1")
                .startDate(START_DATE)
                .duration(DURATION)
                .guests(guests(1, 0))
                .build();
        BookingRequest req = request(2, 1, MealPlan.BB, DURATION, START_DATE, validDetails(3));

        NewOldPackageOptions result =
                validation.validateBookingEditing(req, tour, current);

        assertThat(result.newTourPackageOption()).isEqualTo(option);
        assertThat(result.assumedOldTourPackageOption()).isNull();
    }

    @Test
    void validateBookingEditing_throws_whenNewPackageHasNoCapacity() {
        BookingRepository repository = mock(BookingRepository.class);
        TourBookingValidation validation = new TourBookingValidation(repository);

        TourPackageOption option = packageOption(0, 0);
        Tour tour = tour(option, MealPlan.BB);
        Booking current = Booking.builder()
                .bookingId("b-1")
                .startDate(START_DATE)
                .duration(DURATION)
                .guests(guests(0, 0))
                .build();
        BookingRequest req = request(2, 0, MealPlan.BB, DURATION, START_DATE, validDetails(2));

        assertThatThrownBy(() -> validation.validateBookingEditing(req, tour, current))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("No package found");
    }

    @Test
    void validateBookingEditing_newDateOrDuration_picksCurrentPackageWithMaxAdultSpots() {
        BookingRepository repository = mock(BookingRepository.class);
        TourBookingValidation validation = new TourBookingValidation(repository);

        TourPackageOption oldLowCapacity = packageOption(2, 2);
        oldLowCapacity.setStartDate(START_DATE.minusDays(3));
        oldLowCapacity.setDuration("7 days");

        TourPackageOption oldHighCapacity = packageOption(6, 2);
        oldHighCapacity.setStartDate(START_DATE.minusDays(3));
        oldHighCapacity.setDuration("7 days");

        TourPackageOption newOption = packageOption(4, 3);

        Tour tour = tour(newOption, MealPlan.BB);
        tour.setTourPackageOptions(List.of(oldLowCapacity, oldHighCapacity, newOption));

        Booking current = Booking.builder()
                .bookingId("b-1")
                .startDate(START_DATE.minusDays(3))
                .duration("7 days")
                .guests(guests(1, 0))
                .build();

        BookingRequest req = request(2, 1, MealPlan.BB, DURATION, START_DATE, validDetails(3));

        NewOldPackageOptions result = validation.validateBookingEditing(req, tour, current);

        assertThat(result.newTourPackageOption()).isEqualTo(newOption);
        assertThat(result.assumedOldTourPackageOption()).isEqualTo(oldHighCapacity);
    }

    @Test
    void validateBookingEditing_newDateOrDuration_throwsWhenCurrentPackageCannotBeFound() {
        BookingRepository repository = mock(BookingRepository.class);
        TourBookingValidation validation = new TourBookingValidation(repository);

        TourPackageOption onlyNewOption = packageOption(4, 3);
        Tour tour = tour(onlyNewOption, MealPlan.BB);

        Booking current = Booking.builder()
                .bookingId("b-1")
                .startDate(START_DATE.minusDays(3))
                .duration("7 days")
                .guests(guests(1, 0))
                .build();

        BookingRequest req = request(2, 1, MealPlan.BB, DURATION, START_DATE, validDetails(3));

        assertThatThrownBy(() -> validation.validateBookingEditing(req, tour, current))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("Package not found, please contact your travel agent");
    }
}
