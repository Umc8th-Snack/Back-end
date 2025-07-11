package umc.snack.controller.feed;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "피드 관련 API")
public class FeedController {

    @Operation(summary = "전체 피드에서 기사 제공", description = "메인 피드에서 특정 카테고리의 기사를 페이지네이션하여 제공합니다.")
    @GetMapping("/main/{category}")
    public ResponseEntity<?> getMainFeedArticles(
            @PathVariable String category,
            @RequestParam(defaultValue = "1") int page) {
        // TODO: 개발 예정
        return ResponseEntity.ok("메인 피드 기사 제공 API - 개발 예정 (category: " + category + ", page: " + page + ")");
    }

    @Operation(summary = "맞춤 피드에서 기사 제공", description = "사용자에게 맞춤화된 기사를 페이지네이션하여 제공합니다.")
    @GetMapping("/personalized")
    public ResponseEntity<?> getPersonalizedFeedArticles(
            @RequestParam(defaultValue = "1") int page) {
        // TODO: 개발 예정
        return ResponseEntity.ok("맞춤 피드 기사 제공 API - 개발 예정 (page: " + page + ")");
    }
}