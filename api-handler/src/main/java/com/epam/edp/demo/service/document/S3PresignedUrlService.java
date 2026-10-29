package com.epam.edp.demo.service.document;

import com.epam.edp.demo.entity.document.FileFormat;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
public class S3PresignedUrlService {

    private static final Duration URL_EXPIRATION = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner presigner;

    @Getter
    private final String documentsBucketName;

    public static final String PICTURE_UPLOAD_URL_BASE = "users/profile-pictures/%s";

    public S3PresignedUrlService(S3Client s3Client,
                                 S3Presigner s3Presigner,
                                 @Value("${documents.bucket-name}") String documentsBucketName) {
        this.s3Client = s3Client;
        this.presigner = s3Presigner;
        this.documentsBucketName = documentsBucketName;
    }

    public String generateUploadUrl(String bucketName, String s3Key) {

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(URL_EXPIRATION)
                        .putObjectRequest(putObjectRequest)
                        .build();

        return presigner.presignPutObject(presignRequest)
                .url()
                .toString();
    }

    public String generatePictureUploadUrl(String bucketName, String s3Key, FileFormat format) {

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .contentType(format.getContentType())
                .key(s3Key)
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(URL_EXPIRATION)
                        .putObjectRequest(putObjectRequest)
                        .build();

        return presigner.presignPutObject(presignRequest)
                .url()
                .toString();
    }

    public String generateRetrieveUrl(String bucketName, String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(URL_EXPIRATION)
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }

    public boolean verifyFileExistsByKey(String bucketName, String s3Key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }
}
