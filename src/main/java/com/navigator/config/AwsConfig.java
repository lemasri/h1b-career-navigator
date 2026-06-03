package com.navigator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * AWS client beans.
 *
 * The raw AWS SDK v2 does not auto-configure Spring beans (that is a Spring
 * Cloud AWS feature, which this project does not use), so the SnsClient that
 * {@link com.navigator.service.NotificationService} depends on must be defined
 * explicitly here.
 *
 * Building the client is lazy with respect to credentials — the default
 * provider chain is only consulted when a request is actually published, so
 * the application starts cleanly in local development without AWS credentials.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
