package com.epam.edp.demo.repository.booking;

import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.auth.Role;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
public class BookingRepositoryImpl implements BookingRepository {

    private final MongoTemplate mongoTemplate;

    public BookingRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(Booking booking) {
        log.info("Saving booking with id {}", booking.getBookingId());
        mongoTemplate.save(booking);
        log.info("Booking saved with id: {}", booking.getBookingId());
    }

    @Override
    public List<Booking> findAll() {
        log.info("Fetching all bookings");
        List<Booking> bookings = mongoTemplate.findAll(Booking.class);
        log.info("Fetched {} bookings", bookings.size());
        return bookings;
    }

    @Override
    public boolean anyExists() {
        return mongoTemplate.exists(new Query(), Booking.class);
    }

    @Override
    public List<Booking> getBookingsByCustomerId(String customerId) {
        log.info("Getting bookings for customer with id: {}", customerId);

        Query query = Query.query(Criteria.where("userId").is(customerId));
        List<Booking> bookings = mongoTemplate.find(query, Booking.class);

        log.info("Fetched {} Bookings for customer with id: {}", bookings.size(), customerId);
        return bookings;
    }

    @Override
    public List<Booking> getBookingsByCustomerIdFilterStatus(String customerId, BookingStatus bookingStatus) {
        log.info("Getting {} bookings for customer with id: {}", bookingStatus, customerId);

        Query query = Query.query(Criteria.where("userId").is(customerId).and("state").is(bookingStatus));
        List<Booking> bookings = mongoTemplate.find(query, Booking.class);

        log.info("Fetched {} (filtered) Bookings for customer with id: {}", bookings.size(), customerId);
        return bookings;
    }


    @Override
    public List<Booking> getBookingsByTravelAgentEmail(String travelAgentEmail) {
        log.info("Getting bookings for travel agent with email: {}", travelAgentEmail);

        Query query = Query.query(Criteria.where("travelAgentEmail").is(travelAgentEmail));
        List<Booking> bookings = mongoTemplate.find(query, Booking.class);

        log.info("Fetched {} Bookings for travel agent with email: {}", bookings.size(), travelAgentEmail);
        return bookings;
    }

    @Override
    public List<Booking> getBookingsByTravelAgentEmailFilterStatus(String travelAgentEmail, BookingStatus bookingStatus) {
        log.info("Getting {} Bookings for travel agent with email: {}", bookingStatus, travelAgentEmail);

        Query query = Query.query(Criteria.where("travelAgentEmail").is(travelAgentEmail)
                .and("state").is(bookingStatus)
        );
        List<Booking> bookings = mongoTemplate.find(query, Booking.class);

        log.info("Fetched {} (filtered) Bookings for travel agent with email: {}", bookings.size(), travelAgentEmail);        return bookings;
    }

    @Override
    public Optional<Booking> findByBookingId(String bookingId) {
        log.info("Fetching booking by id {}", bookingId);

        Booking booking = mongoTemplate.findById(bookingId, Booking.class);

        log.info("Fetched booking by id {}", bookingId);
        return Optional.ofNullable(booking);
    }

    @Override
    public void cancelBooking(Booking booking) {
        log.info("Customer canceling booking {}", booking.getBookingId());
        booking.setState(BookingStatus.CANCELED);
        booking.setCanceledBy(Role.CUSTOMER.name());
        mongoTemplate.save(booking);
    }

    @Override
    public void cancelBookingByTravelAgent(Booking booking, String reason) {
        log.info("Travel Agent canceling booking {}", booking.getBookingId());
        booking.setState(BookingStatus.CANCELED);
        booking.setCanceledBy(booking.getTravelAgentEmail());
        booking.setCancelReason(reason);
        mongoTemplate.save(booking);
    }

    @Override
    public void saveBookings(List<Booking> bookings) {
        log.info("Saving bookings in batch {}", bookings);
        bookings.forEach(mongoTemplate::save);
        log.info("Saved bookings");
    }

    public boolean transitionBookingState(String bookingId, BookingStatus expectedState, BookingStatus newState) {

        Query query = Query.query(Criteria.where("_id").is(bookingId).and("state").is(expectedState));
        Update update = new Update().set("state", newState);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Booking.class);

        if (result.getModifiedCount() == 1) {
            return true;
        }

        log.info("Conditional state transition skipped for booking {}: expected {} -> {}",
                bookingId, expectedState, newState);

        return false;
    }
}
