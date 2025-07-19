package umc.snack.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import umc.snack.domain.feed.entity.Category;
import umc.snack.repository.feed.CategoryRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        List<String> categoryNames = List.of("정치", "경제", "사회", "생활/문화", "세계", "IT/과학", "기타");

        for (String name : categoryNames) {
            try {
                boolean exists = categoryRepository.existsByCategoryName(name);
                if (!exists) {
                    categoryRepository.save(Category.builder()
                            .categoryName(name)
                            .build());
                }
            } catch (Exception e) {
                log.error("카테고리 '{}' 초기화 중 오류 발생: {}", name, e.getMessage());
            }
        }
    }
}
