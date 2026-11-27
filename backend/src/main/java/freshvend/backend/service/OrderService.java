package freshvend.backend.service;

import freshvend.backend.entity.Order;
import freshvend.backend.entity.OrderItem;
import freshvend.backend.entity.Machine;
import freshvend.backend.entity.Product;
import freshvend.backend.entity.Slot;
import freshvend.backend.entity.StockLot;
import freshvend.backend.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MachineRepository machineRepository;
    private final ProductRepository productRepository;
    private final SlotRepository slotRepository;
    private final StockLotRepository stockLotRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            MachineRepository machineRepository,
            ProductRepository productRepository,
            SlotRepository slotRepository,
            StockLotRepository stockLotRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.machineRepository = machineRepository;
        this.productRepository = productRepository;
        this.slotRepository = slotRepository;
        this.stockLotRepository = stockLotRepository;
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found"));
    }

    @Transactional
    public Order createOrder(
            Long machineId,
            List<ItemRequest> items) {

        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Machine not found"));

        if (!"ONLINE".equals(machine.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Machine unavailable");
        }

        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Order is empty");
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();

        for (ItemRequest item : items) {

            if (item == null || item.productId() == null
                    || item.quantity() == null
                    || item.quantity() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid order item");
            }

            quantities.merge(
                    item.productId(),
                    item.quantity(),
                    Integer::sum);
        }

        OffsetDateTime now = OffsetDateTime.now();

        List<OrderItem> orderItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {

            Product product = productRepository
                    .findById(entry.getKey())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Product not found"));

            if (!product.isActive()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Product is inactive");
            }

            long available = 0;

            for (Slot slot : slotRepository
                    .findByMachine_IdOrderBySlotNoAsc(machineId)) {

                if (slot.getProduct() != null
                        && slot.getProduct().getId()
                        .equals(product.getId())) {

                    available += stockLotRepository
                            .findBySlot_IdAndQtyOnHandGreaterThanAndExpiresAtAfterOrderByReceivedAtAsc(
                                    slot.getId(), 0, now)
                            .stream()
                            .mapToLong(StockLot::getQtyOnHand)
                            .sum();
                }
            }

            if (entry.getValue() > available) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Insufficient stock");
            }

            OrderItem orderItem = new OrderItem();

            orderItem.setProduct(product);
            orderItem.setQty(entry.getValue());
            orderItem.setUnitPriceVnd(product.getPriceVnd());

            orderItems.add(orderItem);
        }

        Order order = new Order();

        order.setMachine(machine);
        order.setLocationAtSale(machine.getLocation());
        order.setReceiptNo("FV-" + UUID.randomUUID());
        order.setCreatedAt(now);
        order.setStatus("PENDING");
        order.setReservationUntil(now.plusMinutes(5));

        order = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(order);
            orderItemRepository.save(item);
        }

        return order;
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrder_Id(orderId);
    }

    public Long getOrderTotal(Long orderId) {
        return orderItemRepository.getOrderTotal(orderId);
    }

    public record ItemRequest(
            Long productId,
            Integer quantity) {
    }
}