package umc.snack.domain.file.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum FileType {
    
    JPEG("image/jpeg", "jpg"),
    JPG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp");

    private final String mimeType;
    private final String extension;

    public static FileType fromMimeType(String mimeType) {
        return Arrays.stream(FileType.values())
                .filter(fileType -> fileType.getMimeType().equalsIgnoreCase(mimeType))
                .findFirst()
                .orElse(null);
    }

    public static boolean isImageFile(String mimeType) {
        return fromMimeType(mimeType) != null;
    }
}