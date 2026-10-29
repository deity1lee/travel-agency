package com.epam.edp.demo.repository.tour;

import com.epam.edp.demo.entity.booking.Guests;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.entity.tourModel.TourPackageOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Test
    void findTourById_returnsTour() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        Tour expected = tour("tour-1", "Madrid");
        when(mongoTemplate.findById("tour-1", Tour.class)).thenReturn(expected);

        Optional<Tour> result = repository.findTourById("tour-1");

        assertThat(result).contains(expected);
    }

    @Test
    void findTourById_returnsEmpty_whenNotFound() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        when(mongoTemplate.findById("missing", Tour.class)).thenReturn(null);

        assertThat(repository.findTourById("missing")).isEmpty();
    }

    @Test
    void findAll_returnsAllTours() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        List<Tour> tours = List.of(tour("tour-1", "Paris"), tour("tour-2", "Rome"));
        when(mongoTemplate.findAll(Tour.class)).thenReturn(tours);

        List<Tour> result = repository.findAll();

        assertThat(result).extracting(Tour::getTourId).containsExactly("tour-1", "tour-2");
    }

    @Test
    void saveAll_insertsAllToursInBatch() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        List<Tour> tours = List.of(tour("tour-1", "Paris"), tour("tour-2", "Rome"));

        repository.saveAll(tours);

        verify(mongoTemplate).insert(tours, Tour.class);
    }

    @Test
    void saveAll_withEmptyList_doesNotTouchMongo() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);

        repository.saveAll(List.of());

        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void findAll_propagatesFailure() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        when(mongoTemplate.findAll(Tour.class)).thenThrow(new RuntimeException("mongo down"));

        assertThatThrownBy(repository::findAll)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mongo down");
    }

    @Test
    void findDestinations_returnsDistinctSortedNonBlankDestinations() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        when(mongoTemplate.findDistinct(any(Query.class), eq("destination"), eq(Tour.class), eq(String.class)))
                .thenReturn(Arrays.asList("Paris", "Berlin", "Paris", "Amsterdam", null, "  "));

        List<String> destinations = repository.findDestinations(null);

        assertThat(destinations).containsExactly("Amsterdam", "Berlin", "Paris");
    }

    @Test
    void findDestinations_withQuery_stillPostProcessesResults() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        when(mongoTemplate.findDistinct(any(Query.class), eq("destination"), eq(Tour.class), eq(String.class)))
                .thenReturn(Arrays.asList("Pattaya", "Paris", "paris"));

        List<String> destinations = repository.findDestinations("  Pa  ");

        assertThat(destinations).containsExactly("Paris", "Pattaya", "paris");
    }

    @Test
    void findDestinations_wrapsFailure() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        when(mongoTemplate.findDistinct(any(Query.class), eq("destination"), eq(Tour.class), eq(String.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> repository.findDestinations("par"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to retrieve destinations")
                .hasRootCauseMessage("boom");
    }

    @Test
    void saveTour_savesToMongo() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        Tour input = tour("tour-save", "Lisbon");

        repository.saveTour(input);

        verify(mongoTemplate).save(input);
    }

    @Test
    void updateBookedQuantity_decreasesSpotsAndSavesTour() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        TourPackageOption option = packageOption(5, 3);
        Tour tour = Tour.builder()
                .tourId("tour-1")
                .tourPackageOptions(List.of(option))
                .build();
        Guests guests = guests(2, 1);

        repository.updateBookedQuantity(tour, option, guests, false);

        assertThat(option.getAvailableAdultSpots()).isEqualTo(3);
        assertThat(option.getAvailableChildSpots()).isEqualTo(2);
        verify(mongoTemplate).save(tour);
    }

    @Test
    void updateBookedQuantity_increasesSpotsAndSavesTour() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        TourPackageOption option = packageOption(3, 2);
        Tour tour = Tour.builder()
                .tourId("tour-1")
                .tourPackageOptions(List.of(option))
                .build();
        Guests guests = guests(2, 4);

        repository.updateBookedQuantity(tour, option, guests, true);

        assertThat(option.getAvailableAdultSpots()).isEqualTo(5);
        assertThat(option.getAvailableChildSpots()).isEqualTo(6);
        verify(mongoTemplate).save(tour);
    }

    @Test
    void updateBookedQuantityWithReplacementOption_restoresOldOptionAndAdjustsNewOne() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        TourPackageOption oldOption = packageOption(1, 1);
        TourPackageOption updatedOption = packageOption(8, 5);
        Tour tour = Tour.builder()
                .tourId("tour-1")
                .tourPackageOptions(List.of(oldOption, updatedOption))
                .build();
        Guests oldGuests = guests(2, 1);
        Guests updatedGuests = guests(3, 2);

        repository.updateBookedQuantity(tour, oldOption, updatedOption, oldGuests, updatedGuests);

        assertThat(oldOption.getAvailableAdultSpots()).isEqualTo(3);
        assertThat(oldOption.getAvailableChildSpots()).isEqualTo(2);
        assertThat(updatedOption.getAvailableAdultSpots()).isEqualTo(5);
        assertThat(updatedOption.getAvailableChildSpots()).isEqualTo(3);
        verify(mongoTemplate).save(tour);
    }

    @Test
    void updateBookedQuantityWithReplacementOption_whenOldOptionMissing_usesUpdatedOptionForRestoration() {
        TourRepositoryImpl repository = new TourRepositoryImpl(mongoTemplate);
        TourPackageOption updatedOption = packageOption(6, 7);
        Tour tour = Tour.builder()
                .tourId("tour-1")
                .tourPackageOptions(List.of(updatedOption))
                .build();
        Guests oldGuests = guests(2, 3);
        Guests updatedGuests = guests(4, 1);

        repository.updateBookedQuantity(tour, null, updatedOption, oldGuests, updatedGuests);

        assertThat(updatedOption.getAvailableAdultSpots()).isEqualTo(4);
        assertThat(updatedOption.getAvailableChildSpots()).isEqualTo(9);
        verify(mongoTemplate).save(tour);
    }

    private static Tour tour(String id, String destination) {
        return Tour.builder()
                .tourId(id)
                .name("Tour " + id)
                .destination(destination)
                .build();
    }

    private static TourPackageOption packageOption(int adultSpots, int childSpots) {
        TourPackageOption option = new TourPackageOption();
        option.setAvailableAdultSpots(adultSpots);
        option.setAvailableChildSpots(childSpots);
        return option;
    }

    private static Guests guests(int adults, int children) {
        Guests guests = new Guests();
        guests.setAdults(adults);
        guests.setChildren(children);
        return guests;
    }
}
