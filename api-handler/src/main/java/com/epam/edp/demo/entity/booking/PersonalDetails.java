package com.epam.edp.demo.entity.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PersonalDetails {

    @Schema(description = "First name of the user", example = "John")
    @NotBlank(message = "First name must not be empty")
    @Pattern(
        regexp = "^[A-Za-z'-]{1,50}$",
        message = "First name is not in valid format"
    )
    private String firstName;

    @Schema(description = "Last name of the user", example = "Doe")
    @NotBlank(message = "Last name must not be empty")
    @Pattern(
        regexp = "^[A-Za-z'-]{1,50}$",
        message = "Last name is not in valid format"
    )
    private String lastName;

    @Schema(hidden = true)
    private String passportDocumentId;

    @Override
    public String toString() {
        return String.join(" ", firstName, lastName);
    }
}
