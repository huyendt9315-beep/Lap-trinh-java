package freshvend.backend.controller;

import freshvend.backend.entity.Telemetry;
import freshvend.backend.service.TelemetryService;

import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/machines")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(
            TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/{machineId}/telemetry")
    @Transactional
    public TelemetryResponse receiveTelemetry(
            @PathVariable Long machineId,
            @RequestBody TelemetryRequest request) {

        Telemetry telemetry =
                telemetryService.receiveTelemetry(
                        machineId,
                        request.eventKey(),
                        request.measuredAt(),
                        request.tempC(),
                        request.humidityPct(),
                        request.doorOpen(),
                        request.deviceState());

        return toResponse(telemetry);
    }

    @GetMapping("/{machineId}/telemetry")
    @Transactional(readOnly = true)
    public List<TelemetryResponse> getTelemetry(
            @PathVariable Long machineId) {

        return telemetryService.getTelemetry(machineId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TelemetryResponse toResponse(Telemetry t) {

        return new TelemetryResponse(
                t.getId(),
                t.getMachine().getId(),
                t.getEventKey(),
                t.getMeasuredAt(),
                t.getReceivedAt(),
                t.getTempC(),
                t.getHumidityPct(),
                t.getDoorOpen(),
                t.getDeviceState());
    }

    public record TelemetryRequest(
            UUID eventKey,
            OffsetDateTime measuredAt,
            BigDecimal tempC,
            BigDecimal humidityPct,
            Boolean doorOpen,
            String deviceState) {
    }

    public record TelemetryResponse(
            Long id,
            Long machineId,
            UUID eventKey,
            OffsetDateTime measuredAt,
            OffsetDateTime receivedAt,
            BigDecimal tempC,
            BigDecimal humidityPct,
            Boolean doorOpen,
            String deviceState) {
    }
}