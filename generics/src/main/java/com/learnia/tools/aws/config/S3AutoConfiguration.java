package com.learnia.tools.aws.config;

import java.net.URI;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.learnia.tools.aws.S3Tool;
import com.learnia.tools.aws.impl.S3ToolImpl;
import com.learnia.tools.aws.properties.S3Properties;
import com.learnia.tools.aws.service.S3StorageService;
import com.learnia.tools.aws.service.impl.S3StorageServiceImpl;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;

@AutoConfiguration
@EnableConfigurationProperties(S3Properties.class)
public class S3AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public S3Client s3Client(S3Properties properties) {
        if (properties.getRegion() == null || properties.getRegion().isBlank()) {
            throw new IllegalStateException("Property aws.s3.region must be configured");
        }

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
                        .build());

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        if (properties.getAccessKey() != null && !properties.getAccessKey().isBlank()
                && properties.getSecretKey() != null && !properties.getSecretKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public S3Presigner s3Presigner(S3Properties properties) {
        Builder builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
                        .build());

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        if (properties.getAccessKey() != null && !properties.getAccessKey().isBlank()
                && properties.getSecretKey() != null && !properties.getSecretKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public S3Tool s3Tool(S3Client s3Client, S3Presigner s3Presigner) {
        return new S3ToolImpl(s3Client, s3Presigner);
    }

    @Bean
    @ConditionalOnMissingBean
    public S3StorageService s3StorageService(S3Tool s3Tool, S3Properties s3Properties) {
        return new S3StorageServiceImpl(s3Tool, s3Properties);
    }
}
