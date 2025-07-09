package umc.snack.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.domain.feed.entity.Category;
import umc.snack.global.BaseEntity;
import umc.snack.domain.user.UserCategoryScoreId;

@Entity
@Table(name = "user_category_score")
@IdClass(UserCategoryScoreId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserCategoryScore extends BaseEntity {

    @Id
    @Column(name = "score_id")
    private Long scoreId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @Column(name = "scrap_score")
    private Float scrapScore;

    @Column(name = "click_score")
    private Float clickScore;

    @Column(name = "search_score")
    private Float searchScore;

    @Column(name = "behavior_score")
    private Float behaviorScore;
}
