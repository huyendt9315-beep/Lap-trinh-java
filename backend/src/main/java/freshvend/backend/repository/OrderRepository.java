package freshvend.backend.repository;

import freshvend.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    Optional<Order> findByReceiptNo(String receiptNo);

    List<Order> findByMachine_IdOrderByCreatedAtDesc(
            Long machineId);

    List<Order> findByStatus(String status);

    List<Order> findByStatusAndReservationUntilBefore(
            String status,
            OffsetDateTime now);

    boolean existsByReceiptNo(String receiptNo);
}