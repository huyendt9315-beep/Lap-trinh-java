package freshvend.backend.service;

import freshvend.backend.entity.Machine;
import freshvend.backend.entity.Telemetry;
import freshvend.backend.repository.MachineRepository;
import freshvend.backend.repository.TelemetryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final MachineRepository machineRepository;
    private final AlertService alertService;

    public TelemetryService(
            TelemetryRepository telemetryRepository,
            MachineRepository machineRepository,
            AlertService alertService) {

        this.telemetryRepository = telemetryRepository;
        this.machineRepository = machineRepository;
        this.alertService = alertService;
    }

    @Transactional
    public Telemetry receiveTelemetry(
            Long machineId,
            UUID eventKey,
            OffsetDateTime measuredAt,
            BigDecimal tempC,
            BigDecimal humidityPct,
            Boolean doorOpen,
            String deviceState) {

        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Machine not found"));

        if (eventKey == null || measuredAt == null
                || deviceState == null || deviceState.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid telemetry");
        }

        if (humidityPct != null &&
                (humidityPct.compareTo(BigDecimal.ZERO) < 0
                || humidityPct.compareTo(
                        BigDecimal.valueOf(100)) > 0)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid humidity");
        }

        Telemetry existing = telemetryRepository
                .findByEventKey(eventKey)
                .orElse(null);

        if (existing != null) {

            if (!existing.getMachine().getId().equals(machineId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Duplicate event key");
            }

            return existing;
        }

        Telemetry telemetry = new Telemetry();

        telemetry.setMachine(machine);
        telemetry.setEventKey(eventKey);
        telemetry.setMeasuredAt(measuredAt);
        telemetry.setReceivedAt(OffsetDateTime.now());
        telemetry.setTempC(tempC);
        telemetry.setHumidityPct(humidityPct);
        telemetry.setDoorOpen(doorOpen);
        telemetry.setDeviceState(deviceState);

        machine.setLastSeenAt(OffsetDateTime.now());
        machineRepository.save(machine);

        Telemetry saved = telemetryRepository.save(telemetry);

        alertService.checkTemperature(machine, tempC);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Telemetry> getTelemetry(Long machineId) {

        if (!machineRepository.existsById(machineId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Machine not found");
        }

        return telemetryRepository
                .findByMachine_IdOrderByMeasuredAtDesc(machineId);
    }
}