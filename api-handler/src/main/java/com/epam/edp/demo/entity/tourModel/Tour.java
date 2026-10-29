package com.epam.edp.demo.entity.tourModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tours")
public class Tour {
    @Id
    private String tourId;
    private String name;
    private String destination;
    private List<String> durations;
    private TourType tourType;
    private Double rating;
    private Long ratingSum;
    private Integer reviews;
    private String hotelName;
    private String hotelDescription;
    private Double price;
    private List<String> imageUrls;
    private String summary;
    private String accommodation;
    private Map<String, String> customDetails;
    private GuestQuantity guestQuantity;
    private List<TourPackageOption> tourPackageOptions;
    private List<MealSupplement> mealSupplements;
    private Integer freeCancelationDaysBefore;

    public boolean isAvailable() {
        return tourPackageOptions != null
                && tourPackageOptions.stream()
                .anyMatch(option -> option.getAvailableAdultSpots() + option.getAvailableChildSpots() > 0);
    }
}