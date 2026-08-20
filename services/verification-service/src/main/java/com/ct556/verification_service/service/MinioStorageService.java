package com.ct556.verification_service.service;

import com.ct556.verification_service.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioStorageService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public byte[] getObjectBytes(String objectKey) {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectKey)
                        .build()
        )) {
            return inputStream.readAllBytes();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to download signed output from MinIO", ex);
        }
    }

    public boolean objectExists(String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (ErrorResponseException ex) {
            return false;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to check signed output in MinIO", ex);
        }
    }
}
