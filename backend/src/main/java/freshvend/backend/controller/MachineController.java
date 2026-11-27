package freshvend.backend.controller;

import freshvend.backend.entity.Machine;
import freshvend.backend.service.MachineService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/machines")
public class MachineController {

    private final MachineService service;

    public MachineController(MachineService service) {
        this.service = service;
    }

    @GetMapping
    public List<Machine> getAllMachines() {
        return service.getAllMachines();
    }

    @PutMapping("/{machineId}/cold-chain")
    public Machine updateColdChain(
            @PathVariable Long machineId,
            @RequestBody ColdChainRequest request) {

        return service.updateColdChain(
                machineId,
                request.minC(),
                request.maxC());
    }

    public record ColdChainRequest(
            BigDecimal minC,
            BigDecimal maxC) {
    }
}