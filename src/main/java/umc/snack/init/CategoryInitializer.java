package umc.snack.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import umc.snack.domain.feed.entity.Category;
import umc.snack.repository.feed.CategoryRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        List<String> categoryNames = List.of("정치", "경제", "사회", "생활/문화", "세계", "IT/과학", "기타");

        for (String name : categoryNames) {
            boolean exists = categoryRepository.existsByCategoryName(name);
            if (!exists) {
                categoryRepository.save(Category.builder()
                        .categoryName(name)
                        .build());
            }
        }
    }
}
