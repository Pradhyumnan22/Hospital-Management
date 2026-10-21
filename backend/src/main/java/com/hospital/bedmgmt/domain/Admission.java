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
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "admission")
public class Admission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bed_id", nullable = false)
    private Bed bed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AdmissionStatus status;

    @Column(nullable = false, length = 255)
    private String diagnosis;

    @Column(name = "isolation_required", nullable = false)
    private boolean isolationRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_ward_type", length = 32)
    private WardType requiredWardType;

    @Column(name = "admitted_at", nullable = false)
    private LocalDateTime admittedAt;

    @Column(name = "discharged_at")
    private LocalDateTime dischargedAt;

    @Column(name = "admitted_by", nullable = false, length = 64)
    private String admittedBy;

    @Column(name = "discharged_by", length = 64)
    private String dischargedBy;

    @Column(name = "discharge_notes", length = 500)
    private String dischargeNotes;

    @Column(name = "active_patient_key")
    private Long activePatientKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Bed getBed() { return bed; }
    public void setBed(Bed bed) { this.bed = bed; }
    public AdmissionStatus getStatus() { return status; }
    public void setStatus(AdmissionStatus status) { this.status = status; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public boolean isIsolationRequired() { return isolationRequired; }
    public void setIsolationRequired(boolean isolationRequired) { this.isolationRequired = isolationRequired; }
    public WardType getRequiredWardType() { return requiredWardType; }
    public void setRequiredWardType(WardType requiredWardType) { this.requiredWardType = requiredWardType; }
    public LocalDateTime getAdmittedAt() { return admittedAt; }
    public void setAdmittedAt(LocalDateTime admittedAt) { this.admittedAt = admittedAt; }
    public LocalDateTime getDischargedAt() { return dischargedAt; }
    public void setDischargedAt(LocalDateTime dischargedAt) { this.dischargedAt = dischargedAt; }
    public String getAdmittedBy() { return admittedBy; }
    public void setAdmittedBy(String admittedBy) { this.admittedBy = admittedBy; }
    public String getDischargedBy() { return dischargedBy; }
    public void setDischargedBy(String dischargedBy) { this.dischargedBy = dischargedBy; }
    public String getDischargeNotes() { return dischargeNotes; }
    public void setDischargeNotes(String dischargeNotes) { this.dischargeNotes = dischargeNotes; }
    public Long getActivePatientKey() { return activePatientKey; }
    public void setActivePatientKey(Long activePatientKey) { this.activePatientKey = activePatientKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
