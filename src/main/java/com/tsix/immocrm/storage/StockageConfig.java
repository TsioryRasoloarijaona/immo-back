package com.tsix.immocrm.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Clients S3 : un pour les écritures, un pour la signature des URL de lecture.
 *
 * <p><b>{@code pathStyleAccessEnabled(true)} est obligatoire pour MinIO</b>, qui ne sait pas
 * adresser un bucket par sous-domaine ({@code bucket.s3.exemple.com}) mais seulement par
 * chemin ({@code s3.exemple.com/bucket}). Sans cette option, tous les appels échouent en
 * résolution DNS. Elle doit être posée sur le client <b>et</b> sur le signataire, faute de quoi
 * les URL présignées pointent vers un hôte inexistant alors que les dépôts fonctionnent.
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
        S3Client.Builder builder = S3Client.builder()
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
