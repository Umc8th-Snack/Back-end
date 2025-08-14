package umc.snack.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "search_keywords", uniqueConstraints = {
        // (user_id, keyword) 조합이 유니크하도록 제약조건 추가
        @UniqueConstraint(
                name = "search_keyword_uk",
                columnNames = {"user_id", "keyword"}
        )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SearchKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_id") // DB 컬럼명은 스네이크 케이스로
    private Long searchId;      // 필드명은 원하시는 대로 카멜 케이스로

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String keyword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "article_id", nullable = false)
    private Long articleId;
}