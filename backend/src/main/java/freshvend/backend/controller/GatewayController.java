package freshvend.backend.controller;

import freshvend.backend.entity.Payment;
import freshvend.backend.service.PaymentService;

import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@RestController
@Profile("demo")
@RequestMapping("/api/v1/gateway")
public class GatewayController {

    private final PaymentService service;

    public GatewayController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/payment-events")
    @Transactional
    public GatewayResponse confirmPayment(
            @RequestBody PaymentEvent request) {

        Payment payment = service.confirmPayment(
                request.providerRef());

        return new GatewayResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getProviderRef(),
                payment.getAmountVnd(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }

    public record PaymentEvent(
            String providerRef) {
    }

    public record GatewayResponse(
            Long paymentId,
            Long orderId,
            String providerRef,
            Long amountVnd,
            String status,
            OffsetDateTime paidAt) {
    }
}