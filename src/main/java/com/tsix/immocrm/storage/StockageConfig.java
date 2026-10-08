package com.tsix.immocrm.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * {@code pathStyleAccessEnabled(true)} obligatoire pour MinIO (pas d'adressage par sous-domaine), sur le client
 * <b>et</b> le signataire : sinon les URL présignées visent un hôte inexistant alors que les dépôts marchent.
 */
@Configuration
@EnableConfigurationProperties(StockageProperties.class)
public class StockageConfig {

    private static S3Configuration pourMinio() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }

    private static StaticCredentialsProvider identifiants(StockageProperties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
    }

    @Bean
    public S3Client s3Client(StockageProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(identifiants(properties))
                .serviceConfiguration(pourMinio());
        // Endpoint vide = AWS S3, le SDK le déduit alors de la région.
        if (StringUtils.hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(StockageProperties properties) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(identifiants(properties))
                .serviceConfiguration(pourMinio());
        if (StringUtils.hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        return builder.build();
    }
}
