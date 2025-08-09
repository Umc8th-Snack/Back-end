package umc.snack.domain.file.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadResponseDto {
    
    private String fileName;
    private String fileUrl;
    private String originalFileName;
    private Long fileSize;
    
    public static FileUploadResponseDto of(String fileName, String fileUrl, String originalFileName, Long fileSize) {
        return FileUploadResponseDto.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .originalFileName(originalFileName)
                .fileSize(fileSize)
                .build();
    }
}