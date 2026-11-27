package freshvend.backend.service;

import freshvend.backend.entity.Order;
import freshvend.backend.entity.OrderItem;
import freshvend.backend.entity.Dispense;
import freshvend.backend.entity.Slot;
import freshvend.backend.entity.StockLot;
import freshvend.backend.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import freshvend.backend.entity.Attempt;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class DispenseService {

    private final DispenseRepository dispenseRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SlotRepository slotRepository;
    private final StockLotRepository stockLotRepository;
    private final AttemptRepository attemptRepository;
    private final RefundService refundService;

    public DispenseService(
            DispenseRepository dispenseRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            SlotRepository slotRepository,
            StockLotRepository stockLotRepository,
            AttemptRepository attemptRepository,
            RefundService refundService) {

        this.dispenseRepository = dispenseRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.slotRepository = slotRepository;
        this.stockLotRepository = stockLotRepository;
        this.attemptRepository = attemptRepository;
        this.refundService = refundService;
    }

    @Transactional
    public List<Dispense> createCommands(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"));

        if (!"PAID".equals(order.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order has not been paid");
        }

        List<Dispense> existing =
                dispenseRepository.findByItem_Order_Id(orderId);

        if (!existing.isEmpty()) {
            return existing;
        }

        if (!"ONLINE".equals(order.getMachine().getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Machine unavailable");
        }

        Long machineId = order.getMachine().getId();

        List<OrderItem> items =
                orderItemRepository.findByOrder_Id(orderId);

        List<Dispense> commands = new ArrayList<>();

        Map<Long, Long> allocated = new HashMap<>();

        OffsetDateTime now = OffsetDateTime.now();

        for (OrderItem item : items) {

            List<StockLot> lots = new ArrayList<>();

            for (Slot slot : slotRepository
                    .findByMachine_IdOrderBySlotNoAsc(machineId)) {

                if (slot.getProduct() != null
                        && slot.getProduct().getId()
                        .equals(item.getProduct().getId())) {

                    lots.addAll(stockLotRepository
                        .findBySlot_IdAndQtyOnHandGreaterThanAndExpiresAtAfterOrderByReceivedAtAsc(
                                slot.getId(), 0, now));
                }
            }

            lots.sort(Comparator
                    .comparing(StockLot::getReceivedAt)
                    .thenComparing(StockLot::getId));

            int remaining = item.getQty();
            int unitNo = 1;

            for (StockLot lot : lots) {

                long reserved = dispenseRepository
                        .findByLot_Id(lot.getId())
                        .stream()
                        .filter(d -> "PENDING".equals(d.getStatus()))
                        .count();

                long available = lot.getQtyOnHand()
                        - reserved
                        - allocated.getOrDefault(lot.getId(), 0L);

                while (available > 0 && remaining > 0) {

                    Dispense dispense = new Dispense();

                    dispense.setItem(item);
                    dispense.setLot(lot);
                    dispense.setUnitNo(unitNo++);
                    dispense.setCommandKey(UUID.randomUUID());
                    dispense.setStatus("PENDING");

                    commands.add(dispense);

                    allocated.merge(lot.getId(), 1L, Long::sum);

                    available--;
                    remaining--;
                }

                if (remaining == 0) {
                    break;
                }
            }

            if (remaining > 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock for dispensing");
            }
        }

        return dispenseRepository.saveAll(commands);
    }

    @Transactional(readOnly = true)
    public List<Dispense> getPendingCommands(Long machineId) {

        if (!slotRepository
                .findByMachine_IdOrderBySlotNoAsc(machineId)
                .isEmpty()) {

            return dispenseRepository
                .findByItem_Order_Machine_IdAndStatusOrderByIdAsc(
                        machineId, "PENDING");
        }

        return List.of();
    }

    @Transactional
    public Dispense processDispenseEvent(
            Long machineId,
            UUID commandKey,
            UUID eventKey,
            String result) {

        if (commandKey == null || eventKey == null
                || (!"SUCCESS".equals(result)
                && !"FAILED".equals(result))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid dispense event");
        }

        Dispense dispense = dispenseRepository
                .findByCommandKey(commandKey)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Command not found"));

        Order order = dispense.getItem().getOrder();

        if (!order.getMachine().getId().equals(machineId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Invalid machine");
        }

        Optional<Attempt> existing =
                attemptRepository.findByEventKey(eventKey);

        if (existing.isPresent()) {

            if (!existing.get().getDispense().getId()
                    .equals(dispense.getId())
                    || !existing.get().getResult().equals(result)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Event key already used");
            }

            return dispense;
        }

        if (!"PENDING".equals(dispense.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Command already completed");
        }

        OffsetDateTime now = OffsetDateTime.now();

        Attempt attempt = new Attempt();

        attempt.setDispense(dispense);
        attempt.setEventKey(eventKey);
        attempt.setResult(result);
        attempt.setRecordedAt(now);

        int attemptNo = attemptRepository
                .findTopByDispense_IdOrderByAttemptNoDesc(
                        dispense.getId())
                .map(a -> a.getAttemptNo() + 1)
                .orElse(1);

        attempt.setAttemptNo(attemptNo);

        if ("SUCCESS".equals(result)) {

            StockLot lot = dispense.getLot();

            if (lot.getQtyOnHand() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock");
            }

            lot.setQtyOnHand(lot.getQtyOnHand() - 1);

            stockLotRepository.save(lot);

            dispense.setStatus("DELIVERED");
            dispense.setDeliveredAt(now);

        } else if (attemptNo >= 3) {

            dispense.setStatus("FAILED");
        }

        attemptRepository.save(attempt);
        dispenseRepository.saveAndFlush(dispense);

        if ("FAILED".equals(dispense.getStatus())) {
            refundService.createRefund(dispense);
        }

        List<Dispense> allDispenses =
                dispenseRepository.findByItem_Order_Id(order.getId());

        long expectedUnits = orderItemRepository
                .findByOrder_Id(order.getId())
                .stream()
                .mapToLong(OrderItem::getQty)
                .sum();

        boolean allDelivered = expectedUnits > 0
                && allDispenses.size() == expectedUnits
                && allDispenses.stream()
                        .allMatch(d -> "DELIVERED".equals(d.getStatus()));

        if (allDelivered) {

            order.setStatus("COMPLETED");

            orderRepository.save(order);
        }

        return dispense;
    }
}