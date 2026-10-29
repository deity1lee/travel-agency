package com.epam.edp.demo.repository.booking;

import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private BookingRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new BookingRepositoryImpl(mongoTemplate);
    }

    private static Booking booking(String id, BookingStatus state) {
        return Booking.builder()
                .bookingId(id)
                .userId("user-1")
                .travelAgentEmail("agent@example.com")
                .state(state)
                .build();
    }

    @Test
    void save_savesBookingToMongo() {
        Booking input = booking("booking-1", BookingStatus.BOOKED);

        repository.save(input);

        verify(mongoTemplate).save(input);
    }

    @Test
    void findByBookingId_returnsBooking_whenMongoReturnsItem() {
        Booking found = booking("booking-1", BookingStatus.BOOKED);
        when(mongoTemplate.findById("booking-1", Booking.class)).thenReturn(found);

        Optional<Booking> result = repository.findByBookingId("booking-1");

        assertThat(result).contains(found);
        verify(mongoTemplate).findById("booking-1", Booking.class);
    }

    @Test
    void findByBookingId_returnsEmpty_whenMongoReturnsNull() {
        when(mongoTemplate.findById("missing", Booking.class)).thenReturn(null);

        assertThat(repository.findByBookingId("missing")).isEmpty();
    }

    @Test
    void getBookingsByCustomerId_filtersByUserId() {
        Booking a = booking("b-1", BookingStatus.BOOKED);
        Booking b = booking("b-2", BookingStatus.CONFIRMED);
        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(List.of(a, b));

        List<Booking> result = repository.getBookingsByCustomerId("user-1");

        assertThat(result).containsExactly(a, b);
        Query query = captureFindQuery();
        assertThat(query.getQueryObject().get("userId")).isEqualTo("user-1");
    }

    @Test
    void getBookingsByCustomerIdFilterStatus_filtersByUserIdAndStatus() {
        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(List.of());

        repository.getBookingsByCustomerIdFilterStatus("user-1", BookingStatus.CONFIRMED);

        Document query = captureFindQuery().getQueryObject();
        assertThat(query.get("userId")).isEqualTo("user-1");
        assertThat(query.get("state")).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void getBookingsByTravelAgentEmail_filtersByTravelAgentEmail() {
        Booking a = booking("b-1", BookingStatus.BOOKED);
        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(List.of(a));

        List<Booking> result = repository.getBookingsByTravelAgentEmail("agent@example.com");

        assertThat(result).containsExactly(a);
        assertThat(captureFindQuery().getQueryObject().get("travelAgentEmail")).isEqualTo("agent@example.com");
    }

    @Test
    void getBookingsByTravelAgentEmailFilterStatus_filtersByEmailAndStatus() {
        when(mongoTemplate.find(any(Query.class), eq(Booking.class))).thenReturn(List.of());

        repository.getBookingsByTravelAgentEmailFilterStatus("agent@example.com", BookingStatus.BOOKED);

        Document query = captureFindQuery().getQueryObject();
        assertThat(query.get("travelAgentEmail")).isEqualTo("agent@example.com");
        assertThat(query.get("state")).isEqualTo(BookingStatus.BOOKED);
    }

    @Test
    void cancelBooking_setsStatusAndCanceledByAndSaves() {
        Booking input = booking("booking-1", BookingStatus.BOOKED);

        repository.cancelBooking(input);

        assertThat(input.getState()).isEqualTo(BookingStatus.CANCELED);
        assertThat(input.getCanceledBy()).isEqualTo(Role.CUSTOMER.name());
        verify(mongoTemplate).save(input);
    }

    @Test
    void cancelBookingByTravelAgent_setsStateReasonAndCanceledByAgent_andSaves() {
        Booking input = booking("booking-1", BookingStatus.BOOKED);

        repository.cancelBookingByTravelAgent(input, "agent unavailable");

        assertThat(input.getState()).isEqualTo(BookingStatus.CANCELED);
        assertThat(input.getCanceledBy()).isEqualTo("agent@example.com");
        assertThat(input.getCancelReason()).isEqualTo("agent unavailable");
        verify(mongoTemplate).save(input);
    }

    @Test
    void saveBookings_savesEachBooking() {
        Booking first = booking("booking-1", BookingStatus.BOOKED);
        Booking second = booking("booking-2", BookingStatus.CONFIRMED);
        List<Booking> bookings = List.of(first, second);

        repository.saveBookings(bookings);

        verify(mongoTemplate).save(first);
        verify(mongoTemplate).save(second);
    }

    @Test
    void findAll_returnsAllBookings() {
        Booking a = booking("b-1", BookingStatus.BOOKED);
        Booking b = booking("b-2", BookingStatus.CONFIRMED);
        when(mongoTemplate.findAll(Booking.class)).thenReturn(List.of(a, b));

        List<Booking> result = repository.findAll();

        assertThat(result).containsExactly(a, b);
        verify(mongoTemplate).findAll(Booking.class);
    }

    @Test
    void anyExists_returnsTrue_whenMongoHasABooking() {
        when(mongoTemplate.exists(any(Query.class), eq(Booking.class))).thenReturn(true);

        assertThat(repository.anyExists()).isTrue();
    }

    @Test
    void anyExists_returnsFalse_whenNoBookingsPresent() {
        when(mongoTemplate.exists(any(Query.class), eq(Booking.class))).thenReturn(false);

        assertThat(repository.anyExists()).isFalse();
    }

    @Test
    void transitionBookingState_returnsTrue_andBuildsMongoQueryAndUpdate_onSuccess() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Booking.class)))
                .thenReturn(acknowledged(1, 1));

        boolean result = repository.transitionBookingState(
                "booking-1", BookingStatus.BOOKED, BookingStatus.CONFIRMED);

        assertThat(result).isTrue();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(Booking.class));

        Document query = queryCaptor.getValue().getQueryObject();
        assertThat(query.get("_id")).isEqualTo("booking-1");
        assertThat(query.get("state")).isEqualTo(BookingStatus.BOOKED);

        Document set = (Document) updateCaptor.getValue().getUpdateObject().get("$set");
        assertThat(set.get("state")).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void transitionBookingState_returnsFalse_whenNoDocumentWasUpdated() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Booking.class)))
                .thenReturn(acknowledged(0, 0));

        boolean result = repository.transitionBookingState(
                "booking-1", BookingStatus.BOOKED, BookingStatus.CONFIRMED);

        assertThat(result).isFalse();
    }

    private Query captureFindQuery() {
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Booking.class));
        return captor.getValue();
    }

    private static UpdateResult acknowledged(long matchedCount, long modifiedCount) {
        return UpdateResult.acknowledged(matchedCount, modifiedCount, null);
    }
}
