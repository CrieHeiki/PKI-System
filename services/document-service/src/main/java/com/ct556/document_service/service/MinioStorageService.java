package com.ct556.document_service.service;

import com.ct556.document_service.exception.StorageException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioStorageService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioProperties.getBucket())
                                .build()
                );
            }
        } catch (Exception ex) {
            throw new StorageException("Failed to ensure MinIO bucket exists", ex);
        }
    }

    public void upload(Path filePath, String objectKey, String contentType) {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            long size = Files.size(filePath);
            putObject(inputStream, size, objectKey, contentType);
        } catch (Exception ex) {
            throw new StorageException("Failed to upload file to MinIO", ex);
        }
    }

    public void uploadBytes(byte[] content, String objectKey, String contentType) {
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            putObject(inputStream, content.length, objectKey, contentType);
        } catch (Exception ex) {
            throw new StorageException("Failed to upload byte content to MinIO", ex);
        }
    }

    public InputStream getObject(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ex) {
            throw new StorageException("Failed to download file from MinIO", ex);
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
            throw new StorageException("Failed to check object existence in MinIO", ex);
        }
    }

    private void putObject(InputStream inputStream, long size, String objectKey, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectKey)
                        .stream(inputStream, size, -1)
                        .contentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream")
                        .build()
        );
    }
}
