package umc.snack.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.global.BaseEntity;
import umc.snack.domain.user.SearchKeywordId;

@Entity
@Table(name = "search_keywords")
@IdClass(SearchKeywordId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SearchKeyword extends BaseEntity {

    @Id
    @Column(name = "search_id")
    private Long searchId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    private String keyword;

    // 연관관계 맵핑 따로 추가 (선택)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
