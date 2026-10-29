package com.epam.edp.demo.service.document;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.document.GenerateUploadUrlRequestDTO;
import com.epam.edp.demo.dto.document.GenerateUploadUrlResponseDTO;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.document.BookingDocument;

import java.util.Map;
import java.util.Optional;

public interface DocumentService {

    GenerateUploadUrlResponseDTO generateUploadUrl(
            UserContext userContext,
            String bookingId,
            GenerateUploadUrlRequestDTO request);

    Optional<String> verifyDocumentExistsInS3ById(String documentId);

    boolean verifyDocumentExistsInS3(BookingDocument bookingDocument);

    Map<String, String> generateRetrieveUrls(Booking booking);
}
