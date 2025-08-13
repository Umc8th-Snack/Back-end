package umc.snack.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.snack.common.config.security.CustomUserDetails;
import umc.snack.common.dto.ApiResponse;
import umc.snack.service.user.SearchKeywordService;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "검색어 히스토리 관련 API")
public class SearchKeywordController {

    private final SearchKeywordService searchKeywordService;

    @Operation(
            summary = "최근 검색어 조회",
            description = "유저의 최근 검색어 최대 10개를 조회합니다."
    )
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<String>>> getRecentKeywords(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getUserId();
        List<String> keywords = searchKeywordService.getRecentKeywords(userId);
        return ResponseEntity.ok(ApiResponse.onSuccess("SEARCH_9302", "최근 검색어 조회 성공", keywords));
    }
}
