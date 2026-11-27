package freshvend.backend.repository;

import freshvend.backend.entity.StockLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface StockLotRepository
        extends JpaRepository<StockLot, Long> {

    List<StockLot> findBySlot_IdOrderByReceivedAtAsc(
            Long slotId);

    List<StockLot>
        findBySlot_IdAndQtyOnHandGreaterThanAndExpiresAtAfterOrderByReceivedAtAsc(
            Long slotId,
            Integer qty,
            OffsetDateTime now);

    List<StockLot>
        findByExpiresAtLessThanEqualAndQtyOnHandGreaterThan(
            OffsetDateTime now,
            Integer qty);

    @Query("""
        SELECT COALESCE(SUM(s.qtyOnHand), 0)
        FROM StockLot s
        WHERE s.slot.id = :slotId
        """)
    Long getTotalStock(@Param("slotId") Long slotId);
}