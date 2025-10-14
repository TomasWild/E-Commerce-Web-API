package com.wild.ecommerce.storage.service;

import com.wild.ecommerce.common.exception.FileUploadException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioServiceImpl implements MinioService {

    private final S3Client s3Client;

    @Value("${minio.s3.endpoint}")
    private String endpoint;

    @Value("${minio.s3.bucket_name}")
    private String bucketName;

    @Override
    public String uploadImage(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            String fileUrl = String.format("%s/%s/%s", endpoint, bucketName, fileName);
            log.info("Uploaded image to S3: {}", fileUrl);

            return fileUrl;
        } catch (IOException e) {
            log.error("Failed to upload image to S3", e);
            throw new FileUploadException("Failed to upload image to S3");
        }
    }

    @Override
    public void deleteImage(String imageUrl) {
        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("Deleted image '{}' from S3 bucket '{}'", fileName, bucketName);
    }

    @PostConstruct
    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("S3 bucket '{}' already exists", bucketName);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.info("S3 bucket '{}' not found — creating...", bucketName);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("Created S3 bucket '{}'", bucketName);
            } else {
                log.error("Failed to check if bucket '{}' exists", bucketName, e);
                throw new IllegalStateException("Failed to check if bucket '" + bucketName + "' exists");
            }
        }
    }
}
