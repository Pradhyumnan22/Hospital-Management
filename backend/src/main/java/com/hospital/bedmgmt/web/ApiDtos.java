package com.hospital.bedmgmt.web;

import com.hospital.bedmgmt.domain.AdmissionStatus;
import com.hospital.bedmgmt.domain.BedState;
import com.hospital.bedmgmt.domain.Role;
import com.hospital.bedmgmt.domain.Sex;
import com.hospital.bedmgmt.domain.WardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 100) String password
    ) {
    }

    public record UserResponse(Long id, String username, String fullName, Role role, boolean enabled) {
    }

    public record CreateStaffRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 120) String fullName,
            @NotNull Role role
    ) {
    }

    public record UpdateStaffRequest(Boolean enabled, Role role) {
    }

    public record PatientRequest(
            @NotBlank @Size(max = 32) String medicalRecordNumber,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull Sex sex,
            @Size(max = 32) String phone
    ) {
    }

    public record PatientResponse(
            Long id,
            String medicalRecordNumber,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            Sex sex,
            String phone,
            Instant createdAt
    ) {
    }

    public record WardRequest(
            @NotBlank @Size(max = 16) String code,
            @NotBlank @Size(max = 120) String name,
            @NotNull WardType wardType,
            @NotNull Integer floorNumber
    ) {
    }

    public record WardResponse(Long id, String code, String name, WardType wardType, int floorNumber) {
    }

    public record BedRequest(
            @NotNull Long wardId,
            @NotBlank @Size(max = 16) String bedCode,
            boolean isolationCapable,
            boolean ventilator,
            boolean monitor,
            boolean oxygen
    ) {
    }

    public record BedStateRequest(@NotNull BedState state) {
    }

    public record BedResponse(
            Long id,
            Long wardId,
            String wardCode,
            String wardName,
            WardType wardType,
            String bedCode,
            BedState state,
            boolean isolationCapable,
            boolean ventilator,
            boolean monitor,
            boolean oxygen
    ) {
    }

    public record CriteriaRequest(
            @NotNull Long patientId,
            @NotBlank @Size(max = 255) String diagnosis,
            WardType requiredWardType,
            WardType preferredWardType,
            boolean isolationRequired,
            boolean ventilatorRequired,
            boolean monitorRequired,
            boolean oxygenRequired
    ) {
    }

    public record AllocateRequest(
            @NotNull Long patientId,
            @NotNull Long bedId,
            @NotBlank @Size(max = 255) String diagnosis,
            WardType requiredWardType,
            WardType preferredWardType,
            boolean isolationRequired,
            boolean ventilatorRequired,
            boolean monitorRequired,
            boolean oxygenRequired
    ) {
    }

    public record DischargeRequest(@Size(max = 500) String notes) {
    }

    public record AdmissionResponse(
            Long id,
            Long patientId,
            String patientName,
            String medicalRecordNumber,
            Long bedId,
            String bedCode,
            String wardCode,
            String wardName,
            AdmissionStatus status,
            String diagnosis,
            boolean isolationRequired,
            WardType requiredWardType,
            LocalDateTime admittedAt,
            LocalDateTime dischargedAt,
            String admittedBy,
            String dischargedBy,
            String dischargeNotes
    ) {
    }

    public record AuditResponse(
            Long id,
            String actor,
            String action,
            String entityType,
            String entityId,
            String details,
            Instant createdAt
    ) {
    }

    public record NotificationResponse(
            Long id,
            String severity,
            String title,
            String message,
            boolean acknowledged,
            Instant createdAt
    ) {
    }
}
