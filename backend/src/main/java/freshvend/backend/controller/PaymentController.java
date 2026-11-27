package freshvend.backend.controller;

import freshvend.backend.entity.Payment;
import freshvend.backend.service.PaymentService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/orders/{orderId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PaymentResponse createPayment(
            @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") UUID requestKey,
            @RequestBody PaymentRequest request) {

        Payment payment = service.createPayment(
                orderId,
                requestKey,
                request.method());

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getRequestKey(),
                payment.getProviderRef(),
                payment.getMethod(),
                payment.getAmountVnd(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getPaidAt()
        );
    }

    public record PaymentRequest(
            String method) {
    }

    public record PaymentResponse(
            Long paymentId,
            Long orderId,
            UUID requestKey,
            String providerRef,
            String method,
            Long amountVnd,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime paidAt) {
    }
}