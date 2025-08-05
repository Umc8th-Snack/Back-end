package umc.snack.service.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import umc.snack.common.config.properties.S3Properties;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.file.dto.FileType;
import umc.snack.domain.file.dto.FileUploadResponseDto;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public FileUploadResponseDto uploadFile(MultipartFile file, String directory) {
        validateFile(file);
        
        String fileName = generateFileName(file.getOriginalFilename());
        String key = directory + "/" + fileName;
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = getFileUrl(key);
            
            log.info("File uploaded successfully: {}", key);
            
            return FileUploadResponseDto.of(
                    fileName,
                    fileUrl,
                    file.getOriginalFilename(),
                    file.getSize()
            );
            
        } catch (IOException e) {
            log.error("Failed to upload file to S3", e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (S3Exception e) {
            log.error("S3 error occurred during file upload", e);
            throw new CustomException(ErrorCode.S3_UPLOAD_ERROR);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();
                    
            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully: {}", key);
            
        } catch (S3Exception e) {
            log.error("Failed to delete file from S3: {}", fileUrl, e);
            throw new CustomException(ErrorCode.S3_DELETE_ERROR);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_NOT_PROVIDED);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        if (!FileType.isImageFile(file.getContentType())) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String generateFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_FILE_NAME);
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String getFileUrl(String key) {
        return "https://" + s3Properties.getBucketName() + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + key;
    }

    private String extractKeyFromUrl(String fileUrl) {
        String bucketUrl = "https://" + s3Properties.getBucketName() + ".s3." + s3Properties.getRegion() + ".amazonaws.com/";
        if (fileUrl.startsWith(bucketUrl)) {
            return fileUrl.substring(bucketUrl.length());
        }
        
        throw new CustomException(ErrorCode.INVALID_FILE_URL);
    }
}