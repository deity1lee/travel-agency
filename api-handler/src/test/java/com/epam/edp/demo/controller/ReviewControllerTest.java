package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.review.CreateReviewRequestDTO;
import com.epam.edp.demo.dto.review.ReviewListResponseDTO;
import com.epam.edp.demo.dto.review.ReviewResponseDTO;
import com.epam.edp.demo.dto.review.UpdateReviewRequestDTO;
import com.epam.edp.demo.config.WebMvcConfig;
import com.epam.edp.demo.dto.tour.TourReviewQueryDTO;
import com.epam.edp.demo.entity.tourModel.ReviewSortBy;
import com.epam.edp.demo.entity.auth.CustomUserDetails;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.exception.booking.BookingNotFoundException;
import com.epam.edp.demo.dto.moderation.ModerationResult;
import com.epam.edp.demo.dto.moderation.ModerationStatus;
import com.epam.edp.demo.exception.review.ReviewConflictException;
import com.epam.edp.demo.exception.review.ReviewControllerAdvice;
import com.epam.edp.demo.exception.review.ReviewModerationRejectedException;
import com.epam.edp.demo.exception.review.ReviewNotFoundException;
import com.epam.edp.demo.exception.tour.TourValidationException;
import com.epam.edp.demo.filter.JwtAuthenticationFilter;
import com.epam.edp.demo.service.review.ReviewService;
import com.epam.edp.demo.validation.review.TourReviewValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import({ReviewControllerAdvice.class, WebMvcConfig.class, TourReviewValidator.class})
class ReviewControllerTest {

