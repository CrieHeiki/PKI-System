package com.ct556.document_service.config;

import com.ct556.document_service.service.MinioProperties;
import com.ct556.document_service.service.MinioStorageService;
import io.minio.MinioClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    public ApplicationRunner ensureMinioBucketOnStartup(MinioStorageService minioStorageService) {
        return args -> minioStorageService.ensureBucketExists();
    }
}