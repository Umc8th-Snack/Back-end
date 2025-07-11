package umc.snack.domain.share.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShareResultDto {
    //POST 응답의 result 필드용
    private String uuid; // API 명세의 "uuid" 필드
    private String sharedUrl;
}