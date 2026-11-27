package freshvend.backend.controller;

import freshvend.backend.entity.Slot;
import freshvend.backend.service.SlotService;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SlotController {

    private final SlotService service;

    public SlotController(SlotService service) {
        this.service = service;
    }

    @GetMapping("/machines/{machineId}/slots")
    @Transactional(readOnly = true)
    public List<SlotResponse> getSlots(
            @PathVariable Long machineId) {

        return service.getSlotsByMachine(machineId)
                .stream()
                .map(SlotResponse::from)
                .toList();
    }

    @PutMapping("/slots/{slotId}/assignment")
    @Transactional
    public SlotResponse assignProduct(
            @PathVariable Long slotId,
            @RequestBody AssignmentRequest request) {

        Slot slot = service.assignProduct(
                slotId,
                request.productId(),
                request.capacity(),
                request.lowThreshold());

        return SlotResponse.from(slot);
    }

    public record AssignmentRequest(
            Long productId,
            Integer capacity,
            Integer lowThreshold) {
    }

    public record SlotResponse(
            Long id,
            Long machineId,
            Integer slotNo,
            Long productId,
            Integer capacity,
            Integer lowThreshold) {

        public static SlotResponse from(Slot slot) {
            return new SlotResponse(
                    slot.getId(),
                    slot.getMachine().getId(),
                    slot.getSlotNo(),
                    slot.getProduct() == null
                            ? null : slot.getProduct().getId(),
                    slot.getCapacity(),
                    slot.getLowThreshold());
        }
    }
}