package com.epam.edp.demo.repository.tour;

import com.epam.edp.demo.entity.booking.Guests;
import com.epam.edp.demo.entity.tourModel.Tour;
import com.epam.edp.demo.entity.tourModel.TourPackageOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class TourRepositoryImpl implements TourRepository {

    private final MongoTemplate mongoTemplate;

    public TourRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<Tour> findTourById(String tourId) {
        log.info("Fetching tour by id {}", tourId);
        Tour tour = mongoTemplate.findById(tourId, Tour.class);
        log.info("Fetched tour by id {}", tourId);
        return Optional.ofNullable(tour);
    }

    @Override
    public List<Tour> findAll() {
        log.info("fetching all tours");
        try {
            List<Tour> tours = mongoTemplate.findAll(Tour.class);
            log.info("successfully fetched tours, total items count: {}", tours.size());
            return tours;
        } catch (Exception e) {
            log.error("failed to fetch tours", e);
            throw e;
        }
    }

    @Override
    public List<String> findDestinations(String query) {
        log.info("Searching destinations by query: {}", query);
        String normalizedQuery = query == null ? "" : query.trim();
        try {
            Query mongoQuery = new Query();
            if (!normalizedQuery.isEmpty()) {
                mongoQuery.addCriteria(Criteria.where("destination")
                        .regex(Pattern.quote(normalizedQuery), "i"));
            }
            List<String> destinations = mongoTemplate.findDistinct(
                            mongoQuery, "destination", Tour.class, String.class)
                    .stream()
                    .filter(destination -> destination != null && !destination.isBlank())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            log.info("Successfully found {} destinations for query: {}", destinations.size(), query);
            return destinations;
        } catch (Exception e) {
            log.error("Failed to search destinations for query: {}", query, e);
            throw new RuntimeException("Failed to retrieve destinations", e);
        }
    }

    @Override
    public void saveTour(Tour tour) {
        log.info("Saving tour with id {}", tour.getTourId());
        mongoTemplate.save(tour);
        log.info("Tour saved with id: {}", tour.getTourId());
    }

    @Override
    public void saveAll(List<Tour> tours) {
        if (tours.isEmpty()) {
            log.info("No tours to save");
            return;
        }
        log.info("Saving {} tours in batch", tours.size());
        mongoTemplate.insert(tours, Tour.class);
        log.info("Saved {} tours", tours.size());
    }

    @Override
    public void updateBookedQuantity(Tour tour, TourPackageOption packageOption, Guests guests, boolean increase) {
        log.info("updating tour package option for tour {}", tour.getTourId());

        int currentAdults = packageOption.getAvailableAdultSpots();
        int currentChildren = packageOption.getAvailableChildSpots();

        int newAdults;
        int newChildren;

        if (!increase) {
            newAdults = currentAdults - guests.getAdults();
            newChildren = currentChildren - guests.getChildren();
        } else {
            newAdults = currentAdults + guests.getAdults();
            newChildren = currentChildren + guests.getChildren();
        }

        packageOption.setAvailableAdultSpots(newAdults);
        packageOption.setAvailableChildSpots(newChildren);

        log.info("updated tour package option for tour {}", tour.getTourId());
        saveTour(tour);
    }

    @Override
    public void updateBookedQuantity(Tour tour,
                                     TourPackageOption oldPackageOption,
                                     TourPackageOption updatedPackageOption,
                                     Guests oldGuests,
                                     Guests updatedGuests
    ) {
        if (oldPackageOption != null) {
            oldPackageOption.setAvailableAdultSpots(oldPackageOption.getAvailableAdultSpots() + oldGuests.getAdults());
            oldPackageOption.setAvailableChildSpots(oldPackageOption.getAvailableChildSpots() + oldGuests.getChildren());
        } else {
            updatedPackageOption.setAvailableAdultSpots(updatedPackageOption.getAvailableAdultSpots() + oldGuests.getAdults());
            updatedPackageOption.setAvailableChildSpots(updatedPackageOption.getAvailableChildSpots() + oldGuests.getChildren());
        }

        updatedPackageOption.setAvailableAdultSpots(updatedPackageOption.getAvailableAdultSpots() - updatedGuests.getAdults());
        updatedPackageOption.setAvailableChildSpots(updatedPackageOption.getAvailableChildSpots() - updatedGuests.getChildren());

        saveTour(tour);
    }
}
