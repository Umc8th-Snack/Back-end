package umc.snack.service.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import umc.snack.common.config.properties.S3Properties;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.file.dto.FileType;
import umc.snack.domain.file.dto.FileUploadResponseDto;
import umc.snack.domain.user.entity.User;
import umc.snack.repository.user.UserRepository;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService {

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final UserRepository userRepository;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public FileUploadResponseDto uploadFile(MultipartFile file, String directory) {
        validateFile(file);
        
        String fileName = generateFileName(file.getOriginalFilename());
        String key = directory + "/" + fileName;
        
        log.info("Attempting to upload file to S3 - Bucket: {}, Region: {}, Key: {}", 
                s3Properties.getBucketName(), s3Properties.getRegion(), key);
        
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
            log.error("IOException occurred while reading file: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (S3Exception e) {
            log.error("S3Exception occurred during file upload - Error Code: {}, Status Code: {}, Message: {}", 
                    e.awsErrorDetails().errorCode(), 
                    e.statusCode(), 
                    e.awsErrorDetails().errorMessage(), e);
            throw new CustomException(ErrorCode.S3_UPLOAD_ERROR);
        } catch (Exception e) {
            log.error("Unexpected error occurred during file upload: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.S3_UPLOAD_ERROR);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            
            // 파일 존재 여부 확인
            if (!fileExists(key)) {
                log.warn("Attempted to delete non-existent file: {}", key);
                throw new CustomException(ErrorCode.FILE_NOT_FOUND);
            }
            
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

    public void deleteProfileImage(String fileUrl) {
        // 현재 로그인한 사용자 확인
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDetails.getUser();

        // DB에서 사용자 정보 조회 (최신 정보)
        User managedUser = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_2622));

        // 프로필 이미지 URL 검증
        if (managedUser.getProfileImage() == null || !managedUser.getProfileImage().equals(fileUrl)) {
            log.warn("User {} attempted to delete profile image they don't own. Requested: {}, Actual: {}", 
                    currentUser.getUserId(), fileUrl, managedUser.getProfileImage());
            throw new CustomException(ErrorCode.UNAUTHORIZED_FILE_ACCESS);
        }

        try {
            String key = extractKeyFromUrl(fileUrl);
            
            // 파일 존재 여부 확인
            if (!fileExists(key)) {
                log.warn("Attempted to delete non-existent profile image: {}", key);
                throw new CustomException(ErrorCode.FILE_NOT_FOUND);
            }
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();
                    
            s3Client.deleteObject(deleteObjectRequest);
            
            // DB에서 프로필 URL 제거 (닉네임과 소개는 기존 값 유지, 프로필 URL만 null로 설정)
            managedUser.updateUserInfo(managedUser.getNickname(), null, managedUser.getIntroduction());
            userRepository.save(managedUser);
            
            log.info("Profile image deleted successfully: {}", key);
            
        } catch (S3Exception e) {
            log.error("Failed to delete profile image from S3: {}", fileUrl, e);
            throw new CustomException(ErrorCode.S3_DELETE_ERROR);
        }
    }

    /**
     * S3 연결과 설정을 테스트하는 메서드
     */
    public void testS3Connection() {
        try {
            log.info("Testing S3 connection - Bucket: {}, Region: {}", 
                    s3Properties.getBucketName(), s3Properties.getRegion());
            
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .build();
                    
            s3Client.headBucket(headBucketRequest);
            log.info("S3 connection test successful - Bucket exists and is accessible");
            
        } catch (S3Exception e) {
            log.error("S3 connection test failed - Error Code: {}, Status Code: {}, Message: {}", 
                    e.awsErrorDetails().errorCode(), 
                    e.statusCode(), 
                    e.awsErrorDetails().errorMessage(), e);
            throw new CustomException(ErrorCode.S3_UPLOAD_ERROR);
        } catch (Exception e) {
            log.error("S3 connection test failed with unexpected error: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.S3_UPLOAD_ERROR);
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

    /**
     * S3에 파일이 존재하는지 확인
     */
    private boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(key)
                    .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            log.debug("File does not exist in S3: {}", key);
            return false;
        } catch (S3Exception e) {
            log.error("Error checking file existence in S3: {}", key, e);
            throw new CustomException(ErrorCode.S3_DELETE_ERROR);
        }
    }
}