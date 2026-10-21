package com.hospital.bedmgmt.web;

import com.hospital.bedmgmt.domain.AdmissionStatus;
import com.hospital.bedmgmt.forecast.OccupancyForecaster.Forecast;
import com.hospital.bedmgmt.recommendation.RecommendationModels.RecommendationResult;
import com.hospital.bedmgmt.service.HospitalServices;
import com.hospital.bedmgmt.web.ApiDtos.AdmissionResponse;
import com.hospital.bedmgmt.web.ApiDtos.AuditResponse;
import com.hospital.bedmgmt.web.ApiDtos.AllocateRequest;
import com.hospital.bedmgmt.web.ApiDtos.BedRequest;
import com.hospital.bedmgmt.web.ApiDtos.BedResponse;
import com.hospital.bedmgmt.web.ApiDtos.BedStateRequest;
import com.hospital.bedmgmt.web.ApiDtos.CreateStaffRequest;
import com.hospital.bedmgmt.web.ApiDtos.CriteriaRequest;
import com.hospital.bedmgmt.web.ApiDtos.DischargeRequest;
import com.hospital.bedmgmt.web.ApiDtos.NotificationResponse;
import com.hospital.bedmgmt.web.ApiDtos.PatientRequest;
import com.hospital.bedmgmt.web.ApiDtos.PatientResponse;
import com.hospital.bedmgmt.web.ApiDtos.UpdateStaffRequest;
import com.hospital.bedmgmt.web.ApiDtos.UserResponse;
import com.hospital.bedmgmt.web.ApiDtos.WardRequest;
import com.hospital.bedmgmt.web.ApiDtos.WardResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final HospitalServices services;

    public ApiController(HospitalServices services) {
        this.services = services;
    }

    @GetMapping("/patients")
    public List<PatientResponse> patients() {
        return services.listPatients();
    }

    @GetMapping("/patients/{id}")
    public PatientResponse patient(@PathVariable Long id) {
        return services.getPatient(id);
    }

    @GetMapping("/patients/{id}/admissions")
    public List<AdmissionResponse> patientAdmissions(@PathVariable Long id) {
        return services.patientAdmissions(id);
    }

    @PostMapping("/patients")
    @PreAuthorize("hasAnyRole('ADMIN','ADMISSION_STAFF')")
    public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody PatientRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(services.createPatient(request, authentication.getName()));
    }

    @DeleteMapping("/patients/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ADMISSION_STAFF')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePatient(@PathVariable Long id, Authentication authentication) {
        services.deletePatient(id, authentication.getName());
    }

    @PutMapping("/patients/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ADMISSION_STAFF')")
    public PatientResponse updatePatient(@PathVariable Long id, @Valid @RequestBody PatientRequest request, Authentication authentication) {
        return services.updatePatient(id, request, authentication.getName());
    }

    @GetMapping("/wards")
    public List<WardResponse> wards() {
        return services.listWards();
    }

    @PostMapping("/wards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WardResponse> createWard(@Valid @RequestBody WardRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(services.createWard(request, authentication.getName()));
    }

    @DeleteMapping("/wards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWard(@PathVariable Long id, Authentication authentication) {
        services.deleteWard(id, authentication.getName());
    }

    @PutMapping("/wards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public WardResponse updateWard(@PathVariable Long id, @Valid @RequestBody WardRequest request, Authentication authentication) {
        return services.updateWard(id, request, authentication.getName());
    }

    @GetMapping("/beds")
    public List<BedResponse> beds() {
        return services.listBeds();
    }

    @PostMapping("/beds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BedResponse> createBed(@Valid @RequestBody BedRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(services.createBed(request, authentication.getName()));
    }

    @DeleteMapping("/beds/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBed(@PathVariable Long id, Authentication authentication) {
        services.deleteBed(id, authentication.getName());
    }

    @PatchMapping("/beds/{id}/state")
    @PreAuthorize("hasRole('ADMIN')")
    public BedResponse changeBedState(@PathVariable Long id, @Valid @RequestBody BedStateRequest request, Authentication authentication) {
        return services.changeBedState(id, request, authentication.getName());
    }

    @GetMapping("/admissions")
    public List<AdmissionResponse> admissions(@RequestParam(required = false) AdmissionStatus status) {
        return services.listAdmissions(status);
    }

    @PostMapping("/admissions")
    @PreAuthorize("hasAnyRole('ADMIN','ADMISSION_STAFF')")
    public ResponseEntity<AdmissionResponse> admit(@Valid @RequestBody AllocateRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(services.admit(request, authentication.getName()));
    }

    @PostMapping("/admissions/{id}/discharge")
    @PreAuthorize("hasAnyRole('ADMIN','ADMISSION_STAFF')")
    public AdmissionResponse discharge(@PathVariable Long id, @Valid @RequestBody DischargeRequest request, Authentication authentication) {
        return services.discharge(id, request, authentication.getName());
    }

    @PostMapping("/recommendations")
    public RecommendationResult recommend(@Valid @RequestBody CriteriaRequest request) {
        return services.recommend(request);
    }

    @GetMapping("/analytics/overview")
    public Map<String, Object> overview() {
        return services.overview();
    }

    @GetMapping("/analytics/forecast")
    public Forecast forecast() {
        return services.forecast();
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditResponse> audit() {
        return services.listAudit();
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> notifications() {
        return services.listNotifications();
    }

    @PostMapping("/notifications/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN','ADMISSION_STAFF')")
    public NotificationResponse acknowledge(@PathVariable Long id) {
        return services.acknowledge(id);
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> staff() {
        return services.listStaff();
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createStaff(@Valid @RequestBody CreateStaffRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(services.createStaff(request, authentication.getName()));
    }

    @PatchMapping("/staff/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateStaff(@PathVariable Long id, @Valid @RequestBody UpdateStaffRequest request, Authentication authentication) {
        return services.updateStaff(id, request, authentication.getName());
    }
}
