package umc.snack.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.snack.common.exception.CustomException;
import umc.snack.common.exception.ErrorCode;
import umc.snack.domain.user.entity.SearchKeyword;
import umc.snack.repository.user.SearchKeywordRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchKeywordService {

    private final SearchKeywordRepository repository;

    @Cacheable(value = "searchKeyword", key = "#userId")
    public List<String> getRecentKeywords(Long userId) {
        try {
            return repository.findTop10ByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(SearchKeyword::getKeyword)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SEARCH_9401);
        }
    }

    @Transactional
    @CacheEvict(value = "searchKeyword", key = "#userId")
    public void saveKeyword(Long userId, String keyword) {
        try {
            Optional<SearchKeyword> existing = repository.findByUserIdAndKeyword(userId, keyword);

            if (existing.isPresent()) {
                SearchKeyword history = existing.get();
                history.setCreatedAt(LocalDateTime.now());
            } else {
                SearchKeyword keywordEntity = SearchKeyword.builder()
                        .userId(userId)
                        .keyword(keyword)
                        .build();
                repository.save(keywordEntity);
            }

            List<SearchKeyword> all = repository.findAllByUserIdOrderByCreatedAtDesc(userId);
            if (all.size() > 10) {
                repository.deleteAll(all.subList(10, all.size()));
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.SEARCH_9400);
        }
    }
}
