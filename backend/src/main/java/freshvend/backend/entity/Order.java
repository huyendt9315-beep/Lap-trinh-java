package freshvend.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity(name = "FreshVendOrder")
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(name = "location_at_sale", nullable = false)
    private String locationAtSale;

    @Column(name = "receipt_no", nullable = false, unique = true)
    private String receiptNo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "reservation_until", nullable = false)
    private OffsetDateTime reservationUntil;

    public Order() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public String getLocationAtSale() {
        return locationAtSale;
    }

    public void setLocationAtSale(String locationAtSale) {
        this.locationAtSale = locationAtSale;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getReservationUntil() {
        return reservationUntil;
    }

    public void setReservationUntil(OffsetDateTime reservationUntil) {
        this.reservationUntil = reservationUntil;
    }
}