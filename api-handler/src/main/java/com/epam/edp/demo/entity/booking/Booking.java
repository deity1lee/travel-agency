package com.epam.edp.demo.entity.booking;

import com.epam.edp.demo.entity.tourModel.MealPlan;
import com.epam.edp.demo.entity.tourModel.TourType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class Booking {
    @Id
    private String bookingId;
    @Indexed
    private String userId;
    private String userEmail;
    private String userName;
    @Indexed
    private String tourId;
    private BookingStatus state;
    private String tourImageUrl;
    private String name;
    private String destination;
    private TourType tourType;
    private LocalDate startDate;
    private String duration;
    private MealPlan mealPlan;
    private Guests guests;
    private List<PersonalDetails> personalDetails;
    private int totalPrice;
    private String paymentDocumentId;
    private TravelAgentSnapshot travelAgent;
    private String travelAgentEmail;

    private String canceledBy;
    private String cancelReason;
    private double charged;

    public BookingStatus effectiveStatus() {
        return effectiveStatus(LocalDate.now());
    }

    public BookingStatus effectiveStatus(LocalDate today) {
        if (state == BookingStatus.CANCELED) {
            return BookingStatus.CANCELED;
        }
        int durationDays = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
        LocalDate endDate = startDate.plusDays(durationDays);
        if (!today.isBefore(endDate)) {
            return BookingStatus.FINISHED;
        }
        if (!today.isBefore(startDate)) {
            return BookingStatus.STARTED;
        }
        return state;
    }
}
