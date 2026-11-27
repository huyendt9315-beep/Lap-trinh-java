package freshvend.backend.controller;

import freshvend.backend.entity.Dispense;
import freshvend.backend.service.DispenseService;

import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@Profile("demo")
@RequestMapping("/api/v1")
public class DispenseController {

    private final DispenseService service;

    public DispenseController(DispenseService service) {
        this.service = service;
    }

    @PostMapping("/orders/{orderId}/dispenses")
    @Transactional
    public List<DispenseResponse> createCommands(
            @PathVariable Long orderId) {

        return service.createCommands(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/machines/{machineId}/commands")
    @Transactional(readOnly = true)
    public List<DispenseResponse> getCommands(
            @PathVariable Long machineId) {

        return service.getPendingCommands(machineId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/machines/{machineId}/dispense-events")
    @Transactional
    public DispenseResponse processEvent(
            @PathVariable Long machineId,
            @RequestBody DispenseEvent request) {

        Dispense dispense = service.processDispenseEvent(
                machineId,
                request.commandKey(),
                request.eventKey(),
                request.result());

        return toResponse(dispense);
    }

    private DispenseResponse toResponse(Dispense dispense) {

        return new DispenseResponse(
                dispense.getId(),
                dispense.getItem().getOrder().getId(),
                dispense.getItem().getProduct().getId(),
                dispense.getLot().getId(),
                dispense.getLot().getSlot().getId(),
                dispense.getUnitNo(),
                dispense.getCommandKey(),
                dispense.getStatus(),
                dispense.getDeliveredAt()
        );
    }

    public record DispenseEvent(
            UUID commandKey,
            UUID eventKey,
            String result) {
    }

    public record DispenseResponse(
            Long dispenseId,
            Long orderId,
            Long productId,
            Long lotId,
            Long slotId,
            Integer unitNo,
            UUID commandKey,
            String status,
            OffsetDateTime deliveredAt) {
    }
}