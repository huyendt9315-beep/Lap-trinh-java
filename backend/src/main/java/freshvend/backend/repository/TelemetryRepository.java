package freshvend.backend.repository;

import freshvend.backend.entity.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelemetryRepository
        extends JpaRepository<Telemetry, Long> {

    Optional<Telemetry> findByEventKey(UUID eventKey);

    boolean existsByEventKey(UUID eventKey);

    List<Telemetry> findByMachine_IdOrderByMeasuredAtDesc(
            Long machineId);

    Optional<Telemetry> findTopByMachine_IdOrderByMeasuredAtDesc(
            Long machineId);
}