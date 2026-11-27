package freshvend.backend.service;

import freshvend.backend.entity.Alert;
import freshvend.backend.entity.Machine;
import freshvend.backend.repository.AlertRepository;
import freshvend.backend.repository.MachineRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final MachineRepository machineRepository;

    public AlertService(
            AlertRepository alertRepository,
            MachineRepository machineRepository) {

        this.alertRepository = alertRepository;
        this.machineRepository = machineRepository;
    }

    @Transactional
    public void checkTemperature(
            Machine machine, BigDecimal tempC) {

        if (tempC == null) {
            return;
        }

        if (tempC.compareTo(machine.getMinC()) < 0
                || tempC.compareTo(machine.getMaxC()) > 0) {

            createAlert(
                    machine,
                    "TEMPERATURE_OUT_OF_RANGE",
                    "CRITICAL");
        }
    }

    @Transactional
    public void createAlert(
            Machine machine,
            String kind,
            String severity) {

        boolean exists = alertRepository
                .existsByMachine_IdAndKindAndResolvedAtIsNull(
                        machine.getId(), kind);

        if (exists) {
            return;
        }

        Alert alert = new Alert();

        alert.setMachine(machine);
        alert.setKind(kind);
        alert.setSeverity(severity);
        alert.setOpenedAt(OffsetDateTime.now());

        alertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public List<Alert> getAlerts(Long machineId) {

        if (!machineRepository.existsById(machineId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Machine not found");
        }

        return alertRepository
                .findByMachine_IdOrderByOpenedAtDesc(machineId);
    }
    @Transactional
    public Alert resolveAlert(
            Long alertId,
            Long resolvedBy,
            String resolution) {

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Alert not found"));

        if (alert.getResolvedAt() != null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Alert already resolved");
        }

        if (resolvedBy == null || resolvedBy <= 0
                || resolution == null || resolution.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid resolution");
        }

        alert.setResolvedAt(OffsetDateTime.now());
        alert.setResolvedBy(resolvedBy);
        alert.setResolution(resolution);

        return alertRepository.save(alert);
    }
}