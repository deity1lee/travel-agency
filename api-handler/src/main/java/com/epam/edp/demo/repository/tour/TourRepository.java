package com.epam.edp.demo.repository.tour;

import com.epam.edp.demo.entity.booking.Guests;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.entity.tourModel.TourPackageOption;

import java.util.List;
import java.util.Optional;

public interface TourRepository {

    Optional<Tour> findTourById(String tourId);

    List<Tour> findAll();

    List<String> findDestinations(String query);

    void saveTour(Tour tour);

    void saveAll(List<Tour> tours);

    void updateBookedQuantity(Tour tour, TourPackageOption tpo, Guests guests, boolean a);

    void updateBookedQuantity(Tour tour,
                              TourPackageOption oldPackageOption,
                              TourPackageOption updatedPackageOption,
                              Guests oldGuests,
                              Guests updatedGuests);
}
