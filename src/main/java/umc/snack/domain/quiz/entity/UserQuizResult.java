package umc.snack.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.quiz.UserQuizResultId;
import umc.snack.domain.user.entity.User;
import umc.snack.global.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_quiz_results")
@IdClass(UserQuizResultId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserQuizResult extends BaseEntity {

    public enum Status {
        SOLVED,
        UNSOLVED
    }

    public enum Correctness {
        CORRECT,
        WRONG
    }

    @Id
    @Column(name = "result_id")
    private Long resultId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "quiz_id")
    private Long quizId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", insertable = false, updatable = false)
    private Quiz quiz;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_correct")
    private Correctness isCorrect;

    @Column(name = "submitted_answer")
    private Integer submittedAnswer;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}