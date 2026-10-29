package com.epam.edp.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;

@Service
@Slf4j
public class S3ReportStorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public S3ReportStorageService(S3Client s3Client, @Value("${reports.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void uploadCsv(byte[] data, String reportName, LocalDate date) {
        log.debug("uploading csv report to s3 bucket {}, end date={}", bucketName, date);

        String key = "reports/" + reportName + "-" + Instant.now() + ".csv";

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("text/csv")
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromByteBuffer(ByteBuffer.wrap(data))
        );
    }
}
