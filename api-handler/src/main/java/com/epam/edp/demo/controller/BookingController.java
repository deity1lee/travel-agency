package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.bookings.*;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.service.booking.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/bookings")
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @Operation(
            summary = "Get Bookings for user",
            description = "Get all Bookings identified by user"
    )
    @ApiResponse(responseCode = "200", description = "Bookings returned successfully")
    @ApiResponse(responseCode = "401", description = "User verification failed")
    @ApiResponse(responseCode = "403", description = "Caller doesn't have permission to get bookings")
    @ApiResponse(responseCode = "404", description = "Travel Agent not found")
    public BookingListResponse getBookings(@CurrentUser UserContext user,
                                           @RequestParam(required = false) BookingStatus status) {
        log.info("Incoming request: GET /bookings, user={}", user.userId());
        BookingListResponse resp = bookingService.getBookings(user, status);
        log.info("Response: 200 OK – returning user {} bookings {}", user.userId(), resp.bookings());
        return resp;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Booking creation",
            description = "Creates a new Booking"
    )
    @ApiResponse(responseCode = "201", description = "Booking created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Tour not found | User not found")
    @ApiResponse(responseCode = "409", description = "No Travel Agent is available")
    public BookingDetailsResponse createBooking(@CurrentUser UserContext user,
                                                @Valid @RequestBody BookingRequest request) {
        log.info("Incoming request: POST /bookings, user={}", user.userId());
        BookingDetailsResponse resp = bookingService.createBooking(user, request);
        log.info("Response: 201 CREATED – booking={}", resp);
        return resp;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Delete Booking",
            description = "Deletes the Booking identified by booking id"
    )
    @ApiResponse(responseCode = "200", description = "Booking was deleted successfully")
    @ApiResponse(responseCode = "401", description = "User verification failed")
    @ApiResponse(responseCode = "403", description = "Caller doesn't have permission to cancel this booking")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @ApiResponse(responseCode = "409", description = "Only Booking is state BOOKED or CONFIRMED can be confirmed")
    public BookingCancellationResponse cancelBooking(@CurrentUser UserContext user,
                                                     @PathVariable String id,
                                                     @Valid @RequestBody(required = false) CancelBookingRequest cancelRequest) {
        log.info("Incoming request: DELETE /bookings/{}", id);
        BookingCancellationResponse resp = bookingService.cancelBooking(user, id, cancelRequest);
        log.info("Response: 200 OK – booking={} profile deleted", id);
        return resp;
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update User Booking",
            description = "Updates the Booking of the User identified by booking id"
    )
    @ApiResponse(responseCode = "200", description = "Booking was updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "User verification failed")
    @ApiResponse(responseCode = "403", description = "Caller doesn't have permission to edit this booking")
    @ApiResponse(responseCode = "404", description = "Booking not found | Tour not found")
    public BookingDetailsResponse editBooking(@CurrentUser UserContext user,
                                              @PathVariable String id,
                                              @Valid @RequestBody BookingRequest request) {
        log.info("Incoming request: PUT /bookings/{}", id);
        BookingDetailsResponse tlr = bookingService.editBooking(user, id, request);
        log.info("Response: 200 OK – booking {} updated", id);
        return tlr;
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Confirm booking",
            description = "Sets status of Booking to Confirmed"
    )
    @ApiResponse(responseCode = "200", description = "Booking was confirmed set successfully")
    @ApiResponse(responseCode = "401", description = "User verification failed")
    @ApiResponse(responseCode = "403", description = "Caller doesn't have enough permissions")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @ApiResponse(responseCode = "409", description = "Only Booking is state BOOKED can be confirmed")
    public void confirmBooking(@CurrentUser UserContext user,
                               @PathVariable String id) {
        log.info("Incoming request: PATCH /bookings/{} ", id);
        bookingService.confirmBooking(user, id);
        log.info("Response: 200 OK – booking={} status set to Confirmed", id);
    }

}
