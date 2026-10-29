package com.epam.edp.demo.service.tour;

import com.epam.edp.demo.dto.tour.*;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.tourModel.*;
import com.epam.edp.demo.exception.booking.BookingValidationException;
import com.epam.edp.demo.exception.tour.TourNotFoundException;
import com.epam.edp.demo.mapper.TourMapper;
import com.epam.edp.demo.repository.tour.TourRepository;
import com.epam.edp.demo.util.TourDurationParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepository;

    public TourServiceImpl(TourRepository tourRepository) {
        this.tourRepository = tourRepository;
    }

    private record TourWithStartDates(Tour tour, List<LocalDate> startDates) {
    }

    @Override
    public TourListResponseDTO getAvailableTours(TourRequestDTO request) {
        log.info("getting available tours with request: {}", request);

        List<TourWithStartDates> filteredTours = tourRepository.findAll().stream()
                .filter(Tour::isAvailable)
                .filter(tour -> matchesDestination(tour, request.destination()))
                .map(tour -> new TourWithStartDates(tour, optionStartDates(tour)))
                .filter(item -> matchesRequest(item, request))
                .sorted(Comparator.comparing((TourWithStartDates item) -> item.tour, getComparator(request.sortBy())))
                .toList();

        int totalItems = filteredTours.size();
        int page = request.page();
        int pageSize = request.pageSize();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / pageSize);
        int fromIndex = (page - 1) * pageSize;

        if (fromIndex >= totalItems) {
            log.info("requested page {} is out of range. totalItems: {}, totalPages: {}",
                    page, totalItems, totalPages);
            return new TourListResponseDTO(List.of(), page, pageSize, totalPages, totalItems);
        }

        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        log.info("filtered tours count: {}, page: {}, pageSize: {}, totalPages: {}",
                totalItems, page, pageSize, totalPages);

        List<TourListItemDTO> responseTours = filteredTours.subList(fromIndex, toIndex).stream()
                .map(item -> toTourListItemDTO(item.tour, item.startDates, request))
                .toList();

        log.info("returning {} tours for page {}", responseTours.size(), page);
        return new TourListResponseDTO(responseTours, page, pageSize, totalPages, totalItems);
    }

    @Override
    public DestinationListResponseDTO getDestinations(String prefix) {
        String query = prefix == null ? "" : prefix;
        return new DestinationListResponseDTO(tourRepository.findDestinations(query));
    }

    @Override
    public TourDetailsDTO getTourById(String tourId) {
        log.info("Fetching tour by id {}", tourId);
        Tour tour = tourRepository.findTourById(tourId).orElseThrow(() -> {
            log.warn("Tour with id {} not found", tourId);
            return new TourNotFoundException(String.format("tour with id %s doesn't exist", tourId));
        });
        log.info("Converting tour {} to response DTO", tourId);
        TourDetailsDTO response = TourMapper.getTourDetailsFromTour(tour);
        log.info("Returning tour {} details", tourId);
        return response;
    }

    @Override
    public void increaseTourOption(Booking booking) {
        Tour tour = tourRepository.findTourById(booking.getTourId())
                .orElseThrow(() -> new TourNotFoundException(String.format("Tour with id %s not found", booking.getTourId())));

        TourPackageOption tourPackageOption = tour.getTourPackageOptions().stream()
                .filter(option ->
                        option.getStartDate().equals(booking.getStartDate()) &&
                                option.getDuration().equals(booking.getDuration())
                ).findAny().orElseThrow(() -> new BookingValidationException("Tour package does not exist"));

        tourRepository.updateBookedQuantity(tour, tourPackageOption, booking.getGuests(), true);
    }

    private boolean matchesRequest(TourWithStartDates item, TourRequestDTO request) {
        return matchesStartDateRange(item.startDates, request)
                && matchesDurationBuckets(item.tour, request.durations())
                && matchesGuestCapacity(item.tour, request)
                && matchesMealPlans(item.tour, request.mealPlans())
                && matchesTourTypes(item.tour, request.tourTypes());
    }

    private boolean matchesDestination(Tour tour, String destination) {
        return isAnyDestination(destination) || containsIgnoreCase(tour.getDestination(), destination);
    }

    private boolean matchesMealPlans(Tour tour, List<MealPlan> mealPlans) {
        return isEmpty(mealPlans) || matchesAnyMealPlan(tour.getMealSupplements(), mealPlans);
    }

    private boolean matchesTourTypes(Tour tour, List<TourType> tourTypes) {
        return isEmpty(tourTypes)
                || (tour.getTourType() != null && tourTypes.contains(tour.getTourType()));
    }

    private Comparator<Tour> getComparator(TourSortBy sortBy) {
        TourSortBy effective = sortBy == null ? TourSortBy.RATING_DESC : sortBy;
        log.info("sorting tours by {}", effective);
        return switch (effective) {
            case PRICE_DESC  -> Comparator.comparing(TourServiceImpl::minPackagePrice, Comparator.nullsLast(Comparator.reverseOrder()));
            case PRICE_ASC   -> Comparator.comparing(TourServiceImpl::minPackagePrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case RATING_ASC  -> Comparator.comparing(Tour::getRating, Comparator.nullsLast(Comparator.naturalOrder()));
            default          -> Comparator.comparing(Tour::getRating, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    private TourListItemDTO toTourListItemDTO(Tour tour, List<LocalDate> startDates, TourRequestDTO request) {
        Integer price = minPackagePrice(tour);
        LocalDate startDate = resolveStartDate(startDates, request);
        return new TourListItemDTO(
                tour.getTourId(),
                tour.getName(),
                tour.getDestination(),
                startDate,
                tour.getDurations() == null ? List.of() : tour.getDurations(),
                mealPlanLabels(tour.getMealSupplements()),
                price == null ? null : "from $" + price + " for 1 person",
                tour.getRating(),
                tour.getReviews() == null ? 0 : tour.getReviews(),
                resolveFreeCancelation(startDate, tour.getFreeCancelationDaysBefore()),
                firstImageUrl(tour.getImageUrls()));
    }

    private String firstImageUrl(List<String> imageUrls) {
        return isEmpty(imageUrls) ? null : imageUrls.get(0);
    }

    private List<String> mealPlanLabels(List<MealSupplement> supplements) {
        if (supplements == null) {
            return List.of();
        }
        return supplements.stream()
                .map(MealSupplement::getMealPlan)
                .filter(Objects::nonNull)
                .map(plan -> formatMealPlan(plan.name()))
                .toList();
    }

    private LocalDate resolveFreeCancelation(LocalDate startDate, Integer daysBefore) {
        if (startDate == null || daysBefore == null) {
            return null;
        }
        return startDate.minusDays(daysBefore);
    }

    private boolean matchesStartDateRange(List<LocalDate> startDates, TourRequestDTO request) {
        if (request.startDateFrom() == null && request.startDateTo() == null) {
            return true;
        }
        return startDates.stream().anyMatch(date -> isWithinRange(date, request));
    }

    private LocalDate resolveStartDate(List<LocalDate> startDates, TourRequestDTO request) {
        boolean hasRange = request.startDateFrom() != null || request.startDateTo() != null;
        return startDates.stream()
                .filter(date -> !hasRange || isWithinRange(date, request))
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private boolean isWithinRange(LocalDate date, TourRequestDTO request) {
        LocalDate from = request.startDateFrom();
        LocalDate to = request.startDateTo();
        return (from == null || !date.isBefore(from))
                && (to == null || !date.isAfter(to));
    }

    private static Integer minPackagePrice(Tour tour) {
        List<TourPackageOption> options = tour.getTourPackageOptions();
        if (options == null) {
            return null;
        }
        return options.stream()
                .filter(Objects::nonNull)
                .map(TourPackageOption::getPrice)
                .filter(price -> price > 0)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private List<LocalDate> optionStartDates(Tour tour) {
        if (tour.getTourPackageOptions() == null) {
            return List.of();
        }
        return tour.getTourPackageOptions().stream()
                .map(TourPackageOption::getStartDate)
                .toList();
    }

    private boolean isAnyDestination(String destination) {
        return isBlank(destination) || "Any destination".equalsIgnoreCase(destination.trim());
    }

    private boolean matchesDurationBuckets(Tour tour, List<DurationBucket> buckets) {
        if (isEmpty(buckets)) {
            return true;
        }
        List<String> tourDurations = tour.getDurations();
        if (isEmpty(tourDurations)) {
            return false;
        }
        return tourDurations.stream()
                .map(TourDurationParser::parseDays)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(days -> buckets.stream().anyMatch(b -> bucketContains(b, days)));
    }

    private boolean bucketContains(DurationBucket bucket, int days) {
        return days >= bucket.getMinDays()
                && (bucket.getMaxDays() == null || days <= bucket.getMaxDays());
    }

    private boolean matchesAnyMealPlan(List<MealSupplement> supplements, List<MealPlan> requestedMealPlans) {
        if (isEmpty(supplements)) {
            return false;
        }
        Set<MealPlan> availablePlans = supplements.stream()
                .map(MealSupplement::getMealPlan)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return requestedMealPlans.stream().anyMatch(availablePlans::contains);
    }

    private boolean matchesGuestCapacity(Tour tour, TourRequestDTO dto) {
        GuestQuantity capacity = tour.getGuestQuantity();
        if (capacity == null) {
            return false;
        }
        int adults = dto.adults() == null ? 1 : dto.adults();
        int children = dto.children() == null ? 0 : dto.children();
        return adults <= capacity.getAdultsMaxValue()
                && children <= capacity.getChildrenMaxValue()
                && (adults + children) <= capacity.getTotalMaxValue();
    }

    private String formatMealPlan(String mealPlan) {
        return switch (mealPlan) {
            case "BB" -> "Breakfast (BB)";
            case "HB" -> "Half-board (HB)";
            case "FB" -> "Full-board (FB)";
            case "AI" -> "All inclusive (AI)";
            default   -> mealPlan;
        };
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null
                && search != null
                && value.toLowerCase().contains(search.toLowerCase());
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
