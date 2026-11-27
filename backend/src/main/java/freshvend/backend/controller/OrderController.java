package freshvend.backend.controller;

import freshvend.backend.entity.Order;
import freshvend.backend.service.OrderService;
import freshvend.backend.service.OrderService.ItemRequest;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping("/machines/{machineId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public OrderResponse createOrder(
            @PathVariable Long machineId,
            @RequestBody CreateOrderRequest request) {

        Order order = service.createOrder(
                machineId, request.items());

        return toResponse(order);
    }

    @GetMapping("/orders/{orderId}")
    @Transactional(readOnly = true)
    public OrderResponse getOrder(
            @PathVariable Long orderId) {

        return toResponse(
                service.getOrderById(orderId));
    }

    private OrderResponse toResponse(Order order) {

        List<ItemResponse> items = service
                .getOrderItems(order.getId())
                .stream()
                .map(item -> new ItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQty(),
                        item.getUnitPriceVnd(),
                        item.getQty() * item.getUnitPriceVnd()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getMachine().getId(),
                order.getReceiptNo(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getReservationUntil(),
                service.getOrderTotal(order.getId()),
                items
        );
    }

    public record CreateOrderRequest(
            List<ItemRequest> items) {
    }

    public record ItemResponse(
            Long productId,
            String productName,
            Integer quantity,
            Long unitPriceVnd,
            Long subtotalVnd) {
    }

    public record OrderResponse(
            Long orderId,
            Long machineId,
            String receiptNo,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime reservationUntil,
            Long totalVnd,
            List<ItemResponse> items) {
    }
}