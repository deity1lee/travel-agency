package com.epam.edp.demo.service.document;

import com.epam.edp.demo.controller.UserContext;
import com.epam.edp.demo.dto.document.GenerateUploadUrlRequestDTO;
import com.epam.edp.demo.dto.document.GenerateUploadUrlResponseDTO;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.booking.Booking;
import com.epam.edp.demo.entity.booking.BookingStatus;
import com.epam.edp.demo.entity.booking.PersonalDetails;
import com.epam.edp.demo.entity.document.BookingDocument;
import com.epam.edp.demo.entity.document.DocumentStatus;
import com.epam.edp.demo.entity.document.DocumentType;
import com.epam.edp.demo.exception.booking.BookingAccessDeniedException;
import com.epam.edp.demo.exception.booking.BookingStateTransitionException;
import com.epam.edp.demo.exception.document.DocumentValidationException;
import com.epam.edp.demo.repository.booking.BookingRepository;
import com.epam.edp.demo.repository.document.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final S3PresignedUrlService presignedUrlService;
    private final BookingRepository bookingRepository;
    private final S3Client s3Client;

    @Value("${documents.bucket-name}")
    private String documentsBucketName;

    @Override
    public GenerateUploadUrlResponseDTO generateUploadUrl(
            UserContext userContext,
            String bookingId,
            GenerateUploadUrlRequestDTO request) {

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new DocumentValidationException("Booking not found"));

        String userId = userContext.userId();
        if (userContext.role() != Role.CUSTOMER) {
            throw new BookingAccessDeniedException("Only customers can upload documents");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new BookingAccessDeniedException("Customer cannot upload documents for other customers booking");
        }

        if (booking.getState() != BookingStatus.BOOKED) {
            throw new BookingStateTransitionException("Booking is not in BOOKED state, cannot upload documents");
        }

        log.info("Generating upload url for booking {}, request: {}", bookingId, request);

        String documentId = UUID.randomUUID().toString();

        String s3Key = String.format(
                "bookings/%s/%s.%s",
                bookingId,
                documentId,
                request.fileFormat()
        );

        log.info("s3Key: {}", s3Key);

        DocumentType documentType = request.documentType();

        String fileName = documentType == DocumentType.PASSPORT
                ? "%s %s %s.%s".formatted(
                documentType,
                request.guestFirstName(),
                request.guestLastName(),
                request.fileFormat())
                : "%s receipt.%s".formatted(
                documentType,
                request.fileFormat());

        BookingDocument bookingDocument = BookingDocument.builder()
                .documentId(documentId)
                .bookingId(bookingId)
                .documentType(documentType)
                .fileName(fileName)
                .s3Key(s3Key)
                .status(DocumentStatus.PENDING_UPLOAD)
                .build();

        String uploadUrl = presignedUrlService.generateUploadUrl(
                documentsBucketName,
                s3Key
        );

        log.info("upload url: {}", uploadUrl);

        if (documentType == DocumentType.PASSPORT) {
            booking.getPersonalDetails().stream()
                    .filter(person -> request.guestFirstName().equals(person.getFirstName()))
                    .filter(person -> request.guestLastName().equals(person.getLastName()))
                    .findFirst()
                    .orElseThrow(() -> new DocumentValidationException(
                            "Guest %s %s not found in booking %s".formatted(
                                    request.guestFirstName(),
                                    request.guestLastName(),
                                    bookingId)))
                    .setPassportDocumentId(documentId);
        } else {
            booking.setPaymentDocumentId(documentId);
        }

        documentRepository.save(bookingDocument);
        bookingRepository.save(booking);

        return GenerateUploadUrlResponseDTO.builder()
                .documentId(documentId)
                .uploadUrl(uploadUrl)
                .build();
    }

    @Override
    public Optional<String> verifyDocumentExistsInS3ById(String documentId) {
        BookingDocument bookingDocument = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentValidationException("Document not found"));

        if (verifyDocumentExistsInS3(bookingDocument)) {
            return Optional.empty();
        }

        return Optional.of(bookingDocument.getFileName());
    }

    @Override
    public boolean verifyDocumentExistsInS3(BookingDocument bookingDocument) {
        return switch (bookingDocument.getStatus()) {
            case UPLOADED -> true;

            case PENDING_UPLOAD -> {
                boolean uploaded = false;

                try {
                    verifyFileExists(bookingDocument.getS3Key());
                    bookingDocument.setStatus(DocumentStatus.UPLOADED);
                    uploaded = true;
                } catch (DocumentValidationException e) {
                    log.warn("Document {} verification failed, marking as FAILED", bookingDocument.getS3Key(), e);
                    bookingDocument.setStatus(DocumentStatus.FAILED);
                }

                documentRepository.save(bookingDocument);

                yield uploaded;
            }

            default -> false;
        };
    }

    private void verifyFileExists(String s3Key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(documentsBucketName)
                    .key(s3Key)
                    .build();

            s3Client.headObject(request);
        } catch (S3Exception e) {
            log.info("Document {} not uploaded to S3 yet: ", s3Key, e);
            throw new DocumentValidationException("File not uploaded to S3 yet");
        }
    }

    @Override
    public Map<String, String> generateRetrieveUrls(Booking booking) {
        log.info("Generating retrieval urls for booking {}", booking.getBookingId());
        List<String> documentIds = booking.getPersonalDetails().stream()
                .map(PersonalDetails::getPassportDocumentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (booking.getPaymentDocumentId() != null) {
            documentIds.add(booking.getPaymentDocumentId());
        }

        List<BookingDocument> bookingDocuments = List.of();
        try {
            bookingDocuments = documentRepository.findByIds(documentIds);
        } catch (Exception e) {
            log.warn("exception while retrieving documents", e);
        }
        log.info("Retrieved {} documents for booking {}", bookingDocuments.size(), booking.getBookingId());

        Map<String, String> documentUrls = new HashMap<>();
        for (BookingDocument doc : bookingDocuments) {
            if (verifyDocumentExistsInS3(doc)) {
                String url = presignedUrlService.generateRetrieveUrl(
                        documentsBucketName,
                        doc.getS3Key()
                );
                documentUrls.put(doc.getDocumentId(), url);
            }
        }

        return documentUrls;
    }
}