package freshvend.backend.service;

import freshvend.backend.entity.Slot;
import freshvend.backend.entity.Product;
import freshvend.backend.entity.StockLot;
import freshvend.backend.repository.SlotRepository;
import freshvend.backend.repository.StockLotRepository;
import freshvend.backend.repository.MachineRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CatalogService {

    private final SlotRepository slotRepository;
    private final StockLotRepository stockLotRepository;
    private final MachineRepository machineRepository;

    public CatalogService(
            SlotRepository slotRepository,
            StockLotRepository stockLotRepository,
            MachineRepository machineRepository) {

        this.slotRepository = slotRepository;
        this.stockLotRepository = stockLotRepository;
        this.machineRepository = machineRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogItem> getCatalog(Long machineId) {

        if (!machineRepository.existsById(machineId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Machine not found");
        }

        List<Slot> slots = slotRepository
                .findByMachine_IdOrderBySlotNoAsc(machineId);

        List<CatalogItem> catalog = new ArrayList<>();

        OffsetDateTime now = OffsetDateTime.now();

        for (Slot slot : slots) {

            Product product = slot.getProduct();

            if (product == null || !product.isActive()) {
                continue;
            }

            int quantity = stockLotRepository
                .findBySlot_IdAndQtyOnHandGreaterThanAndExpiresAtAfterOrderByReceivedAtAsc(
                        slot.getId(), 0, now)
                .stream()
                .mapToInt(StockLot::getQtyOnHand)
                .sum();

            catalog.add(new CatalogItem(
                    slot.getId(),
                    product.getId(),
                    product.getSku(),
                    product.getName(),
                    product.getImageUrl(),
                    product.getPriceVnd(),
                    quantity,
                    quantity > 0));
        }

        return catalog;
    }

    public record CatalogItem(
            Long slotId,
            Long productId,
            String sku,
            String name,
            String imageUrl,
            Long priceVnd,
            Integer quantity,
            boolean available) {
    }
}