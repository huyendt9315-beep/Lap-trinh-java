package freshvend.backend.repository;

import freshvend.backend.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository
        extends JpaRepository<Refund, Long> {

    Optional<Refund> findByRequestKey(UUID requestKey);

    List<Refund> findByPayment_Id(Long paymentId);

    Optional<Refund> findByDispense_Id(Long dispenseId);

    boolean existsByDispense_Id(Long dispenseId);

    List<Refund> findByStatus(String status);
}