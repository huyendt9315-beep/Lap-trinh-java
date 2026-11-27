package freshvend.backend.repository;

import freshvend.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    List<Payment> findByOrder_Id(Long orderId);

    Optional<Payment> findByOrder_IdAndRequestKey(
            Long orderId, UUID requestKey);

    Optional<Payment> findByProviderRef(String providerRef);

    List<Payment> findByStatus(String status);

    boolean existsByOrder_IdAndStatus(
            Long orderId, String status);
}