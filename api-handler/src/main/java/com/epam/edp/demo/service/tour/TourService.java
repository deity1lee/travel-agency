package com.epam.edp.demo.service.tour;

import com.epam.edp.demo.dto.tour.DestinationListResponseDTO;
import com.epam.edp.demo.dto.tour.TourDetailsDTO;
import com.epam.edp.demo.dto.tour.TourListResponseDTO;
import com.epam.edp.demo.dto.tour.TourRequestDTO;
import com.epam.edp.demo.entity.booking.Booking;

public interface TourService {

    TourListResponseDTO getAvailableTours(TourRequestDTO tourRequestDto);

    DestinationListResponseDTO getDestinations(String prefix);

    TourDetailsDTO getTourById(String id);

    void increaseTourOption(Booking booking);
}
