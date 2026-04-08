package com.taskmanager.service;

import com.taskmanager.exception.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final String bucketName;
    private final String region;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public FileService(
            @Value("${aws.s3.access-key}") String accessKey,
            @Value("${aws.s3.secret-key}") String secretKey,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.region}") String region
    ) {
        this.bucketName = bucketName;
        this.region = region;

        // Only initialize S3 client if credentials are provided
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey));

            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();

            this.s3Presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();

            log.info("AWS S3 client initialized for bucket: {} in region: {}", bucketName, region);
        } else {
            this.s3Client = null;
            this.s3Presigner = null;
            log.warn("AWS S3 credentials not configured — file uploads will use local storage fallback");
        }
    }

    public String uploadFile(MultipartFile file) {
        // Try S3 first if configured
        if (s3Client != null) {
            try {
                String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
                String safeFileName = originalName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                String key = "uploads/" + UUID.randomUUID() + "_" + safeFileName;

                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .build();

                log.info("Uploading file to S3: bucket={}, key={}, size={} bytes", bucketName, key, file.getSize());
                s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

                // Generate a pre-signed URL (valid for 7 days) so files are always accessible
                String presignedUrl = generatePresignedUrl(key);
                log.info("S3 upload successful. Pre-signed URL generated for key: {}", key);
                return presignedUrl;

            } catch (Exception e) {
                log.error("AWS S3 upload failed: {}. Falling back to local storage.", e.getMessage());
                // Fall down to local storage
            }
        }

        // Graceful Fallback: Local Storage
        try {
            log.info("Using local file storage fallback...");
            String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String safeFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            String fileName = UUID.randomUUID() + "_" + safeFileName;

            java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads");
            if (!java.nio.file.Files.exists(uploadDir)) {
                java.nio.file.Files.createDirectories(uploadDir);
            }

            java.nio.file.Path filePath = uploadDir.resolve(fileName);
            java.nio.file.Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            log.info("Local file upload successful: /uploads/{}", fileName);
            return "/uploads/" + fileName;
        } catch (IOException ex) {
            log.error("Critical failure: Both S3 and local fallback failed: {}", ex.getMessage());
            throw new FileUploadException("Critical Failure: Both S3 and Local fallback failed. " + ex.getMessage());
        }
    }

    /**
     * Generate a pre-signed download URL for an S3 object.
     * Valid for 7 days — long enough for demo/interview use.
     */
    private String generatePresignedUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(7))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
