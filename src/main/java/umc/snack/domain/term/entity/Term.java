package umc.snack.domain.term.entity;

import jakarta.persistence.*;
import lombok.*;
import umc.snack.global.BaseEntity;

@Entity
@Table(name = "terms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Term extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long termId;

    private String word;

    @Column(columnDefinition = "TEXT")
    private String definition;
}
