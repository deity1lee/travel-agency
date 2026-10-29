package com.epam.edp.demo.controller;

import com.epam.edp.demo.config.WebMvcConfig;
import com.epam.edp.demo.dto.bookings.BookingCancellationResponse;
import com.epam.edp.demo.dto.bookings.BookingDetailsResponse;
import com.epam.edp.demo.dto.bookings.BookingDto;
import com.epam.edp.demo.dto.bookings.BookingListResponse;
import com.epam.edp.demo.dto.bookings.BookingRequest;
import com.epam.edp.demo.dto.bookings.CancelBookingRequest;
import com.epam.edp.demo.entity.auth.CustomUserDetails;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.booking.Guests;
import com.epam.edp.demo.entity.booking.PersonalDetails;
import com.epam.edp.demo.entity.tourModel.MealPlan;
import com.epam.edp.demo.exception.GlobalExceptionHandler;
import com.epam.edp.demo.exception.NoAvailableSpotsException;
import com.epam.edp.demo.exception.booking.BookingAccessDeniedException;
import com.epam.edp.demo.exception.booking.BookingNotFoundException;
import com.epam.edp.demo.exception.booking.BookingStateTransitionException;
import com.epam.edp.demo.exception.booking.BookingValidationException;
import com.epam.edp.demo.filter.JwtAuthenticationFilter;
import com.epam.edp.demo.service.booking.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class})
class BookingControllerTest {

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
    private BookingService bookingService;

    private static BookingRequest sampleBookingRequest() {
        Guests guests = new Guests();
        guests.setAdults(2);
        guests.setChildren(0);
        return new BookingRequest(
                "tour-1",
                LocalDate.of(2027, 3, 1),
                "12 days",
                MealPlan.BB,
                guests,
                List.of(new PersonalDetails("John", "Doe", null),
                        new PersonalDetails("Jane", "Doe", null))
        );
    }

