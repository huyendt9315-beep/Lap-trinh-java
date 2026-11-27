package freshvend.backend.service;

import freshvend.backend.entity.Machine;
import freshvend.backend.repository.MachineRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MachineService {

    private final MachineRepository repository;

    public MachineService(MachineRepository repository) {
        this.repository = repository;
    }

    public List<Machine> getAllMachines() {
        return repository.findAll();
    }

    public Machine getMachineById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Machine not found"));
    }

    @Transactional
    public Machine updateColdChain(
            Long id,
            BigDecimal minC,
            BigDecimal maxC) {

        if (minC == null || maxC == null
                || minC.compareTo(maxC) >= 0
                || minC.compareTo(BigDecimal.valueOf(2)) < 0
                || maxC.compareTo(BigDecimal.valueOf(8)) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid temperature range");
        }

        Machine machine = getMachineById(id);

        machine.setMinC(minC);
        machine.setMaxC(maxC);

        return repository.save(machine);
    }
}