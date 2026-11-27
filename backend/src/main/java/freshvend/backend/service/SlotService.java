package freshvend.backend.service;

import freshvend.backend.entity.Slot;
import freshvend.backend.entity.Product;
import freshvend.backend.repository.SlotRepository;
import freshvend.backend.repository.ProductRepository;
import freshvend.backend.repository.MachineRepository;
import freshvend.backend.repository.StockLotRepository;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SlotService {

    private final SlotRepository slotRepository;
    private final ProductRepository productRepository;
    private final MachineRepository machineRepository;
    private final StockLotRepository stockLotRepository;

    public SlotService(
            SlotRepository slotRepository,
            ProductRepository productRepository,
            MachineRepository machineRepository,
            StockLotRepository stockLotRepository) {

        this.slotRepository = slotRepository;
        this.productRepository = productRepository;
        this.machineRepository = machineRepository;
        this.stockLotRepository = stockLotRepository;
    }

    public List<Slot> getSlotsByMachine(Long machineId) {

        if (!machineRepository.existsById(machineId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Machine not found");
        }

        return slotRepository
                .findByMachine_IdOrderBySlotNoAsc(machineId);
    }

    public Slot getSlotById(Long slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Slot not found"));
    }

    @Transactional
    public Slot assignProduct(
            Long slotId,
            Long productId,
            Integer capacity,
            Integer lowThreshold) {

        Slot slot = getSlotById(slotId);

        if (capacity == null || capacity <= 0
                || lowThreshold == null
                || lowThreshold < 0
                || lowThreshold > capacity
                || productId == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid slot data");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));

        if (!product.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Product is inactive");
        }

        Long currentStock =
                stockLotRepository.getTotalStock(slotId);

        if (currentStock > 0
                && (slot.getProduct() == null
                || !slot.getProduct().getId().equals(productId))) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot change product while stock remains");
        }

        if (capacity < currentStock) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Capacity is lower than current stock");
        }

        slot.setProduct(product);
        slot.setCapacity(capacity);
        slot.setLowThreshold(lowThreshold);

        return slotRepository.save(slot);
    }
}