package umc.snack.repository.term;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.snack.domain.term.entity.Term;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {
    Optional<Term> findByWord(String word);
    List<Term> findAllByWordAndDefinition(String word, String definition);
}
