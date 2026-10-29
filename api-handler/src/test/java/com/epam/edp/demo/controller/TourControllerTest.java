package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.tour.DestinationListResponseDTO;
import com.epam.edp.demo.dto.tour.TourDetailsDTO;
import com.epam.edp.demo.dto.tour.TourListResponseDTO;
import com.epam.edp.demo.dto.tour.TourRequestDTO;
import com.epam.edp.demo.exception.tour.TourNotFoundException;
import com.epam.edp.demo.filter.JwtAuthenticationFilter;
import com.epam.edp.demo.service.tour.TourService;
import com.epam.edp.demo.validation.tour.TourSearchValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TourController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(TourSearchValidator.class)
class TourControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TourService tourService;

    @Test
    void getTourById_returnsOk_whenServiceReturnsDetails() throws Exception {
        TourDetailsDTO dto = TourDetailsDTO.builder()
                .id("tour-001")
                .name("Paris weekender")
                .destination("Paris")
                .rating(4.7)
                .imageUrls(List.of("https:example.com/img.jpg"))
                .summary("A great weekend in Paris")
                .freeCancelationDaysBefore(14)
                .accommodation("Hotel")
                .hotelName("Le Grand")
                .hotelDescription("Lovely hotel")
                .mealPlans(List.of("BB"))
                .customDetails(Map.of())
                .guestQuantity(null)
                .tourPackageOption(List.of())
                .mealSupplementsPerDay(Map.of())
                .build();
        when(tourService.getTourById(eq("tour-001"))).thenReturn(dto);

        mockMvc.perform(get("/tours/tour-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tour-001"))
                .andExpect(jsonPath("$.name").value("Paris weekender"));
    }

    @Test
    void getTourById_returns404_whenTourNotFoundExceptionThrown() throws Exception {
        when(tourService.getTourById(eq("missing-id")))
                .thenThrow(new TourNotFoundException("tour with id missing-id doesn't exist"));

        mockMvc.perform(get("/tours/missing-id"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("tour with id missing-id doesn't exist"));
    }

    @Test
    void getTourById_returns404_whenGenericExceptionThrown() throws Exception {
        when(tourService.getTourById(eq("boom-id")))
                .thenThrow(new RuntimeException("backend down"));

        mockMvc.perform(get("/tours/boom-id"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("tour not found"));
    }

    @Test
    void getDestinations_returnsOk_whenQueryAtLeast3Chars() throws Exception {
        DestinationListResponseDTO dto = new DestinationListResponseDTO(List.of("Paris", "Parma"));
        when(tourService.getDestinations(eq("par"))).thenReturn(dto);

        mockMvc.perform(get("/tours/destinations").param("destination", "par"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinations[0]").value("Paris"))
                .andExpect(jsonPath("$.destinations[1]").value("Parma"));

        verify(tourService).getDestinations(eq("par"));
    }

    @Test
    void getDestinations_returns400_whenQueryMissing() throws Exception {
        mockMvc.perform(get("/tours/destinations"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Destination query must contain at least 3 characters"));

        verifyNoInteractions(tourService);
    }

    @Test
    void getDestinations_returns400_whenQueryEmpty() throws Exception {
        mockMvc.perform(get("/tours/destinations").param("destination", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Destination query must contain at least 3 characters"));

        verifyNoInteractions(tourService);
    }

    @Test
    void getDestinations_returns400_whenQueryLength1() throws Exception {
        mockMvc.perform(get("/tours/destinations").param("destination", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Destination query must contain at least 3 characters"));

        verifyNoInteractions(tourService);
    }

    @Test
    void getDestinations_returns400_whenQueryLength2() throws Exception {
        mockMvc.perform(get("/tours/destinations").param("destination", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Destination query must contain at least 3 characters"));

        verifyNoInteractions(tourService);
    }

    @Test
    void getDestinations_returns400_whenQueryWhitespaceTrimsBelow3() throws Exception {
        mockMvc.perform(get("/tours/destinations").param("destination", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Destination query must contain at least 3 characters"));

        verifyNoInteractions(tourService);
    }

    @Test
    void getDestinations_returns404_whenServiceThrows() throws Exception {
        when(tourService.getDestinations(eq("par")))
                .thenThrow(new RuntimeException("backend down"));

        mockMvc.perform(get("/tours/destinations").param("destination", "par"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("no destinations available"));
    }

    @Test
    void getAvailableTours_returnsOk_whenServiceReturnsList() throws Exception {
        TourListResponseDTO response = new TourListResponseDTO(List.of(), 1, 6, 0, 0);
        when(tourService.getAvailableTours(any(TourRequestDTO.class))).thenReturn(response);

        mockMvc.perform(get("/tours/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(6));
    }

    @Test
    void getAvailableTours_returns400_whenSortByInvalid() throws Exception {
        mockMvc.perform(get("/tours/available").param("sortBy", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.sortBy")
                        .value("must be one of: [RATING_DESC, RATING_ASC, PRICE_DESC, PRICE_ASC]"));

        verifyNoInteractions(tourService);
    }

    @Test
    void getAvailableTours_returns404_whenServiceThrows() throws Exception {
        when(tourService.getAvailableTours(any(TourRequestDTO.class)))
                .thenThrow(new RuntimeException("backend down"));

        mockMvc.perform(get("/tours/available"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("no tours available"));
    }
}