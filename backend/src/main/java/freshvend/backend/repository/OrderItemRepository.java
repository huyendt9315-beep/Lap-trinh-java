package freshvend.backend.repository;

import freshvend.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_Id(Long orderId);

    List<OrderItem> findByProduct_Id(Long productId);

    @Query("""
        SELECT COALESCE(SUM(i.qty * i.unitPriceVnd), 0)
        FROM OrderItem i
        WHERE i.order.id = :orderId
        """)
    Long getOrderTotal(@Param("orderId") Long orderId);
}