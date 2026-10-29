package com.epam.edp.demo.repository.document;

import com.epam.edp.demo.entity.document.BookingDocument;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    void save(BookingDocument bookingDocument);

    Optional<BookingDocument> findById(String documentId);

    List<BookingDocument> findByIds(List<String> documentIds);

    void delete(BookingDocument bookingDocument);
}
