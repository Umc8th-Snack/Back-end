package umc.snack.controller.feed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import umc.snack.domain.feed.dto.ArticleInFeedDto;
import umc.snack.domain.feed.dto.IndividualArticleDto;
import umc.snack.service.feed.FeedService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedController.class)
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @Test
    @DisplayName("카테고리별 피드 첫 페이지 조회 성공")
    @WithMockUser
    void getMainFeedArticles_firstPage_success() throws Exception {
        // given
        String category = "IT/과학";
        ArticleInFeedDto mockResponse = ArticleInFeedDto.builder()
                .category("IT/과학")
                .hasNext(true)
                .nextCursorId(100L)
                .articles(Collections.singletonList(
                        IndividualArticleDto.builder().articleId(100L).title("테스트 기사").build()
                ))
                .build();

        given(feedService.getMainFeedByCategory(eq(category), eq(null), any())).willReturn(mockResponse);

        // when & then: URL에서 {category} 제거, .param()으로 추가
        mockMvc.perform(get("/api/feeds/main")
                        .param("category", category))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.category").value("IT/과학"));
    }

    @Test
    @DisplayName("카테고리별 피드 다음 페이지 조회 성공 (커서 사용)")
    @WithMockUser
    void getMainFeedArticles_nextPage_success() throws Exception {
        // given
        String category = "경제";
        Long lastArticleId = 100L;
        ArticleInFeedDto mockResponse = ArticleInFeedDto.builder()
                .category("경제")
                .hasNext(false)
                .nextCursorId(null)
                .articles(Collections.emptyList())
                .build();

        given(feedService.getMainFeedByCategory(eq(category), eq(lastArticleId), any())).willReturn(mockResponse);

        // when & then: URL에서 {category} 제거, .param()으로 추가
        mockMvc.perform(get("/api/feeds/main")
                        .param("category", category)
                        .param("lastArticleId", String.valueOf(lastArticleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.category").value("경제"));
    }
}