package freshvend.backend.service;

import freshvend.backend.entity.Dispense;
import freshvend.backend.entity.Payment;
import freshvend.backend.entity.Refund;
import freshvend.backend.repository.PaymentRepository;
import freshvend.backend.repository.RefundRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;

    public RefundService(
            RefundRepository refundRepository,
            PaymentRepository paymentRepository) {

        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Refund createRefund(Dispense dispense) {

        if (!"FAILED".equals(dispense.getStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Dispense has not failed");
        }

        Refund existing = refundRepository
                .findByDispense_Id(dispense.getId())
                .orElse(null);

        if (existing != null) {
            return existing;
        }

        Long orderId = dispense.getItem()
                .getOrder().getId();

        Payment payment = paymentRepository
                .findByOrder_Id(orderId)
                .stream()
                .filter(p -> "PAID".equals(p.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Paid payment not found"));

        Long amount = dispense.getItem().getUnitPriceVnd();

        long totalRefund = refundRepository
                .findByPayment_Id(payment.getId())
                .stream()
                .mapToLong(Refund::getAmountVnd)
                .sum();

        if (amount <= 0
                || totalRefund + amount > payment.getAmountVnd()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid refund amount");
        }

        Refund refund = new Refund();

        refund.setPayment(payment);
        refund.setDispense(dispense);
        refund.setRequestKey(UUID.randomUUID());
        refund.setAmountVnd(amount);
        refund.setStatus("PENDING");

        return refundRepository.save(refund);
    }
}