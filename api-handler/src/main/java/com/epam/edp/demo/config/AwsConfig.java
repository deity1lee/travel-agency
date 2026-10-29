package com.epam.edp.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsConfig {

    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;

    private final String region;
    private final String accountId;
    private final String accessRole;

    public AwsConfig(@Value("${aws.access.key.id}") String accessKey,
                     @Value("${aws.secret.access.key}") String secretKey,
                     @Value("${aws.session.token}") String sessionToken,
                     @Value("${spring.cloud.aws.region.static}") String region,
                     @Value("${account_id}") String accountId,
                     @Value("${access_role}") String accessRole) {
        this.accessKeyId = accessKey;
        this.secretAccessKey = secretKey;
        this.sessionToken = sessionToken;
        this.region = region;
        this.accountId = accountId;
        this.accessRole = accessRole;
    }

    @Bean
    public StaticCredentialsProvider getSsoCredentials() {
        return StaticCredentialsProvider.create(
                AwsSessionCredentials.create(
                        accessKeyId,
                        secretAccessKey,
                        sessionToken)
        );
    }

    @Bean
    public StsClient getStsClient(StaticCredentialsProvider ssoCredentials) {
        return StsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(ssoCredentials)
                .build();
    }

    @Bean
    @Primary
    public AwsCredentialsProvider credentialsProvider(StsClient stsClient) {
        String roleArn = "arn:aws:iam::" + accountId + ":role/" + accessRole;
        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(AssumeRoleRequest.builder()
                        .roleArn(roleArn)
                        .roleSessionName("api-handler")
                        .build())
                .build();
    }

    @Bean
    public SqsClient sqsClient(AwsCredentialsProvider provider) {
        return SqsClient.builder()
                .region(Region.AP_SOUTHEAST_2)
                .credentialsProvider(provider)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsCredentialsProvider provider) {
        return S3Presigner.builder()
                .region(Region.AP_SOUTHEAST_2)
                .credentialsProvider(provider)
                .build();
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider provider) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(provider)
                .build();
    }
}
