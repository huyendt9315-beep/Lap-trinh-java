package freshvend.backend.controller;

import freshvend.backend.entity.StockLot;
import freshvend.backend.service.StockLotService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StockLotController {

    private final StockLotService service;

    public StockLotController(StockLotService service) {
        this.service = service;
    }

    @GetMapping("/slots/{slotId}/lots")
    @Transactional(readOnly = true)
    public List<StockLotResponse> getLots(
            @PathVariable Long slotId) {

        return service.getLotsBySlot(slotId)
                .stream()
                .map(StockLotResponse::from)
                .toList();
    }

    @PostMapping("/slots/{slotId}/lots")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public StockLotResponse receiveStock(
            @PathVariable Long slotId,
            @RequestBody ReceiveRequest request) {

        StockLot lot = service.receiveStock(
                slotId,
                request.operatorId(),
                request.quantity(),
                request.expiresAt());

        return StockLotResponse.from(lot);
    }

    @PostMapping("/lots/{lotId}/removals")
    @Transactional
    public StockLotResponse removeStock(
            @PathVariable Long lotId,
            @RequestBody RemovalRequest request) {

        StockLot lot = service.removeStock(
                lotId,
                request.quantity());

        return StockLotResponse.from(lot);
    }

    public record ReceiveRequest(
            Long operatorId,
            Integer quantity,
            OffsetDateTime expiresAt) {
    }

    public record RemovalRequest(
            Integer quantity) {
    }

    public record StockLotResponse(
            Long id,
            Long slotId,
            Long productId,
            Long operatorId,
            OffsetDateTime receivedAt,
            OffsetDateTime expiresAt,
            Integer qtyIn,
            Integer qtyOnHand) {

        public static StockLotResponse from(StockLot lot) {
            return new StockLotResponse(
                    lot.getId(),
                    lot.getSlot().getId(),
                    lot.getProduct().getId(),
                    lot.getOperatorId(),
                    lot.getReceivedAt(),
                    lot.getExpiresAt(),
                    lot.getQtyIn(),
                    lot.getQtyOnHand());
        }
    }
}