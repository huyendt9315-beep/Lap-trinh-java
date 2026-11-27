package freshvend.backend.controller;

import freshvend.backend.entity.Alert;
import freshvend.backend.service.AlertService;

import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/machines/{machineId}/alerts")
    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts(
            @PathVariable Long machineId) {

        return alertService.getAlerts(machineId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AlertResponse toResponse(Alert alert) {

        return new AlertResponse(
                alert.getId(),
                alert.getMachine().getId(),
                alert.getSlotId(),
                alert.getLotId(),
                alert.getDispenseId(),
                alert.getKind(),
                alert.getSeverity(),
                alert.getOpenedAt(),
                alert.getResolvedAt(),
                alert.getResolvedBy(),
                alert.getResolution());
    }

    public record AlertResponse(
            Long alertId,
            Long machineId,
            Long slotId,
            Long lotId,
            Long dispenseId,
            String kind,
            String severity,
            OffsetDateTime openedAt,
            OffsetDateTime resolvedAt,
            Long resolvedBy,
            String resolution) {
    }
    @PostMapping("/alerts/{alertId}/resolution")
    @Transactional
    public AlertResponse resolveAlert(
            @PathVariable Long alertId,
            @RequestBody ResolutionRequest request) {

        Alert alert = alertService.resolveAlert(
                alertId,
                request.resolvedBy(),
                request.resolution());

        return toResponse(alert);
    }

    public record ResolutionRequest(
            Long resolvedBy,
            String resolution) {
    }
}