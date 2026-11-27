package freshvend.backend.repository;

import freshvend.backend.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findByMachine_IdOrderBySlotNoAsc(Long machineId);

    Optional<Slot> findByMachine_IdAndSlotNo(
            Long machineId, Integer slotNo);

    List<Slot> findByProduct_Id(Long productId);
}