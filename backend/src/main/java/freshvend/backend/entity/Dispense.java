package freshvend.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dispenses")
public class Dispense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private OrderItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private StockLot lot;

    @Column(name = "unit_no", nullable = false)
    private Integer unitNo;

    @Column(name = "command_key", nullable = false, unique = true)
    private UUID commandKey;

    @Column(nullable = false)
    private String status;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    public Dispense() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderItem getItem() {
        return item;
    }

    public void setItem(OrderItem item) {
        this.item = item;
    }

    public StockLot getLot() {
        return lot;
    }

    public void setLot(StockLot lot) {
        this.lot = lot;
    }

    public Integer getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(Integer unitNo) {
        this.unitNo = unitNo;
    }

    public UUID getCommandKey() {
        return commandKey;
    }

    public void setCommandKey(UUID commandKey) {
        this.commandKey = commandKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(OffsetDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}