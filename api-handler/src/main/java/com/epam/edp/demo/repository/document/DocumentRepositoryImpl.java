package com.epam.edp.demo.repository.document;

import com.epam.edp.demo.entity.document.BookingDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class DocumentRepositoryImpl implements DocumentRepository {

    private final MongoTemplate mongoTemplate;

    public DocumentRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(BookingDocument bookingDocument) {
        log.info("Saving document {}", bookingDocument.getDocumentId());
        mongoTemplate.save(bookingDocument);
        log.info("Saved document {}", bookingDocument.getDocumentId());
    }

    @Override
    public Optional<BookingDocument> findById(String documentId) {
        log.info("Finding document by id {}", documentId);
        BookingDocument bookingDocument = mongoTemplate.findById(documentId, BookingDocument.class);
        return Optional.ofNullable(bookingDocument);
    }

    @Override
    public List<BookingDocument> findByIds(List<String> documentIds) {
        log.info("Finding {} documents by ids {}", documentIds.size(), documentIds);
        if (documentIds.isEmpty()) {
            return List.of();
        }
        Query query = Query.query(Criteria.where("_id").in(documentIds));
        List<BookingDocument> bookingDocuments = mongoTemplate.find(query, BookingDocument.class);

        log.info("Returning {} documents", bookingDocuments.size());
        return bookingDocuments;
    }

    @Override
    public void delete(BookingDocument bookingDocument) {
        log.info("Deleting document {}", bookingDocument);

        mongoTemplate.remove(bookingDocument);

        log.info("Deleted document with id {}", bookingDocument.getDocumentId());
    }
}
