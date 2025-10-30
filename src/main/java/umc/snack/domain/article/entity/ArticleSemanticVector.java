package umc.snack.domain.article.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.snack.global.BaseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "article_semantic_vectors")
public class ArticleSemanticVector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vector_id")
    private Long vectorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false, unique = true)
    private Article article;

    @Column(name = "vector", columnDefinition = "TEXT")
    private String vector;  // JSON 형태로 저장된 벡터

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;  // 키워드와 점수를 문자열로 저장

    @Column(name = "model_version")
    private String modelVersion;

    /**
     * 벡터를 double 배열로 반환
     */
    @Transient
    public double[] getVectorArray() {
        if (vector == null || vector.trim().isEmpty()) {
            return new double[0];
        }

        try {
            // [1.0, 2.0, 3.0] 형태의 문자열을 파싱
            String cleanString = vector.replace("[", "").replace("]", "").trim();
            if (cleanString.isEmpty()) {
                return new double[0];
            }

            String[] parts = cleanString.split(",");
            double[] result = new double[parts.length];

            for (int i = 0; i < parts.length; i++) {
                result[i] = Double.parseDouble(parts[i].trim());
            }

            return result;
        } catch (Exception e) {
            return new double[0];
        }
    }

    /**
     * double 배열을 벡터 문자열로 설정
     */
    public void setVectorFromArray(double[] vectorArray) {
        if (vectorArray == null || vectorArray.length == 0) {
            this.vector = "[]";
            return;
        }

        // Arrays.toString()을 사용하는 더 간단한 방법
        this.vector = Arrays.toString(vectorArray);
    }

    /**
     * 벡터 문자열 직접 설정 (이미 Lombok @Setter로 생성됨)
     */
    public void setVector(String vector) {
        this.vector = vector;
    }

}
