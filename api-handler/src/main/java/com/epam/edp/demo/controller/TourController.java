package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.tour.*;
import com.epam.edp.demo.entity.tourModel.MealPlan;
import com.epam.edp.demo.entity.tourModel.TourSortBy;
import com.epam.edp.demo.entity.tourModel.TourType;
import com.epam.edp.demo.service.tour.TourService;
import com.epam.edp.demo.validation.tour.TourSearchValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/tours")
@Tag(name = "Tours", description = "Search, browse, and retrieve tour offerings")
@Slf4j
public class TourController {

    private final TourService tourService;

    @Autowired
    public TourController(TourService tourService) {
        this.tourService = tourService;
    }

    @GetMapping("/available")
    @Operation(summary = "Search available tours")
    @Parameters({
            @Parameter(name = "destination", description = "Destination name filter",
                    schema = @Schema(type = "string")),
            @Parameter(name = "duration", description = "Duration buckets (wire values). Repeat the param to pick multiple.",
                    explode = Explode.TRUE, style = ParameterStyle.FORM,
                    array = @ArraySchema(schema = @Schema(type = "string",
                            allowableValues = {"1-3 days", "4-7 days", "8-12 days", "13+ days"}))),
            @Parameter(name = "startDate",
                    description = "Date range in the format \"MMM d yyyy - MMM d yyyy\"",
                    schema = @Schema(type = "string")),
            @Parameter(name = "adults", description = "Number of adult guests",
                    schema = @Schema(type = "integer", minimum = "1")),
            @Parameter(name = "children", description = "Number of child guests",
                    schema = @Schema(type = "integer", minimum = "0")),
            @Parameter(name = "page", description = "Page number (1-based)",
                    schema = @Schema(type = "integer", defaultValue = "1")),
            @Parameter(name = "pageSize", description = "Items per page",
                    schema = @Schema(type = "integer", defaultValue = "6")),
            @Parameter(name = "mealPlan", description = "Meal plan filter. Repeat the param to pick multiple.",
                    explode = Explode.TRUE, style = ParameterStyle.FORM,
                    array = @ArraySchema(schema = @Schema(implementation = MealPlan.class))),
            @Parameter(name = "tourType", description = "Tour type filter. Repeat the param to pick multiple.",
                    explode = Explode.TRUE, style = ParameterStyle.FORM,
                    array = @ArraySchema(schema = @Schema(implementation = TourType.class))),
            @Parameter(name = "sortBy", description = "Sort order",
                    schema = @Schema(implementation = TourSortBy.class))
    })
    @ApiResponse(responseCode = "200", description = "Available tours page",
            content = @Content(schema = @Schema(implementation = TourListResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
    @ApiResponse(responseCode = "404", description = "No tours available", content = @Content)
    public TourListResponseDTO getAvailableTours(
            @Parameter(hidden = true) @RequestParam Map<String, String> params,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> multi) {
        log.info("Incoming request: GET /tours/available");
        TourRequestDTO dto = TourSearchValidator.validateAndBuild(params, multi);
        return tourService.getAvailableTours(dto);
    }

    @GetMapping("/destinations")
    @Operation(summary = "Autocomplete destination names")
    @ApiResponse(responseCode = "200", description = "Matching destinations",
            content = @Content(schema = @Schema(implementation = DestinationListResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Query too short", content = @Content)
    @ApiResponse(responseCode = "404", description = "No destinations available", content = @Content)
    public ResponseEntity<?> getDestinations(
            @Parameter(description = "Autocomplete query (minimum 3 characters)",
                    schema = @Schema(type = "string", minLength = 3))
            @RequestParam(required = false) String destination) { // why is required false, if we throw exception if it's null below?
        log.info("Incoming request: GET /tours/destinations with ?destination={}.", destination);
        if (destination == null || destination.trim().length() < 3) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Destination query must contain at least 3 characters"));
        }
        return ResponseEntity.ok(tourService.getDestinations(destination));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tour details by id")
    @ApiResponse(responseCode = "200", description = "Tour details",
            content = @Content(schema = @Schema(implementation = TourDetailsDTO.class)))
    @ApiResponse(responseCode = "404", description = "Tour not found", content = @Content)
    public TourDetailsDTO getTourById(
            @Parameter(description = "Tour identifier") @PathVariable String id) {
        log.info("Incoming request: GET /tours/{}", id);
        return tourService.getTourById(id);
    }
}
