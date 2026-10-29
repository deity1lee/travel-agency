package com.epam.edp.demo.service.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.epam.edp.demo.entity.document.FileFormat;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3PresignedUrlServiceTest {

    @Mock
    private S3Presigner presigner;

    @Mock
    private S3Client client;

    @InjectMocks
    private S3PresignedUrlService service;

    @Test
    void generateUploadUrl_returnsPresignedUrl() throws MalformedURLException {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://s3.example.com/upload"));
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        String result = service.generateUploadUrl("my-bucket", "bookings/booking-1/doc-1.pdf");

        assertThat(result).isEqualTo("https://s3.example.com/upload");
        verify(presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void generateRetrieveUrl_returnsPresignedUrl() throws MalformedURLException {
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://s3.example.com/download"));
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        String result = service.generateRetrieveUrl("my-bucket", "bookings/booking-1/doc-1.pdf");

        assertThat(result).isEqualTo("https://s3.example.com/download");
        verify(presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void generatePictureUploadUrl_returnsPresignedUrl() throws MalformedURLException {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://s3.example.com/picture-upload"));
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        String result = service.generatePictureUploadUrl(
                "my-bucket", "users/profile-pictures/user-1.png", FileFormat.png);

        assertThat(result).isEqualTo("https://s3.example.com/picture-upload");
        verify(presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void verifyFileExistsByKey_returnsTrue_whenObjectExists() {
        when(client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        boolean result = service.verifyFileExistsByKey("my-bucket", "bookings/booking-1/doc-1.pdf");

        assertThat(result).isTrue();
        verify(client).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void verifyFileExistsByKey_returnsFalse_whenObjectDoesNotExist() {
        when(client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        boolean result = service.verifyFileExistsByKey("my-bucket", "bookings/booking-1/missing.pdf");

        assertThat(result).isFalse();
        verify(client).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void getDocumentsBucketName_returnsConfiguredValue() {
        S3PresignedUrlService serviceWithBucket =
                new S3PresignedUrlService(client, presigner, "documents-bucket");

        assertThat(serviceWithBucket.getDocumentsBucketName()).isEqualTo("documents-bucket");
    }
}
