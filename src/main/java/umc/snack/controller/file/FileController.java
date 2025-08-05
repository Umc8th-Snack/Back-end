package umc.snack.controller.file;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import umc.snack.common.dto.ApiResponse;
import umc.snack.domain.file.dto.FileUploadResponseDto;
import umc.snack.service.file.S3FileService;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File", description = "파일 업로드 관련 API")
public class FileController {

    private final S3FileService s3FileService;

    @Operation(summary = "이미지 파일 업로드", description = "S3에 이미지 파일을 업로드합니다. 지원 형식: JPEG, JPG, PNG, GIF, WEBP (최대 5MB)")
    @PostMapping("/upload/image")
    public ResponseEntity<ApiResponse<FileUploadResponseDto>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        
        FileUploadResponseDto result = s3FileService.uploadFile(file, "images");
        
        return ResponseEntity.ok(
                ApiResponse.onSuccess("FILE_2701", "이미지 업로드가 성공적으로 완료되었습니다.", result)
        );
    }

    @Operation(summary = "프로필 이미지 업로드", description = "S3에 프로필 이미지를 업로드합니다. 지원 형식: JPEG, JPG, PNG, GIF, WEBP (최대 5MB)")
    @PostMapping("/upload/profile")
    public ResponseEntity<ApiResponse<FileUploadResponseDto>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) {
        
        FileUploadResponseDto result = s3FileService.uploadFile(file, "profiles");
        
        return ResponseEntity.ok(
                ApiResponse.onSuccess("FILE_2702", "프로필 이미지 업로드가 성공적으로 완료되었습니다.", result)
        );
    }

    @Operation(summary = "파일 삭제", description = "S3에서 파일을 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @RequestParam("fileUrl") String fileUrl) {
        
        s3FileService.deleteFile(fileUrl);
        
        return ResponseEntity.ok(
                ApiResponse.onSuccess("FILE_2703", "파일이 성공적으로 삭제되었습니다.", null)
        );
    }
}