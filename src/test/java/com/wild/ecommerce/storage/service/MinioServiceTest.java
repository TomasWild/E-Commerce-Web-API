package com.wild.ecommerce.storage.service;

import com.wild.ecommerce.common.exception.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MinioServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private MinioServiceImpl minioService;

    private static final String TEST_BUCKET_NAME = "test-bucket";
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(minioService, "endpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(minioService, "bucketName", TEST_BUCKET_NAME);

        mockFile = new MockMultipartFile(
                "image",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
    }

    @Test
    void uploadImage_WithValidFile_ShouldReturnImageUrl() {
        // Arrange
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        String result = minioService.uploadImage(mockFile);

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("http://localhost:9000"));
        assertTrue(result.contains(TEST_BUCKET_NAME));
        assertTrue(result.contains("test-image.jpg"));

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(TEST_BUCKET_NAME, capturedRequest.bucket());
        assertEquals("image/jpeg", capturedRequest.contentType());
        assertTrue(capturedRequest.key().contains("test-image.jpg"));
    }

    @Test
    void uploadImage_ShouldGenerateUniqueFileName() {
        // Arrange
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        String result1 = minioService.uploadImage(mockFile);
        String result2 = minioService.uploadImage(mockFile);

        // Assert
        assertNotEquals(result1, result2);

        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadImage_WithIOException_ShouldThrowIllegalStateException() throws IOException {
        // Arrange
        MultipartFile faultyFile = mock(MultipartFile.class);
        when(faultyFile.getOriginalFilename()).thenReturn("test.jpg");
        when(faultyFile.getContentType()).thenReturn("image/jpeg");
        when(faultyFile.getInputStream()).thenThrow(new IOException("Failed to read file"));

        // Act & Assert
        FileUploadException exception = assertThrows(
                FileUploadException.class,
                () -> minioService.uploadImage(faultyFile)
        );

        assertEquals("Failed to upload image to S3", exception.getMessage());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadImage_ShouldUploadWithCorrectContentType() {
        // Arrange
        MultipartFile pngFile = new MockMultipartFile(
                "image",
                "test-image.png",
                "image/png",
                "test png content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        minioService.uploadImage(pngFile);

        // Assert
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        assertEquals("image/png", requestCaptor.getValue().contentType());
    }

    @Test
    void deleteImage_WithValidUrl_ShouldDeleteFromS3() {
        // Arrange
        String imageUrl = "http://localhost:9000/test-bucket/uuid_test-image.jpg";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        // Act
        minioService.deleteImage(imageUrl);

        // Assert
        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(TEST_BUCKET_NAME, capturedRequest.bucket());
        assertEquals("uuid_test-image.jpg", capturedRequest.key());
    }

    @Test
    void deleteImage_WithComplexFileName_ShouldExtractCorrectKey() {
        // Arrange
        String imageUrl = "http://localhost:9000/test-bucket/123e4567-e89b-12d3-a456-426614174000_my-image.jpg";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        // Act
        minioService.deleteImage(imageUrl);

        // Assert
        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        assertEquals("123e4567-e89b-12d3-a456-426614174000_my-image.jpg",
                requestCaptor.getValue().key());
    }

    @Test
    void deleteImage_WhenS3ThrowsException_ShouldPropagateException() {
        // Arrange
        String imageUrl = "http://localhost:9000/test-bucket/test-image.jpg";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .message("Access Denied")
                        .statusCode(403)
                        .build());

        // Act & Assert
        assertThrows(S3Exception.class, () -> minioService.deleteImage(imageUrl));
    }

    @Test
    void ensureBucketExists_WhenBucketExists_ShouldNotCreateBucket() {
        // Arrange
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenReturn(HeadBucketResponse.builder().build());

        // Act
        ReflectionTestUtils.invokeMethod(minioService, "ensureBucketExists");

        // Assert
        verify(s3Client).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void ensureBucketExists_WhenBucketNotFound_ShouldCreateBucket() {
        // Arrange
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(S3Exception.builder()
                        .message("Not Found")
                        .statusCode(404)
                        .build());

        when(s3Client.createBucket(any(CreateBucketRequest.class)))
                .thenReturn(CreateBucketResponse.builder().build());

        // Act
        ReflectionTestUtils.invokeMethod(minioService, "ensureBucketExists");

        // Assert
        ArgumentCaptor<CreateBucketRequest> requestCaptor = ArgumentCaptor.forClass(CreateBucketRequest.class);
        verify(s3Client).createBucket(requestCaptor.capture());
        assertEquals(TEST_BUCKET_NAME, requestCaptor.getValue().bucket());
    }

    @Test
    void ensureBucketExists_WhenS3ThrowsNon404Error_ShouldThrowException() {
        // Arrange
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(S3Exception.builder()
                        .message("Access Denied")
                        .statusCode(403)
                        .build());

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(minioService, "ensureBucketExists")
        );

        assertTrue(exception.getMessage().contains("Failed to check if bucket"));
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void uploadImage_WithEmptyFileName_ShouldStillUpload() {
        // Arrange
        MultipartFile fileWithNoName = new MockMultipartFile(
                "image",
                "",
                "image/jpeg",
                "test content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        String result = minioService.uploadImage(fileWithNoName);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(TEST_BUCKET_NAME));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadImage_WithLargeFile_ShouldHandleCorrectly() {
        // Arrange
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        MultipartFile largeFile = new MockMultipartFile(
                "image",
                "large-image.jpg",
                "image/jpeg",
                largeContent
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Act
        String result = minioService.uploadImage(largeFile);

        // Assert
        assertNotNull(result);

        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(any(PutObjectRequest.class), bodyCaptor.capture());
    }
}
