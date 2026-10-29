package com.epam.edp.demo.entity.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import com.epam.edp.demo.validation.booking.ValidGuestCount;

@Getter
@Setter
@ValidGuestCount
public class Guests {

    @Schema(description = "Number of the adults in the booking", example = "1")
    @NotNull(message = "Number of the adults must be specified")
    @Positive(message = "Number of the adults must be positive")
    @Min(value = 1, message = "At least 1 adult is required")
    private Integer adults;

    @Schema(description = "Number of the children in the booking", example = "0")
    @NotNull(message = "Number of children must be specified")
    @PositiveOrZero(message = "Number of children must be positive or zero")
    private Integer children;

    @Schema(hidden = true)
    public int getTotalGuests() {
        return adults + children;
    }
}
