package umc.snack.domain.feed.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name", length = 50, unique = true)
    private String categoryName;
}
