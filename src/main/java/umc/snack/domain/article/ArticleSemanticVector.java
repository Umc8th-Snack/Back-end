package umc.snack.domain.article;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.snack.domain.article.entity.Article;


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
    private Long id;

    // 'articles' 테이블의 'id'와 1:1 관계를 가집니다.
    @OneToOne
    @JoinColumn(name = "article_id", referencedColumnName = "article_id")
    private Article article;

    @Column(name = "vector", nullable = false, columnDefinition = "text")
    private String vector; // DB에 double[]를 문자열로 저장

    @Column(name = "keywords", nullable = false, columnDefinition = "text")
    private String keywords; // "[{\"word\":\"정부\",\"tfidf\":0.21}, ...]" 형태로 저장

    // JPA가 Double[]을 바로 지원하지 않으므로, getter/setter에서 변환 로직을 추가할 수 있습니다.
    // 여기서는 문자열로 저장하고, 서비스 계층에서 파싱하도록 단순화합니다.
    // 만약 DB가 배열을 지원한다면(예: PostgreSQL), @Type 어노테이션을 사용할 수 있습니다.

    // 이 예제에서는 DTO에서 받은 double[]을 String으로 변환해서 저장한다고 가정합니다.
    public void setVector(double[] vectorArray) {
        this.vector = java.util.Arrays.stream(vectorArray)
                .mapToObj(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
    }

    public double[] getVectorArray() {
        return java.util.Arrays.stream(this.vector.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
    }
}
