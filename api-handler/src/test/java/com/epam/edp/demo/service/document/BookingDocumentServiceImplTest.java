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
import com.epam.edp.demo.entity.document.FileFormat;
import com.epam.edp.demo.exception.document.DocumentValidationException;
import com.epam.edp.demo.repository.booking.BookingRepository;
import com.epam.edp.demo.repository.document.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BookingDocumentServiceImplTest {

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final S3PresignedUrlService presignedUrlService = mock(S3PresignedUrlService.class);
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final S3Client s3Client = mock(S3Client.class);

    private DocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocumentServiceImpl(
                documentRepository,
                presignedUrlService,
                bookingRepository,
                s3Client
        );
        ReflectionTestUtils.setField(service, "documentsBucketName", "test-bucket");
    }

    // ---- generateUploadUrl ----

    @Test
    void generateUploadUrl_passport_savesDocumentAndSetsPassportIdOnGuest() {
        GenerateUploadUrlRequestDTO request = new GenerateUploadUrlRequestDTO(
                FileFormat.png, "John", "Doe", DocumentType.PASSPORT, 5);
        UserContext userContext = new UserContext("user-1", "jane@example.com", Role.CUSTOMER);

        PersonalDetails person = new PersonalDetails("John", "Doe", null);
        Booking booking = Booking.builder()
                .bookingId("booking-1")
                .userId("user-1")
                .state(BookingStatus.BOOKED)
                .personalDetails(List.of(person))
                .build();

        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(presignedUrlService.generateUploadUrl(eq("test-bucket"), any())).thenReturn("https://upload.url");

        GenerateUploadUrlResponseDTO response = service.generateUploadUrl(userContext, "booking-1", request);

        assertThat(response.uploadUrl()).isEqualTo("https://upload.url");
        assertThat(response.documentId()).isNotNull();
        assertThat(person.getPassportDocumentId()).isEqualTo(response.documentId());

        verify(documentRepository).save(any(BookingDocument.class));
        verify(bookingRepository).save(booking);
    }

    @Test
    void generateUploadUrl_payment_setsPaymentDocumentIdOnBooking() {
        GenerateUploadUrlRequestDTO request = new GenerateUploadUrlRequestDTO(
                FileFormat.jpg, null, null, DocumentType.PAYMENT, 3);
        UserContext userContext = new UserContext("user-2", "john@example.com", Role.CUSTOMER);

        Booking booking = Booking.builder()
                .bookingId("booking-2")
                .userId("user-2")
                .state(BookingStatus.BOOKED)
                .personalDetails(List.of())
                .build();

        when(bookingRepository.findByBookingId("booking-2")).thenReturn(Optional.of(booking));
        when(presignedUrlService.generateUploadUrl(any(), any())).thenReturn("https://payment.url");

        GenerateUploadUrlResponseDTO response = service.generateUploadUrl(userContext, "booking-2", request);

        assertThat(booking.getPaymentDocumentId()).isEqualTo(response.documentId());
        verify(documentRepository).save(any(BookingDocument.class));
        verify(bookingRepository).save(booking);
    }

    @Test
    void generateUploadUrl_s3KeyIncludesBookingIdAndFormat() {
        GenerateUploadUrlRequestDTO request = new GenerateUploadUrlRequestDTO(
                FileFormat.png, null, null, DocumentType.PAYMENT, 2);
        UserContext userContext = new UserContext("user-abc", "abc@example.com", Role.CUSTOMER);

        Booking booking = Booking.builder()
                .bookingId("bk-abc")
                .userId("user-abc")
                .state(BookingStatus.BOOKED)
                .personalDetails(List.of())
                .build();

        when(bookingRepository.findByBookingId("bk-abc")).thenReturn(Optional.of(booking));
        when(presignedUrlService.generateUploadUrl(any(), any())).thenReturn("https://url");

        service.generateUploadUrl(userContext, "bk-abc", request);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(presignedUrlService).generateUploadUrl(eq("test-bucket"), keyCaptor.capture());
        assertThat(keyCaptor.getValue()).startsWith("bookings/bk-abc/").endsWith(".png");
    }

    @Test
    void generateUploadUrl_bookingNotFound_throws() {
        GenerateUploadUrlRequestDTO request = new GenerateUploadUrlRequestDTO(
                FileFormat.pdf, "Jane", "Smith", DocumentType.PASSPORT, 5);
        UserContext userContext = new UserContext("user-missing", "missing@example.com", Role.CUSTOMER);

        when(bookingRepository.findByBookingId("missing")).thenReturn(Optional.empty());
        when(presignedUrlService.generateUploadUrl(any(), any())).thenReturn("https://url");

        assertThatThrownBy(() -> service.generateUploadUrl(userContext, "missing", request))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void generateUploadUrl_guestNotFoundInBooking_throws() {
        GenerateUploadUrlRequestDTO request = new GenerateUploadUrlRequestDTO(
                FileFormat.pdf, "Unknown", "Guest", DocumentType.PASSPORT, 5);
        UserContext userContext = new UserContext("user-1", "jane@example.com", Role.CUSTOMER);

        PersonalDetails person = new PersonalDetails("John", "Doe", null);
        Booking booking = Booking.builder()
                .bookingId("booking-1")
                .userId("user-1")
                .state(BookingStatus.BOOKED)
                .personalDetails(List.of(person))
                .build();

        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));
        when(presignedUrlService.generateUploadUrl(any(), any())).thenReturn("https://url");

        assertThatThrownBy(() -> service.generateUploadUrl(userContext, "booking-1", request))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("not found in booking");
    }

    @Test
    void generateUploadUrl_callsValidationService() {
        GenerateUploadUrlRequestDTO request = new GenerateUploadUrlRequestDTO(
                FileFormat.pdf, null, null, DocumentType.PAYMENT, 5);
        UserContext userContext = new UserContext("user-b1", "b1@example.com", Role.CUSTOMER);

        Booking booking = Booking.builder()
                .bookingId("b1")
                .userId("user-b1")
                .state(BookingStatus.BOOKED)
                .personalDetails(List.of())
                .build();

        when(bookingRepository.findByBookingId("b1")).thenReturn(Optional.of(booking));
        when(presignedUrlService.generateUploadUrl(any(), any())).thenReturn("https://url");

        service.generateUploadUrl(userContext, "b1", request);
    }

    // ---- verifyDocumentExistsInS3ById ----

    @Test
    void verifyDocumentExistsInS3ById_documentNotFound_throws() {
        when(documentRepository.findById("doc-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyDocumentExistsInS3ById("doc-1"))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void verifyDocumentExistsInS3ById_alreadyUploaded_returnsEmpty() {
        BookingDocument bookingDocument = BookingDocument.builder()
                .documentId("doc-1")
                .s3Key("bookings/b1/doc-1.pdf")
                .status(DocumentStatus.UPLOADED)
                .build();

        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(bookingDocument));

        Optional<String> result = service.verifyDocumentExistsInS3ById("doc-1");

        assertThat(result).isEmpty();
    }

    @Test
    void verifyDocumentExistsInS3ById_pendingAndFileInS3_returnsEmpty() {
        BookingDocument bookingDocument = BookingDocument.builder()
                .documentId("doc-2")
                .s3Key("bookings/b1/doc-2.pdf")
                .fileName("PASSPORT John Doe.pdf")
                .status(DocumentStatus.PENDING_UPLOAD)
                .build();

        when(documentRepository.findById("doc-2")).thenReturn(Optional.of(bookingDocument));
        // headObject does not throw → file exists

        Optional<String> result = service.verifyDocumentExistsInS3ById("doc-2");

        assertThat(result).isEmpty();
        assertThat(bookingDocument.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
    }

    @Test
    void verifyDocumentExistsInS3ById_pendingAndFileNotInS3_returnsFileName() {
        BookingDocument bookingDocument = BookingDocument.builder()
                .documentId("doc-3")
                .s3Key("bookings/b1/doc-3.pdf")
                .fileName("PASSPORT Jane Smith.pdf")
                .status(DocumentStatus.PENDING_UPLOAD)
                .build();

        when(documentRepository.findById("doc-3")).thenReturn(Optional.of(bookingDocument));
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Not found").statusCode(404).build());

        Optional<String> result = service.verifyDocumentExistsInS3ById("doc-3");

        assertThat(result).contains("PASSPORT Jane Smith.pdf");
        assertThat(bookingDocument.getStatus()).isEqualTo(DocumentStatus.FAILED);
    }

    // ---- verifyDocumentExistsInS3 ----

    @Test
    void verifyDocumentExistsInS3_uploaded_returnsTrueWithoutS3Call() {
        BookingDocument bookingDocument = BookingDocument.builder()
                .status(DocumentStatus.UPLOADED)
                .build();

        boolean result = service.verifyDocumentExistsInS3(bookingDocument);

        assertThat(result).isTrue();
        verifyNoInteractions(s3Client);
    }

    @Test
    void verifyDocumentExistsInS3_failed_returnsFalseWithoutS3Call() {
        BookingDocument bookingDocument = BookingDocument.builder()
                .status(DocumentStatus.FAILED)
                .build();

        boolean result = service.verifyDocumentExistsInS3(bookingDocument);

        assertThat(result).isFalse();
        verifyNoInteractions(s3Client);
    }

    @Test
    void verifyDocumentExistsInS3_pendingAndFileExists_returnsTrueAndSetsUploaded() {
        BookingDocument bookingDocument = BookingDocument.builder()
                .documentId("doc-4")
                .s3Key("key/doc-4.pdf")
                .status(DocumentStatus.PENDING_UPLOAD)
                .build();
        // headObject does not throw → file exists

        boolean result = service.verifyDocumentExistsInS3(bookingDocument);

        assertThat(result).isTrue();
        assertThat(bookingDocument.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        verify(documentRepository).save(bookingDocument);
    }

    @Test
    void verifyDocumentExistsInS3_pendingAndFileNotInS3_returnsFalseAndSetsFailed() {
        BookingDocument bookingDocument = BookingDocument.builder()
                .documentId("doc-5")
                .s3Key("key/doc-5.pdf")
                .status(DocumentStatus.PENDING_UPLOAD)
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Not found").statusCode(404).build());

        boolean result = service.verifyDocumentExistsInS3(bookingDocument);

        assertThat(result).isFalse();
        assertThat(bookingDocument.getStatus()).isEqualTo(DocumentStatus.FAILED);
        verify(documentRepository).save(bookingDocument);
    }

    // ---- generateRetrieveUrls ----

    @Test
    void generateRetrieveUrls_noDocumentIds_returnsEmptyMap() {
        Booking booking = Booking.builder()
                .bookingId("b1")
                .personalDetails(List.of())
                .paymentDocumentId(null)
                .build();

        when(documentRepository.findByIds(List.of())).thenReturn(List.of());

        Map<String, String> result = service.generateRetrieveUrls(booking);

        assertThat(result).isEmpty();
        verify(documentRepository).findByIds(List.of());
    }

    @Test
    void generateRetrieveUrls_uploadedDocuments_returnsUrlMap() {
        PersonalDetails person = new PersonalDetails("John", "Doe", "doc-passport");
        Booking booking = Booking.builder()
                .bookingId("b2")
                .personalDetails(List.of(person))
                .paymentDocumentId("doc-payment")
                .build();

        BookingDocument passportDoc = BookingDocument.builder()
                .documentId("doc-passport")
                .s3Key("key/passport.pdf")
                .status(DocumentStatus.UPLOADED)
                .build();

        BookingDocument paymentDoc = BookingDocument.builder()
                .documentId("doc-payment")
                .s3Key("key/payment.pdf")
                .status(DocumentStatus.UPLOADED)
                .build();

        when(documentRepository.findByIds(any())).thenReturn(List.of(passportDoc, paymentDoc));
        when(presignedUrlService.generateRetrieveUrl("test-bucket", "key/passport.pdf"))
                .thenReturn("https://passport.url");
        when(presignedUrlService.generateRetrieveUrl("test-bucket", "key/payment.pdf"))
                .thenReturn("https://payment.url");

        Map<String, String> result = service.generateRetrieveUrls(booking);

        assertThat(result)
                .containsEntry("doc-passport", "https://passport.url")
                .containsEntry("doc-payment", "https://payment.url");
    }

    @Test
    void generateRetrieveUrls_documentRepositoryThrows_returnsEmptyMap() {
        PersonalDetails person = new PersonalDetails("John", "Doe", "doc-1");
        Booking booking = Booking.builder()
                .bookingId("b3")
                .personalDetails(List.of(person))
                .build();

        when(documentRepository.findByIds(any())).thenThrow(new RuntimeException("DynamoDB error"));

        Map<String, String> result = service.generateRetrieveUrls(booking);

        assertThat(result).isEmpty();
    }

    @Test
    void generateRetrieveUrls_pendingDocumentNotInS3_excludedFromResult() {
        PersonalDetails person = new PersonalDetails("Alice", "Smith", "doc-pending");
        Booking booking = Booking.builder()
                .bookingId("b4")
                .personalDetails(List.of(person))
                .build();

        BookingDocument pendingDoc = BookingDocument.builder()
                .documentId("doc-pending")
                .s3Key("key/pending.pdf")
                .status(DocumentStatus.PENDING_UPLOAD)
                .build();

        when(documentRepository.findByIds(any())).thenReturn(List.of(pendingDoc));
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Not found").statusCode(404).build());

        Map<String, String> result = service.generateRetrieveUrls(booking);

        assertThat(result).isEmpty();
        verifyNoInteractions(presignedUrlService);
    }

    @Test
    void generateRetrieveUrls_collectsPassportIdsFromAllPersonalDetails() {
        PersonalDetails person1 = new PersonalDetails("Alice", "Smith", "doc-alice");
        PersonalDetails person2 = new PersonalDetails("Bob", "Jones", "doc-bob");
        Booking booking = Booking.builder()
                .bookingId("b5")
                .personalDetails(List.of(person1, person2))
                .build();

        when(documentRepository.findByIds(any())).thenReturn(List.of());

        service.generateRetrieveUrls(booking);

        ArgumentCaptor<List<String>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentRepository).findByIds(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder("doc-alice", "doc-bob");
    }

    @Test
    void generateUploadUrl_nonCustomerRole_throwsBookingAccessDenied() {
        GenerateUploadUrlRequestDTO request = new GenerateUploadUrlRequestDTO(
                FileFormat.pdf, null, null, DocumentType.PAYMENT, 5);
        UserContext userContext = new UserContext("agent-1", "agent@example.com", Role.TRAVEL_AGENT);

        Booking booking = Booking.builder()
                .bookingId("booking-1")
                .userId("agent-1")
                .state(BookingStatus.BOOKED)
                .personalDetails(List.of())
                .build();
        when(bookingRepository.findByBookingId("booking-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.generateUploadUrl(userContext, "booking-1", request))
                .isInstanceOf(com.epam.edp.demo.exception.booking.BookingAccessDeniedException.class)
                .hasMessageContaining("Only customers can upload documents");

        verifyNoInteractions(documentRepository, presignedUrlService);
    }

    @Test
    void generateUploadUrl_differentUserId_throwsBookingAccessDenied() {
        GenerateUploadUrlRequestDTO request = new GenerateUploadUrlRequestDTO(
                FileFormat.pdf, null, null, DocumentType.PAYMENT, 5);
        UserContext userContext = new UserContext("user-other", "other@example.com", Role.CUSTOMER);

        Booking booking = Booking.builder()
                .bookingId("booking-x")
                .userId("user-owner")
                .state(BookingStatus.BOOKED)
                .personalDetails(List.of())
                .build();

        when(bookingRepository.findByBookingId("booking-x")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.generateUploadUrl(userContext, "booking-x", request))
                .isInstanceOf(com.epam.edp.demo.exception.booking.BookingAccessDeniedException.class)
                .hasMessageContaining("cannot upload documents for other customers booking");

        verify(documentRepository, never()).save(any());
    }

    @Test
    void generateUploadUrl_bookingNotInBookedState_throwsBookingStateTransition() {
        GenerateUploadUrlRequestDTO request = new GenerateUploadUrlRequestDTO(
                FileFormat.pdf, null, null, DocumentType.PAYMENT, 5);
        UserContext userContext = new UserContext("user-1", "u@example.com", Role.CUSTOMER);

        Booking booking = Booking.builder()
                .bookingId("booking-c")
                .userId("user-1")
                .state(BookingStatus.CONFIRMED)
                .personalDetails(List.of())
                .build();

        when(bookingRepository.findByBookingId("booking-c")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.generateUploadUrl(userContext, "booking-c", request))
                .isInstanceOf(com.epam.edp.demo.exception.booking.BookingStateTransitionException.class)
                .hasMessageContaining("not in BOOKED state");

        verify(documentRepository, never()).save(any());
    }
}