    private static RequestPostProcessor authedUser() {
        User user = User.builder()
                .id("user-1")
                .email("jane@example.com")
                .role(Role.CUSTOMER)
                .build();
        CustomUserDetails details = new CustomUserDetails(user);
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
            return request;
        };
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @Test
    void getTourReviews_returnsOkWithReviewList() throws Exception {
        ReviewListResponseDTO response = new ReviewListResponseDTO(
                List.of(new ReviewResponseDTO("Jane", null, "2026-06-01T10:00:00Z", 5, "Great")),
                1, 4, 1, 1);
        when(reviewService.getTourReviews(anyString(), any(TourReviewQueryDTO.class))).thenReturn(response);

        mockMvc.perform(get("/tours/tour-1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews[0].authorName").value("Jane"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(4))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void getTourReviews_passesPaginationAndSortToService() throws Exception {
        ReviewListResponseDTO response = new ReviewListResponseDTO(List.of(), 2, 10, 0, 0);
        when(reviewService.getTourReviews(anyString(), any(TourReviewQueryDTO.class))).thenReturn(response);

        mockMvc.perform(get("/tours/tour-1/reviews")
                        .param("page", "2")
                        .param("pageSize", "10")
                        .param("sortBy", "NEWEST"))
                .andExpect(status().isOk());

        verify(reviewService).getTourReviews(eq("tour-1"), eq(new TourReviewQueryDTO(
                2, 10, ReviewSortBy.NEWEST)));
    }

    @Test
    void createReview_returns201WithReviewResponse() throws Exception {
        CreateReviewRequestDTO request = new CreateReviewRequestDTO(5, "Great");
        ReviewResponseDTO response = new ReviewResponseDTO("Jane", null, "2026-06-16T10:00:00Z", 5, "Great");
        when(reviewService.createReview(eq("booking-1"), any(), any(CreateReviewRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/bookings/booking-1/review")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Jane"))
                .andExpect(jsonPath("$.rate").value(5));
    }

    @Test
    void createReview_returns400_whenServiceThrowsValidationException() throws Exception {
        CreateReviewRequestDTO request = new CreateReviewRequestDTO(0, null);
        when(reviewService.createReview(eq("booking-1"), any(), any(CreateReviewRequestDTO.class)))
                .thenThrow(new TourValidationException(Map.of("rate", "must be between 1 and 5")));

        mockMvc.perform(post("/bookings/booking-1/review")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.rate").value("must be between 1 and 5"));
    }

    @Test
    void createReview_returns409_whenServiceThrowsConflict() throws Exception {
        CreateReviewRequestDTO request = new CreateReviewRequestDTO(5, "Great");
        when(reviewService.createReview(eq("booking-1"), any(), any(CreateReviewRequestDTO.class)))
                .thenThrow(new ReviewConflictException("A review already exists for this booking. You can only update it."));

        mockMvc.perform(post("/bookings/booking-1/review")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("A review already exists for this booking. You can only update it."));
    }

    @Test
    void createReview_returns422_whenModerationRejects() throws Exception {
        CreateReviewRequestDTO request = new CreateReviewRequestDTO(5, "I will hurt you");
        ModerationResult verdict = new ModerationResult(
                ModerationStatus.REJECTED, "contains a threat", List.of("THREAT"),
                "Please remove threatening language and resubmit.");
        when(reviewService.createReview(eq("booking-1"), any(), any(CreateReviewRequestDTO.class)))
                .thenThrow(new ReviewModerationRejectedException(verdict));

        mockMvc.perform(post("/bookings/booking-1/review")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.action").value("BLOCKED"))
                .andExpect(jsonPath("$.reason").value("contains a threat"))
                .andExpect(jsonPath("$.categories[0]").value("THREAT"));
    }

    @Test
    void createReview_returns422WithEditAction_whenModerationNeedsEdit() throws Exception {
        CreateReviewRequestDTO request = new CreateReviewRequestDTO(5, "damn this tour");
        ModerationResult verdict = new ModerationResult(
                ModerationStatus.NEEDS_EDIT, "mild profanity", List.of("PROFANITY"),
                "Please rephrase and resubmit.");
        when(reviewService.createReview(eq("booking-1"), any(), any(CreateReviewRequestDTO.class)))
                .thenThrow(new ReviewModerationRejectedException(verdict));

        mockMvc.perform(post("/bookings/booking-1/review")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("NEEDS_EDIT"))
                .andExpect(jsonPath("$.action").value("EDIT_AND_RESUBMIT"))
                .andExpect(jsonPath("$.userMessage").value("Please rephrase and resubmit."));
    }

    @Test
    void createReview_returns404_whenBookingNotFound() throws Exception {
        CreateReviewRequestDTO request = new CreateReviewRequestDTO(5, "Great");
        when(reviewService.createReview(eq("missing"), any(), any(CreateReviewRequestDTO.class)))
                .thenThrow(new BookingNotFoundException("booking not found"));

        mockMvc.perform(post("/bookings/missing/review")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("booking not found"));
    }

    @Test
    void updateReview_returns200WithReviewResponse() throws Exception {
        UpdateReviewRequestDTO request = new UpdateReviewRequestDTO(4, "updated");
        ReviewResponseDTO response = new ReviewResponseDTO("Jane", null, "2026-06-16T10:00:00Z", 4, "updated");
        when(reviewService.updateReview(eq("booking-1"), any(), any(UpdateReviewRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(put("/bookings/booking-1/review")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(4))
                .andExpect(jsonPath("$.reviewContent").value("updated"));
    }

    @Test
    void updateReview_returns404_whenReviewNotFound() throws Exception {
        UpdateReviewRequestDTO request = new UpdateReviewRequestDTO(4, "updated");
        when(reviewService.updateReview(eq("booking-1"), any(), any(UpdateReviewRequestDTO.class)))
                .thenThrow(new ReviewNotFoundException("review not found"));

        mockMvc.perform(put("/bookings/booking-1/review")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("review not found"));
    }

    @Test
    void updateReview_returns400_whenValidationFails() throws Exception {
        UpdateReviewRequestDTO request = new UpdateReviewRequestDTO(7, null);
        when(reviewService.updateReview(eq("booking-1"), any(), any(UpdateReviewRequestDTO.class)))
                .thenThrow(new TourValidationException(Map.of("rate", "must be between 1 and 5")));

        mockMvc.perform(put("/bookings/booking-1/review")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rate").value("must be between 1 and 5"));
    }

    @Test
    void updateReview_returns409_whenConflict() throws Exception {
        UpdateReviewRequestDTO request = new UpdateReviewRequestDTO(5, "updated");
        when(reviewService.updateReview(eq("booking-1"), any(), any(UpdateReviewRequestDTO.class)))
                .thenThrow(new ReviewConflictException("conflict, please retry"));

        mockMvc.perform(put("/bookings/booking-1/review")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("conflict, please retry"));
    }
}
