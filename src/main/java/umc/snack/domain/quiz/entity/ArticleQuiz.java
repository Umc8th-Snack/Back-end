package umc.snack.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.article.entity.Article;
import umc.snack.domain.quiz.ArticleQuizId;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "article_quiz")
@IdClass(ArticleQuizId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ArticleQuiz extends BaseEntity {

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @Id
    @Column(name = "quiz_id")
    private Long quizId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", insertable = false, updatable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", insertable = false, updatable = false)
    private Quiz quiz;
}
