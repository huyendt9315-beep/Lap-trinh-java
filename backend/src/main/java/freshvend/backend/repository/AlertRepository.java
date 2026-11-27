package freshvend.backend.repository;

import freshvend.backend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository
        extends JpaRepository<Alert, Long> {

    List<Alert> findByMachine_IdOrderByOpenedAtDesc(
            Long machineId);

    List<Alert> findByMachine_IdAndResolvedAtIsNull(
            Long machineId);

    boolean existsByMachine_IdAndKindAndResolvedAtIsNull(
            Long machineId, String kind);
}