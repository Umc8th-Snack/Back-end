package umc.snack.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "quizzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Quiz extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Long quizId;

    @Column(name = "quiz_content", columnDefinition = "JSON")
    private String quizContent;
}
