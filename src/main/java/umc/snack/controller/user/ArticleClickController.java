package umc.snack.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import umc.snack.domain.user.dto.UserClicksDto;
import umc.snack.service.user.UserClickService;
import umc.snack.common.config.security.CustomUserDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
@Tag(name = "Article Click", description = "기사 클릭 관련 API")
public class ArticleClickController {
    
    private final UserClickService userClickService;
    
    @PostMapping("/{articleId}/click")
    @Operation(summary = "기사 클릭 로그 저장", description = "사용자가 기사를 클릭했을 때 로그를 저장합니다.")
    public ResponseEntity<Map<String, Object>> saveArticleClick(
            @PathVariable Long articleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getUserId();
        
        UserClicksDto userClicksDto = UserClicksDto.builder()
                .userId(userId)
                .articleId(articleId)
                .build();
        
        userClickService.saveUserClick(userClicksDto);
        
        Map<String, Object> response = new HashMap<>();
        response.put("isSuccess", true);
        response.put("code", "FEED_9506");
        response.put("message", "클릭 로그가 저장되었습니다.");
        response.put("result", null);
        
        return ResponseEntity.status(201).body(response);
    }
    
    @GetMapping("/clicked")
    @Operation(summary = "사용자가 클릭한 기사 조회", description = "현재 로그인한 사용자가 클릭한 기사 ID 목록을 반환합니다.")
    public ResponseEntity<Map<String, Object>> getClickedArticles(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getUserId();
        
        List<Long> articleIds = userClickService.getClickedArticleIds(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("clickedArticleIds", articleIds);
        result.put("count", articleIds.size());
        
        Map<String, Object> response = new HashMap<>();
        response.put("isSuccess", true);
        response.put("code", "FEED_9507");
        response.put("message", "클릭한 기사 조회에 성공했습니다.");
        response.put("result", result);
        
        return ResponseEntity.ok(response);
    }
}
