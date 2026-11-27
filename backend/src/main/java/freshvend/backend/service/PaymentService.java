package freshvend.backend.service;

import freshvend.backend.entity.Order;
import freshvend.backend.entity.Payment;
import freshvend.backend.repository.OrderRepository;
import freshvend.backend.repository.OrderItemRepository;
import freshvend.backend.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {

        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public Payment createPayment(
            Long orderId,
            UUID requestKey,
            String method) {

        if (requestKey == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request key is required");
        }

        if (!"QR".equals(method)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported payment method");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"));

        Optional<Payment> existing = paymentRepository
                .findByOrder_IdAndRequestKey(
                        orderId, requestKey);

        if (existing.isPresent()) {
            return existing.get();
        }

        if (!"PENDING".equals(order.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid order status");
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (!order.getReservationUntil().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order has expired");
        }

        if (paymentRepository.existsByOrder_IdAndStatus(
                orderId, "PENDING")) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment already pending");
        }

        if (paymentRepository.existsByOrder_IdAndStatus(
                orderId, "PAID")) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order already paid");
        }

        Long amount = orderItemRepository
                .getOrderTotal(orderId);

        if (amount == null || amount <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment amount");
        }

        Payment payment = new Payment();

        payment.setOrder(order);
        payment.setRequestKey(requestKey);
        payment.setProviderRef("DEMO-" + UUID.randomUUID());
        payment.setMethod(method);
        payment.setAmountVnd(amount);
        payment.setStatus("PENDING");
        payment.setCreatedAt(now);

        return paymentRepository.save(payment);
    }

    public Payment getPaymentById(Long paymentId) {

        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"));
    }

    public List<Payment> getPaymentsByOrder(Long orderId) {

        return paymentRepository.findByOrder_Id(orderId);
    }
    @Transactional
    public Payment confirmPayment(String providerRef) {

        Payment payment = paymentRepository
                .findByProviderRef(providerRef)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"));

        if ("PAID".equals(payment.getStatus())) {
            return payment;
        }

        if (!"PENDING".equals(payment.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid payment status");
        }

        Order order = payment.getOrder();

        if (!"PENDING".equals(order.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid order status");
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (!order.getReservationUntil().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order has expired");
        }

        payment.setStatus("PAID");
        payment.setPaidAt(now);

        order.setStatus("PAID");

        orderRepository.save(order);

        return paymentRepository.save(payment);
    }
}