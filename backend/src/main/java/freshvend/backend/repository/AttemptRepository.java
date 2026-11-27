package freshvend.backend.repository;

import freshvend.backend.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptRepository
        extends JpaRepository<Attempt, Long> {

    Optional<Attempt> findByEventKey(UUID eventKey);

    boolean existsByEventKey(UUID eventKey);

    List<Attempt> findByDispense_IdOrderByAttemptNoAsc(
            Long dispenseId);

    Optional<Attempt> findTopByDispense_IdOrderByAttemptNoDesc(
            Long dispenseId);

    long countByDispense_Id(Long dispenseId);

    List<Attempt> findByResult(String result);
}