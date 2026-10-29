package com.epam.edp.demo.service.tour;

import com.epam.edp.demo.dto.tour.DestinationListResponseDTO;
import com.epam.edp.demo.dto.tour.TourDetailsDTO;
import com.epam.edp.demo.dto.tour.TourListItemDTO;
import com.epam.edp.demo.dto.tour.TourListResponseDTO;
import com.epam.edp.demo.dto.tour.TourRequestDTO;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.Guests;
import com.epam.edp.demo.entity.tourModel.DurationBucket;
import com.epam.edp.demo.entity.tourModel.GuestQuantity;
import com.epam.edp.demo.entity.tourModel.MealPlan;
import com.epam.edp.demo.entity.tourModel.MealSupplement;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.entity.tourModel.TourPackageOption;
import com.epam.edp.demo.entity.tourModel.TourSortBy;
import com.epam.edp.demo.entity.tourModel.TourType;
import com.epam.edp.demo.exception.booking.BookingValidationException;
import com.epam.edp.demo.exception.tour.TourNotFoundException;
import com.epam.edp.demo.repository.tour.TourRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourServiceImplTest {

    private final TourRepository repository = mock(TourRepository.class);
    private final TourServiceImpl service = new TourServiceImpl(repository);

    @Test
    void getAvailableTours_excludesUnavailableTours() {
        Tour available = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        Tour soldOut = tour("t2", "Rome", 4.8, List.of(packageOption("2026-07-01", "7 days", 1500, 0, 0)));
        when(repository.findAll()).thenReturn(List.of(available, soldOut));

        TourListResponseDTO response = service.getAvailableTours(request().build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t1");
        assertThat(response.totalItems()).isEqualTo(1);
    }

    @Test
    void getAvailableTours_returnsSingleEmptyPageWhenNothingMatches() {
        when(repository.findAll()).thenReturn(List.of());

        TourListResponseDTO response = service.getAvailableTours(request().build());

        assertThat(response.tours()).isEmpty();
        assertThat(response.totalItems()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void getAvailableTours_destinationFilterIsCaseInsensitiveContains() {
        Tour paris = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        Tour rome = tour("t2", "Rome", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        when(repository.findAll()).thenReturn(List.of(paris, rome));

        TourListResponseDTO response = service.getAvailableTours(request().destination("PAR").build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t1");
    }

    @Test
    void getAvailableTours_anyDestinationKeywordSkipsFilter() {
        Tour paris = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        Tour rome = tour("t2", "Rome", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        when(repository.findAll()).thenReturn(List.of(paris, rome));

        TourListResponseDTO response = service.getAvailableTours(request().destination("Any destination").build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactlyInAnyOrder("t1", "t2");
    }

    @Test
    void getAvailableTours_filtersByStartDateRange() {
        Tour early = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        Tour late = tour("t2", "Paris", 4.5, List.of(packageOption("2026-12-01", "7 days", 1000, 2, 1)));
        when(repository.findAll()).thenReturn(List.of(early, late));

        TourListResponseDTO response = service.getAvailableTours(request()
                .startDateFrom(LocalDate.parse("2026-06-01"))
                .startDateTo(LocalDate.parse("2026-08-01"))
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t1");
    }

    @Test
    void getAvailableTours_filtersByDurationBucket() {
        Tour shortTour = tour("t1", "Paris", 4.5,
                List.of(packageOption("2026-07-01", "3 days", 500, 2, 1)));
        shortTour.setDurations(List.of("3 days"));
        Tour longTour = tour("t2", "Paris", 4.5,
                List.of(packageOption("2026-07-01", "14 days", 2000, 2, 1)));
        longTour.setDurations(List.of("14 days"));
        when(repository.findAll()).thenReturn(List.of(shortTour, longTour));

        TourListResponseDTO response = service.getAvailableTours(request()
                .durations(List.of(DurationBucket.DAYS_1_3))
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t1");
    }

    @Test
    void getAvailableTours_filtersByMealPlan() {
        Tour bbOnly = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        bbOnly.setMealSupplements(List.of(supplement(MealPlan.BB, 0)));
        Tour aiOnly = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        aiOnly.setMealSupplements(List.of(supplement(MealPlan.AI, 60)));
        when(repository.findAll()).thenReturn(List.of(bbOnly, aiOnly));

        TourListResponseDTO response = service.getAvailableTours(request()
                .mealPlans(List.of(MealPlan.AI))
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t2");
    }

    @Test
    void getAvailableTours_filtersByTourType() {
        Tour resort = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        resort.setTourType(TourType.RESORT);
        Tour hike = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        hike.setTourType(TourType.HIKE);
        when(repository.findAll()).thenReturn(List.of(resort, hike));

        TourListResponseDTO response = service.getAvailableTours(request()
                .tourTypes(List.of(TourType.HIKE))
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t2");
    }

    @Test
    void getAvailableTours_excludesTourWithNullTourTypeWhenTypeFilterProvided() {
        Tour nullType = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        nullType.setTourType(null);
        Tour hike = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        hike.setTourType(TourType.HIKE);
        when(repository.findAll()).thenReturn(List.of(nullType, hike));

        TourListResponseDTO response = service.getAvailableTours(request()
                .tourTypes(List.of(TourType.HIKE))
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t2");
    }

    @Test
    void getAvailableTours_filtersByGuestCapacity() {
        Tour small = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        small.setGuestQuantity(guestQuantity(1, 0, 1));
        Tour large = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        large.setGuestQuantity(guestQuantity(4, 2, 6));
        when(repository.findAll()).thenReturn(List.of(small, large));

        TourListResponseDTO response = service.getAvailableTours(request()
                .adults(3)
                .children(1)
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t2");
    }

    @Test
    void getAvailableTours_guestCapacityUsesDefaultGuestsWhenRequestValuesAreNull() {
        Tour supportsDefault = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        supportsDefault.setGuestQuantity(guestQuantity(1, 0, 1));
        Tour requiresNoAdults = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        requiresNoAdults.setGuestQuantity(guestQuantity(0, 2, 2));
        when(repository.findAll()).thenReturn(List.of(supportsDefault, requiresNoAdults));

        TourListResponseDTO response = service.getAvailableTours(request()
                .adults(null)
                .children(null)
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t1");
    }

    @Test
    void getAvailableTours_excludesTourWhenGuestCapacityMissingOrTotalExceeded() {
        Tour missingCapacity = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        missingCapacity.setGuestQuantity(null);
        Tour totalExceeded = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        totalExceeded.setGuestQuantity(guestQuantity(3, 2, 4));
        Tour matchesAllLimits = tour("t3", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        matchesAllLimits.setGuestQuantity(guestQuantity(3, 2, 5));
        when(repository.findAll()).thenReturn(List.of(missingCapacity, totalExceeded, matchesAllLimits));

        TourListResponseDTO response = service.getAvailableTours(request()
                .adults(3)
                .children(2)
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t3");
    }

    @Test
    void getAvailableTours_excludesTourWhenChildrenExceedCapacity() {
        Tour childrenExceeded = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        childrenExceeded.setGuestQuantity(guestQuantity(3, 1, 6));
        Tour withinLimits = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        withinLimits.setGuestQuantity(guestQuantity(3, 2, 6));
        when(repository.findAll()).thenReturn(List.of(childrenExceeded, withinLimits));

        TourListResponseDTO response = service.getAvailableTours(request()
                .adults(3)
                .children(2)
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t2");
    }

    @Test
    void getAvailableTours_sortsByRatingDescByDefault() {
        Tour low = tour("t1", "Paris", 4.0, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        Tour high = tour("t2", "Paris", 4.9, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        when(repository.findAll()).thenReturn(List.of(low, high));

        TourListResponseDTO response = service.getAvailableTours(request().build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t2", "t1");
    }

    @Test
    void getAvailableTours_sortsByPriceAsc() {
        Tour expensive = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 2000, 2, 1)));
        Tour cheap = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 500, 2, 1)));
        when(repository.findAll()).thenReturn(List.of(expensive, cheap));

        TourListResponseDTO response = service.getAvailableTours(request()
                .sortBy(TourSortBy.PRICE_ASC)
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t2", "t1");
    }

    @Test
    void getAvailableTours_sortsByPriceDesc() {
        Tour expensive = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 2000, 2, 1)));
        Tour cheap = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 500, 2, 1)));
        when(repository.findAll()).thenReturn(List.of(expensive, cheap));

        TourListResponseDTO response = service.getAvailableTours(request()
                .sortBy(TourSortBy.PRICE_DESC)
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t1", "t2");
    }

    @Test
    void getAvailableTours_sortsByRatingAsc() {
        Tour low = tour("t1", "Paris", 4.0, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        Tour high = tour("t2", "Paris", 4.9, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        when(repository.findAll()).thenReturn(List.of(low, high));

        TourListResponseDTO response = service.getAvailableTours(request()
                .sortBy(TourSortBy.RATING_ASC)
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t1", "t2");
    }

    @Test
    void getAvailableTours_paginatesAndComputesTotalPages() {
        when(repository.findAll()).thenReturn(List.of(
                tour("t1", "Paris", 4.1, List.of(packageOption("2026-07-01", "7 days", 100, 2, 1))),
                tour("t2", "Paris", 4.2, List.of(packageOption("2026-07-01", "7 days", 100, 2, 1))),
                tour("t3", "Paris", 4.3, List.of(packageOption("2026-07-01", "7 days", 100, 2, 1))),
                tour("t4", "Paris", 4.4, List.of(packageOption("2026-07-01", "7 days", 100, 2, 1))),
                tour("t5", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 100, 2, 1)))));

        TourListResponseDTO response = service.getAvailableTours(request()
                .page(2)
                .pageSize(2)
                .build());

        assertThat(response.tours()).hasSize(2);
        assertThat(response.totalItems()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.page()).isEqualTo(2);
    }

    @Test
    void getAvailableTours_returnsEmptyPageWhenOutOfRange() {
        when(repository.findAll()).thenReturn(List.of(
                tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 100, 2, 1)))));

        TourListResponseDTO response = service.getAvailableTours(request()
                .page(5)
                .pageSize(2)
                .build());

        assertThat(response.tours()).isEmpty();
        assertThat(response.totalItems()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void getAvailableTours_mapsItemFields() {
        Tour t = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-10", "7 days", 1299, 2, 1)));
        t.setFreeCancelationDaysBefore(14);
        t.setImageUrls(List.of("https://img/one.jpg", "https://img/two.jpg"));
        t.setMealSupplements(List.of(supplement(MealPlan.BB, 0), supplement(MealPlan.HB, 25)));
        t.setReviews(42);
        when(repository.findAll()).thenReturn(List.of(t));

        TourListItemDTO item = service.getAvailableTours(request().build()).tours().get(0);

        assertThat(item.id()).isEqualTo("t1");
        assertThat(item.destination()).isEqualTo("Paris");
        assertThat(item.startDate()).isEqualTo(LocalDate.parse("2026-07-10"));
        assertThat(item.price()).isEqualTo("from $1299 for 1 person");
        assertThat(item.freeCancelation()).isEqualTo(LocalDate.parse("2026-06-26"));
        assertThat(item.imageUrl()).isEqualTo("https://img/one.jpg");
        assertThat(item.mealPlans()).containsExactly("Breakfast (BB)", "Half-board (HB)");
        assertThat(item.reviews()).isEqualTo(42);
    }

    @Test
    void getAvailableTours_priceLabelNullWhenNoPricedOptions() {
        Tour t = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-10", "7 days", 0, 2, 1)));
        when(repository.findAll()).thenReturn(List.of(t));

        TourListItemDTO item = service.getAvailableTours(request().build()).tours().get(0);

        assertThat(item.price()).isNull();
    }

    @Test
    void getAvailableTours_handlesNullOptionalFieldsInResponseMapping() {
        Tour t = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-10", "7 days", 1299, 2, 1)));
        t.setDurations(null);
        t.setMealSupplements(null);
        t.setImageUrls(null);
        t.setReviews(null);
        when(repository.findAll()).thenReturn(List.of(t));

        TourListItemDTO item = service.getAvailableTours(request().build()).tours().get(0);

        assertThat(item.durations()).isEmpty();
        assertThat(item.mealPlans()).isEmpty();
        assertThat(item.imageUrl()).isNull();
        assertThat(item.reviews()).isEqualTo(0);
        assertThat(item.freeCancelation()).isNull();
    }

    @Test
    void getAvailableTours_formatsFullBoardMealPlanLabel() {
        Tour t = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-10", "7 days", 1299, 2, 1)));
        t.setMealSupplements(List.of(supplement(MealPlan.FB, 30)));
        when(repository.findAll()).thenReturn(List.of(t));

        TourListItemDTO item = service.getAvailableTours(request().build()).tours().get(0);

        assertThat(item.mealPlans()).containsExactly("Full-board (FB)");
    }

    @Test
    void getAvailableTours_filtersByOpenEndedDateRangeBoundaries() {
        Tour early = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        Tour late = tour("t2", "Paris", 4.5, List.of(packageOption("2026-09-01", "7 days", 1000, 2, 1)));
        when(repository.findAll()).thenReturn(List.of(early, late));

        TourListResponseDTO fromOnly = service.getAvailableTours(request()
                .startDateFrom(LocalDate.parse("2026-08-01"))
                .build());
        TourListResponseDTO toOnly = service.getAvailableTours(request()
                .startDateTo(LocalDate.parse("2026-08-01"))
                .build());

        assertThat(fromOnly.tours()).extracting(TourListItemDTO::id).containsExactly("t2");
        assertThat(toOnly.tours()).extracting(TourListItemDTO::id).containsExactly("t1");
    }

    @Test
    void getAvailableTours_durationFilterRejectsToursWithMissingDurations() {
        Tour withoutDurations = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        withoutDurations.setDurations(List.of());
        Tour withDuration = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "3 days", 1000, 2, 1)));
        withDuration.setDurations(List.of("3 days"));
        when(repository.findAll()).thenReturn(List.of(withoutDurations, withDuration));

        TourListResponseDTO response = service.getAvailableTours(request()
                .durations(List.of(DurationBucket.DAYS_1_3))
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t2");
    }

    @Test
    void getAvailableTours_mealPlanFilterRejectsToursWithoutSupplements() {
        Tour noSupplements = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        noSupplements.setMealSupplements(List.of());
        Tour aiOnly = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        aiOnly.setMealSupplements(List.of(supplement(MealPlan.AI, 40)));
        when(repository.findAll()).thenReturn(List.of(noSupplements, aiOnly));

        TourListResponseDTO response = service.getAvailableTours(request()
                .mealPlans(List.of(MealPlan.AI))
                .build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t2");
    }

    @Test
    void getAvailableTours_treatsBlankDestinationAsAnyDestination() {
        Tour paris = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        Tour rome = tour("t2", "Rome", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        when(repository.findAll()).thenReturn(List.of(paris, rome));

        TourListResponseDTO response = service.getAvailableTours(request().destination("   ").build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactlyInAnyOrder("t1", "t2");
    }

    @Test
    void getAvailableTours_handlesNullTourDestinationInDestinationMatch() {
        Tour noDestination = tour("t1", null, 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        Tour paris = tour("t2", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        when(repository.findAll()).thenReturn(List.of(noDestination, paris));

        TourListResponseDTO response = service.getAvailableTours(request().destination("par").build());

        assertThat(response.tours()).extracting(TourListItemDTO::id).containsExactly("t2");
    }

    @Test
    void getDestinations_passesPrefixThroughToRepository() {
        when(repository.findDestinations(eq("par"))).thenReturn(List.of("Paris", "Parma"));

        DestinationListResponseDTO response = service.getDestinations("par");

        assertThat(response.destinations()).containsExactly("Paris", "Parma");
        verify(repository).findDestinations("par");
    }

    @Test
    void getDestinations_nullPrefixBecomesEmptyString() {
        when(repository.findDestinations(eq(""))).thenReturn(List.of("Paris"));

        DestinationListResponseDTO response = service.getDestinations(null);

        assertThat(response.destinations()).containsExactly("Paris");
        verify(repository).findDestinations("");
    }

    @Test
    void getTourById_returnsDetailsWhenFound() {
        Tour t = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        t.setMealSupplements(List.of(supplement(MealPlan.BB, 0)));
        when(repository.findTourById("t1")).thenReturn(Optional.of(t));

        TourDetailsDTO details = service.getTourById("t1");

        assertThat(details.id()).isEqualTo("t1");
        assertThat(details.destination()).isEqualTo("Paris");
    }

    @Test
    void getTourById_throwsTourNotFoundExceptionWhenMissing() {
        when(repository.findTourById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTourById("missing"))
                .isInstanceOf(TourNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void increaseTourOption_updatesBookedQuantityForMatchingPackageOption() {
        TourPackageOption option = packageOption("2026-07-01", "7 days", 1200, 2, 1);
        Tour tour = tour("t1", "Paris", 4.5, List.of(option));
        Booking booking = booking("t1", "2026-07-01", "7 days", 2, 1);
        when(repository.findTourById("t1")).thenReturn(Optional.of(tour));

        service.increaseTourOption(booking);

        verify(repository).updateBookedQuantity(eq(tour), eq(option), eq(booking.getGuests()), eq(true));
    }

    @Test
    void increaseTourOption_throwsTourNotFoundExceptionWhenTourMissing() {
        Booking booking = booking("missing", "2026-07-01", "7 days", 1, 0);
        when(repository.findTourById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.increaseTourOption(booking))
                .isInstanceOf(TourNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void increaseTourOption_throwsBookingValidationExceptionWhenPackageOptionNotFound() {
        Tour tour = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "10 days", 1400, 2, 1)));
        Booking booking = booking("t1", "2026-07-01", "7 days", 1, 0);
        when(repository.findTourById("t1")).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> service.increaseTourOption(booking))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("Tour package does not exist");
    }

    @Test
    void increaseTourOption_throwsBookingValidationExceptionWhenStartDateDoesNotMatch() {
        Tour tour = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1400, 2, 1)));
        Booking booking = booking("t1", "2026-07-02", "7 days", 1, 0);
        when(repository.findTourById("t1")).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> service.increaseTourOption(booking))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("Tour package does not exist");
    }

    @Test
    void privateHelpers_coverRemainingBranches() throws Exception {
        Tour tour = tour("t1", "Paris", 4.5, List.of(packageOption("2026-07-01", "7 days", 1000, 2, 1)));
        tour.setTourPackageOptions(null);

        @SuppressWarnings("unchecked")
        List<LocalDate> startDates = (List<LocalDate>) invokePrivate("optionStartDates",
                new Class[]{Tour.class}, tour);
        assertThat(startDates).isEmpty();

        assertThat((Integer) invokePrivateStatic("minPackagePrice", new Class[]{Tour.class}, tour)).isNull();
        assertThat((String) invokePrivate("formatMealPlan", new Class[]{String.class}, "RO")).isEqualTo("RO");
        assertThat((Boolean) invokePrivate("containsIgnoreCase",
                new Class[]{String.class, String.class}, "Paris", null)).isFalse();
        assertThat((Boolean) invokePrivate("bucketContains",
                new Class[]{DurationBucket.class, int.class}, DurationBucket.DAYS_13_PLUS, 12)).isFalse();
        assertThat((Boolean) invokePrivate("bucketContains",
                new Class[]{DurationBucket.class, int.class}, DurationBucket.DAYS_13_PLUS, 20)).isTrue();
        assertThat((LocalDate) invokePrivate("resolveFreeCancelation",
                new Class[]{LocalDate.class, Integer.class}, null, 7)).isNull();

        TourRequestDTO rangeRequest = request()
                .startDateFrom(LocalDate.parse("2026-08-01"))
                .startDateTo(LocalDate.parse("2026-09-01"))
                .build();

        LocalDate resolved = (LocalDate) invokePrivate("resolveStartDate",
                new Class[]{List.class, TourRequestDTO.class}, List.of(LocalDate.parse("2026-07-01")), rangeRequest);
        assertThat(resolved).isNull();
    }

    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = TourServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(service, args);
    }

    private Object invokePrivateStatic(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = TourServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static TourRequestDTO.TourRequestDTOBuilder request() {
        return TourRequestDTO.builder()
                .destination(null)
                .durations(List.of())
                .mealPlans(List.of())
                .tourTypes(List.of())
                .adults(1)
                .children(0)
                .page(1)
                .pageSize(6);
    }

    private static Tour tour(String id, String destination, double rating, List<TourPackageOption> options) {
        return Tour.builder()
                .tourId(id)
                .name("Tour " + id)
                .destination(destination)
                .durations(List.of("7 days"))
                .tourType(TourType.RESORT)
                .rating(rating)
                .reviews(10)
                .guestQuantity(guestQuantity(4, 2, 6))
                .tourPackageOptions(options)
                .mealSupplements(List.of(supplement(MealPlan.BB, 0)))
                .build();
    }

    private static TourPackageOption packageOption(String startDate, String duration, int price,
                                                   int adultSpots, int childSpots) {
        TourPackageOption opt = new TourPackageOption();
        opt.setStartDate(LocalDate.parse(startDate));
        opt.setDuration(duration);
        opt.setPrice(price);
        opt.setAvailableAdultSpots(adultSpots);
        opt.setAvailableChildSpots(childSpots);
        return opt;
    }

    private static MealSupplement supplement(MealPlan plan, int pricePerDay) {
        MealSupplement s = new MealSupplement();
        s.setMealPlan(plan);
        s.setPricePerDay(pricePerDay);
        return s;
    }

    private static GuestQuantity guestQuantity(int adults, int children, int total) {
        GuestQuantity gq = new GuestQuantity();
        gq.setAdultsMaxValue(adults);
        gq.setChildrenMaxValue(children);
        gq.setTotalMaxValue(total);
        return gq;
    }

    private static Booking booking(String tourId, String startDate, String duration, int adults, int children) {
        Guests guests = new Guests();
        guests.setAdults(adults);
        guests.setChildren(children);

        return Booking.builder()
                .tourId(tourId)
                .startDate(LocalDate.parse(startDate))
                .duration(duration)
                .guests(guests)
                .build();
    }
}