    @Test
    void getBookings_returnsOkWithBookingList() throws Exception {
        BookingDto dto = new BookingDto(
                "booking-1", "tourId", "BOOKED", null, "Tropical Caribe", "Caribbean",
                null, null, null, null, null, null, null, null);
        when(bookingService.getBookings(any(), eq(null)))
                .thenReturn(new BookingListResponse(List.of(dto)));

        mockMvc.perform(get("/bookings").with(authedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings[0].id").value("booking-1"))
                .andExpect(jsonPath("$.bookings[0].state").value("BOOKED"))
                .andExpect(jsonPath("$.bookings[0].name").value("Tropical Caribe"));
    }

    @Test
    void getBookings_passesStatusFilterToService() throws Exception {
        when(bookingService.getBookings(any(), eq(BookingStatus.CONFIRMED)))
                .thenReturn(new BookingListResponse(List.of()));

        mockMvc.perform(get("/bookings").param("status", "CONFIRMED").with(authedUser()))
                .andExpect(status().isOk());

        ArgumentCaptor<UserContext> userCaptor = ArgumentCaptor.forClass(UserContext.class);
        verify(bookingService).getBookings(userCaptor.capture(), eq(BookingStatus.CONFIRMED));
        assertThat(userCaptor.getValue().userId()).isEqualTo("user-1");
        assertThat(userCaptor.getValue().role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void getBookings_returns404_whenBookingServiceThrowsBookingNotFound() throws Exception {
        when(bookingService.getBookings(any(), any()))
                .thenThrow(new BookingNotFoundException("no bookings found"));

        mockMvc.perform(get("/bookings").with(authedUser()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("no bookings found"));
    }

    @Test
    void createBooking_returns201WithDetailsResponse() throws Exception {
        BookingRequest request = sampleBookingRequest();
        BookingDetailsResponse response = new BookingDetailsResponse(
                "2027-02-19", "You have booked tour Tropical Caribe");
        when(bookingService.createBooking(any(), any(BookingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/bookings")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.freeCancellation").value("2027-02-19"))
                .andExpect(jsonPath("$.details").value("You have booked tour Tropical Caribe"));
    }

    @Test
    void createBooking_returns400_whenServiceThrowsBookingValidationException() throws Exception {
        BookingRequest request = sampleBookingRequest();
        when(bookingService.createBooking(any(), any(BookingRequest.class)))
                .thenThrow(new BookingValidationException("Invalid meal plan: XX"));

        mockMvc.perform(post("/bookings")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid meal plan: XX"));
    }

    @Test
    void createBooking_returns409_whenServiceThrowsNoAvailableSpotsException() throws Exception {
        BookingRequest request = sampleBookingRequest();
        when(bookingService.createBooking(any(), any(BookingRequest.class)))
                .thenThrow(new NoAvailableSpotsException("No available spots for selected package"));

        mockMvc.perform(post("/bookings")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("No available spots for selected package"));
    }

    @Test
    void createBooking_returns400_whenBodyFailsBeanValidation() throws Exception {
        Guests guests = new Guests();
        guests.setAdults(1);
        guests.setChildren(0);
        BookingRequest invalid = new BookingRequest(
                "", null, null, null, guests, List.of());

        mockMvc.perform(post("/bookings")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelBooking_returnsOkWithCancellationResponse() throws Exception {
        BookingCancellationResponse response = new BookingCancellationResponse(
                "Booking canceled successfully", 0.0);
        when(bookingService.cancelBooking(any(), eq("booking-1"), any()))
                .thenReturn(response);

        mockMvc.perform(delete("/bookings/booking-1").with(authedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Booking canceled successfully"))
                .andExpect(jsonPath("$.chargedAmount").value(0.0));
    }

    @Test
    void cancelBooking_forwardsCancelRequestBodyToService() throws Exception {
        CancelBookingRequest cancelRequest = new CancelBookingRequest("agent unavailable");
        when(bookingService.cancelBooking(any(), eq("booking-1"), any()))
                .thenReturn(new BookingCancellationResponse("canceled", 10.0));

        mockMvc.perform(delete("/bookings/booking-1")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk());

        ArgumentCaptor<CancelBookingRequest> captor = ArgumentCaptor.forClass(CancelBookingRequest.class);
        verify(bookingService).cancelBooking(any(), eq("booking-1"), captor.capture());
        assertThat(captor.getValue().reason()).isEqualTo("agent unavailable");
    }

    @Test
    void cancelBooking_returns404_whenBookingNotFound() throws Exception {
        when(bookingService.cancelBooking(any(), eq("missing"), any()))
                .thenThrow(new BookingNotFoundException("booking not found"));

        mockMvc.perform(delete("/bookings/missing").with(authedUser()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("booking not found"));
    }

    @Test
    void cancelBooking_returns403_whenAccessDenied() throws Exception {
        when(bookingService.cancelBooking(any(), eq("booking-1"), any()))
                .thenThrow(new BookingAccessDeniedException("not allowed"));

        mockMvc.perform(delete("/bookings/booking-1").with(authedUser()))
                .andExpect(status().isForbidden())
                .andExpect(content().string("not allowed"));
    }

    @Test
    void editBooking_returns200WithDetailsResponse() throws Exception {
        BookingRequest request = sampleBookingRequest();
        BookingDetailsResponse response = new BookingDetailsResponse(
                "2027-02-19", "You have edited your booking");
        when(bookingService.editBooking(any(), eq("booking-1"), any(BookingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/bookings/booking-1")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freeCancellation").value("2027-02-19"))
                .andExpect(jsonPath("$.details").value("You have edited your booking"));
    }

    @Test
    void editBooking_returns400_whenBodyFailsBeanValidation() throws Exception {
        Guests guests = new Guests();
        guests.setAdults(1);
        guests.setChildren(0);
        BookingRequest invalid = new BookingRequest(
                "", null, null, null, guests, List.of());

        mockMvc.perform(put("/bookings/booking-1")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editBooking_returns404_whenBookingNotFound() throws Exception {
        BookingRequest request = sampleBookingRequest();
        when(bookingService.editBooking(any(), eq("missing"), any(BookingRequest.class)))
                .thenThrow(new BookingNotFoundException("booking not found"));

        mockMvc.perform(put("/bookings/missing")
                        .with(authedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("booking not found"));
    }

    @Test
    void confirmBooking_returns200_whenServiceSucceeds() throws Exception {
        mockMvc.perform(patch("/bookings/booking-1").with(authedUser()))
                .andExpect(status().isOk());

        verify(bookingService).confirmBooking(any(), eq("booking-1"));
    }

    @Test
    void confirmBooking_returns409_whenStateTransitionInvalid() throws Exception {
        org.mockito.Mockito.doThrow(new BookingStateTransitionException("already confirmed"))
                .when(bookingService).confirmBooking(any(), eq("booking-1"));

        mockMvc.perform(patch("/bookings/booking-1").with(authedUser()))
                .andExpect(status().isConflict())
                .andExpect(content().string("already confirmed"));
    }

    @Test
    void confirmBooking_returns404_whenBookingNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new BookingNotFoundException("booking not found"))
                .when(bookingService).confirmBooking(any(), eq("missing"));

        mockMvc.perform(patch("/bookings/missing").with(authedUser()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("booking not found"));
    }
}
