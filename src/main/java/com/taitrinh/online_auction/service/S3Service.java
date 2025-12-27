package com.taitrinh.online_auction.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.taitrinh.online_auction.exception.FileUploadException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Service for handling file uploads to AWS S3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB in bytes

    // Image dimension constraints
    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 600;
    private static final int MAX_WIDTH = 5000;
    private static final int MAX_HEIGHT = 5000;

    /**
     * Upload a file to S3 and return the public URL
     * 
     * @param file   The multipart file to upload
     * @param folder The folder path within the bucket (e.g., "products")
     * @return The public URL of the uploaded file
     * @throws FileUploadException if upload fails or validation errors occur
     */
    public String uploadFile(MultipartFile file, String folder) {
        log.debug("Uploading file: {} to folder: {}", file.getOriginalFilename(), folder);

        // Validate file is not empty
        if (file.isEmpty()) {
            throw new FileUploadException("Tồn tại một ảnh bị trống");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException(
                    String.format("Tồn tại một ảnh có kích thước file quá lớn. Tối đa %dMB, file hiện tại: %.2fMB",
                            MAX_FILE_SIZE / (1024 * 1024),
                            file.getSize() / (1024.0 * 1024.0)));
        }

        // Validate file type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidImageFile(originalFilename)) {
            throw new FileUploadException(
                    "Tồn tại một ảnh không đúng định dạng. Chỉ chấp nhận file ảnh với định dạng: "
                            + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // Read file bytes once (to avoid stream consumption issues)
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Error reading file bytes: {}", e.getMessage());
            throw new FileUploadException("Lỗi khi đọc file ảnh: " + e.getMessage());
        }

        // Validate image dimensions using the byte array
        try {
            log.debug("Validating image dimensions for: {}", originalFilename);

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));

            if (image == null) {
                log.error("ImageIO.read() returned null for file: {}", originalFilename);
                log.error("File size: {} bytes, Content-Type: {}", fileBytes.length, file.getContentType());

                // Log magic number for debugging
                if (fileBytes.length >= 4) {
                    String magicNumber = String.format("%02X %02X %02X %02X",
                            fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3]);
                    log.error("Magic number: {}", magicNumber);
                }

                throw new FileUploadException(
                        String.format("Không thể đọc file ảnh '%s'. File có thể bị hỏng hoặc định dạng không hợp lệ.",
                                originalFilename));
            }

            int width = image.getWidth();
            int height = image.getHeight();

            log.info("Image dimensions: {}x{} for file: {}", width, height, originalFilename);

            // Check minimum dimensions
            if (width < MIN_WIDTH || height < MIN_HEIGHT) {
                throw new FileUploadException(
                        String.format(
                                "Tồn tại một ảnh có kích thước quá nhỏ. Tối thiểu %dx%d pixel, ảnh hiện tại: %dx%d pixel",
                                MIN_WIDTH, MIN_HEIGHT, width, height));
            }

            // Check maximum dimensions
            if (width > MAX_WIDTH || height > MAX_HEIGHT) {
                throw new FileUploadException(
                        String.format(
                                "Tồn tại một ảnh có kích thước quá lớn. Tối đa %dx%d pixel, ảnh hiện tại: %dx%d pixel",
                                MAX_WIDTH, MAX_HEIGHT, width, height));
            }

            log.debug("Image dimensions validated successfully: {}x{}", width, height);

        } catch (FileUploadException e) {
            throw e; // Re-throw our custom exceptions
        } catch (IOException e) {
            log.error("Error reading image dimensions: {}", e.getMessage());
            throw new FileUploadException("Lỗi khi đọc thông tin ảnh: " + e.getMessage());
        }

        // Generate unique filename
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        String key = folder + "/" + uniqueFilename;

        try {
            // Create put request
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            // Upload file using the pre-read bytes
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

            // Generate public URL
            String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, key);

            log.info("File uploaded successfully: {}", publicUrl);
            return publicUrl;

        } catch (S3Exception e) {
            log.error("S3 error uploading file: {}", e.getMessage(), e);
            throw new FileUploadException("Lỗi khi upload file lên S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Delete a file from S3 using its URL
     * 
     * @param fileUrl The full S3 URL of the file to delete
     * @throws FileUploadException if deletion fails
     */
    public void deleteFile(String fileUrl) {
        try {
            // Extract key from URL
            // URL format: https://bucket-name.s3.region.amazonaws.com/folder/filename.ext
            String key = fileUrl.substring(fileUrl.indexOf(".com/") + 5);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully: {}", fileUrl);

        } catch (S3Exception e) {
            log.error("S3 error deleting file: {}", e.getMessage(), e);
            throw new FileUploadException("Lỗi khi xóa file trên S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            throw new FileUploadException("Lỗi khi xóa file: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the file has a valid image extension
     */
    private boolean isValidImageFile(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
}
