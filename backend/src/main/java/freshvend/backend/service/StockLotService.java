package freshvend.backend.service;

import freshvend.backend.entity.StockLot;
import freshvend.backend.entity.Slot;
import freshvend.backend.repository.StockLotRepository;
import freshvend.backend.repository.SlotRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class StockLotService {

    private final StockLotRepository stockLotRepository;
    private final SlotRepository slotRepository;

    public StockLotService(
            StockLotRepository stockLotRepository,
            SlotRepository slotRepository) {

        this.stockLotRepository = stockLotRepository;
        this.slotRepository = slotRepository;
    }

    public List<StockLot> getLotsBySlot(Long slotId) {

        if (!slotRepository.existsById(slotId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Slot not found");
        }

        return stockLotRepository
                .findBySlot_IdOrderByReceivedAtAsc(slotId);
    }

    @Transactional
    public StockLot receiveStock(
            Long slotId,
            Long operatorId,
            Integer quantity,
            OffsetDateTime expiresAt) {

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Slot not found"));

        if (slot.getProduct() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Slot has no product");
        }

        if (!slot.getProduct().isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Product is inactive");
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (quantity == null || quantity <= 0
                || operatorId == null || operatorId <= 0
                || expiresAt == null
                || !expiresAt.isAfter(now)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid stock data");
        }

        Long currentStock =
                stockLotRepository.getTotalStock(slotId);

        if (currentStock + quantity > slot.getCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Slot capacity exceeded");
        }

        StockLot lot = new StockLot();

        lot.setSlot(slot);
        lot.setProduct(slot.getProduct());
        lot.setOperatorId(operatorId);
        lot.setReceivedAt(now);
        lot.setExpiresAt(expiresAt);
        lot.setQtyIn(quantity);
        lot.setQtyOnHand(quantity);

        return stockLotRepository.save(lot);
    }

    @Transactional
    public StockLot removeStock(Long lotId, Integer quantity) {

        StockLot lot = stockLotRepository.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Stock lot not found"));

        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid quantity");
        }

        if (quantity > lot.getQtyOnHand()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Insufficient stock");
        }

        lot.setQtyOnHand(lot.getQtyOnHand() - quantity);

        return stockLotRepository.save(lot);
    }
}