package freshvend.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String status;

    @Column(name = "min_c", nullable = false)
    private BigDecimal minC;

    @Column(name = "max_c", nullable = false)
    private BigDecimal maxC;

    @Column(name = "degraded_message", nullable = false)
    private String degradedMessage;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    public Machine() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getMinC() {
        return minC;
    }

    public void setMinC(BigDecimal minC) {
        this.minC = minC;
    }

    public BigDecimal getMaxC() {
        return maxC;
    }

    public void setMaxC(BigDecimal maxC) {
        this.maxC = maxC;
    }

    public String getDegradedMessage() {
        return degradedMessage;
    }

    public void setDegradedMessage(String degradedMessage) {
        this.degradedMessage = degradedMessage;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(OffsetDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}