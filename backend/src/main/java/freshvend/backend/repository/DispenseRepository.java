package freshvend.backend.repository;

import freshvend.backend.entity.Dispense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispenseRepository
        extends JpaRepository<Dispense, Long> {

    Optional<Dispense> findByCommandKey(UUID commandKey);

    List<Dispense> findByItem_Id(Long itemId);

    List<Dispense> findByItem_Order_Id(Long orderId);

    List<Dispense> findByItem_Order_Machine_IdAndStatusOrderByIdAsc(
            Long machineId, String status);

    List<Dispense> findByLot_Id(Long lotId);

    List<Dispense> findByStatus(String status);

    boolean existsByItem_IdAndUnitNo(
            Long itemId, Integer unitNo);
}