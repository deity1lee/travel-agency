package com.epam.edp.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ReportStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3ReportStorageService s3ReportStorageService;

    @Test
    void test_uploadCsv_putsObjectToConfiguredBucket_whenInputIsValid() {
        ReflectionTestUtils.setField(s3ReportStorageService, "bucketName", "reports-bucket");
        byte[] csv = "h1,h2\nv1,v2".getBytes();
        LocalDate date = LocalDate.of(2026, 7, 4);

        s3ReportStorageService.uploadCsv(csv, "tour-perf", date);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("reports-bucket", request.bucket());
        assertEquals("text/csv", request.contentType());
        assertNotNull(request.key());
        assertTrue(request.key().startsWith("reports/tour-perf-"));
        assertTrue(request.key().endsWith(".csv"));
    }

    @Test
    void test_uploadCsv_propagatesException_whenS3ClientFails() {
        ReflectionTestUtils.setField(s3ReportStorageService, "bucketName", "reports-bucket");
        byte[] csv = "h1,h2\nv1,v2".getBytes();
        LocalDate date = LocalDate.of(2026, 7, 4);
        doThrow(new RuntimeException("s3 unavailable"))
                .when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> s3ReportStorageService.uploadCsv(csv, "tour-perf", date)
        );

        assertEquals("s3 unavailable", ex.getMessage());
    }

    @Test
    void test_uploadCsv_throwsException_whenDataIsNull() {
        ReflectionTestUtils.setField(s3ReportStorageService, "bucketName", "reports-bucket");
        LocalDate date = LocalDate.of(2026, 7, 4);

        assertThrows(NullPointerException.class, () -> s3ReportStorageService.uploadCsv(null, "tour-perf", date));
    }
}


