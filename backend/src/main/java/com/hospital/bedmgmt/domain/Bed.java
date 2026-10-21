package com.hospital.bedmgmt.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "bed")
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;

    @Column(name = "bed_code", nullable = false, length = 16)
    private String bedCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BedState state = BedState.AVAILABLE;

    @Column(name = "isolation_capable", nullable = false)
    private boolean isolationCapable;

    @Column(name = "has_ventilator", nullable = false)
    private boolean ventilator;

    @Column(name = "has_monitor", nullable = false)
    private boolean monitor;

    @Column(name = "has_oxygen", nullable = false)
    private boolean oxygen;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Ward getWard() { return ward; }
    public void setWard(Ward ward) { this.ward = ward; }
    public String getBedCode() { return bedCode; }
    public void setBedCode(String bedCode) { this.bedCode = bedCode; }
    public BedState getState() { return state; }
    public void setState(BedState state) { this.state = state; }
    public boolean isIsolationCapable() { return isolationCapable; }
    public void setIsolationCapable(boolean isolationCapable) { this.isolationCapable = isolationCapable; }
    public boolean isVentilator() { return ventilator; }
    public void setVentilator(boolean ventilator) { this.ventilator = ventilator; }
    public boolean isMonitor() { return monitor; }
    public void setMonitor(boolean monitor) { this.monitor = monitor; }
    public boolean isOxygen() { return oxygen; }
    public void setOxygen(boolean oxygen) { this.oxygen = oxygen; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
