package com.hospital.bedmgmt.service;

import com.hospital.bedmgmt.domain.Admission;
import com.hospital.bedmgmt.domain.AdmissionStatus;
import com.hospital.bedmgmt.domain.AppNotification;
import com.hospital.bedmgmt.domain.Bed;
import com.hospital.bedmgmt.domain.BedState;
import com.hospital.bedmgmt.domain.BedStateMachine;
import com.hospital.bedmgmt.domain.Patient;
import com.hospital.bedmgmt.domain.UserAccount;
import com.hospital.bedmgmt.domain.Ward;
import com.hospital.bedmgmt.error.ApiException;
import com.hospital.bedmgmt.forecast.OccupancyCalculator;
import com.hospital.bedmgmt.forecast.OccupancyCalculator.BedSnapshot;
import com.hospital.bedmgmt.forecast.OccupancyCalculator.OccupancySnapshot;
import com.hospital.bedmgmt.forecast.OccupancyCalculator.WardOccupancy;
import com.hospital.bedmgmt.forecast.OccupancyForecaster;
import com.hospital.bedmgmt.forecast.OccupancyForecaster.DailyFlow;
import com.hospital.bedmgmt.forecast.OccupancyForecaster.Forecast;
import com.hospital.bedmgmt.recommendation.BedRecommendationEngine;
import com.hospital.bedmgmt.recommendation.RecommendationModels.BedCandidate;
import com.hospital.bedmgmt.recommendation.RecommendationModels.RecommendationCriteria;
import com.hospital.bedmgmt.recommendation.RecommendationModels.RecommendationResult;
import com.hospital.bedmgmt.repo.AdmissionRepository;
import com.hospital.bedmgmt.repo.BedRepository;
import com.hospital.bedmgmt.repo.NotificationRepository;
import com.hospital.bedmgmt.repo.PatientRepository;
import com.hospital.bedmgmt.repo.UserAccountRepository;
import com.hospital.bedmgmt.repo.WardRepository;
import com.hospital.bedmgmt.web.ApiDtos.AdmissionResponse;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HospitalServices {

    private final PatientRepository patients;
    private final WardRepository wards;
    private final BedRepository beds;
    private final AdmissionRepository admissions;
    private final UserAccountRepository users;
    private final NotificationRepository notifications;
    private final AuditService audit;
    private final PasswordEncoder passwordEncoder;
    private final BedRecommendationEngine engine = new BedRecommendationEngine();

    public HospitalServices(
            PatientRepository patients,
            WardRepository wards,
            BedRepository beds,
            AdmissionRepository admissions,
            UserAccountRepository users,
            NotificationRepository notifications,
            AuditService audit,
            PasswordEncoder passwordEncoder
    ) {
        this.patients = patients;
        this.wards = wards;
        this.beds = beds;
        this.admissions = admissions;
        this.users = users;
        this.notifications = notifications;
        this.audit = audit;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> listPatients() {
        return patients.findAllByOrderByLastNameAscFirstNameAsc().stream().map(this::toPatient).toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatient(Long id) {
        return toPatient(requirePatient(id));
    }

    @Transactional
    public PatientResponse createPatient(PatientRequest request, String actor) {
        if (patients.findByMedicalRecordNumber(request.medicalRecordNumber()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Medical record number already exists");
        }
        Patient patient = new Patient();
        applyPatient(patient, request);
        Patient saved = patients.save(patient);
        audit.record(actor, "CREATE_PATIENT", "patient", String.valueOf(saved.getId()), saved.getMedicalRecordNumber());
        return toPatient(saved);
    }

    @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request, String actor) {
        Patient patient = requirePatient(id);
        patients.findByMedicalRecordNumber(request.medicalRecordNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Medical record number already exists");
                });
        applyPatient(patient, request);
        audit.record(actor, "UPDATE_PATIENT", "patient", String.valueOf(id), request.medicalRecordNumber());
        return toPatient(patient);
    }

    @Transactional(readOnly = true)
    public List<WardResponse> listWards() {
        return wards.findAllByOrderByCodeAsc().stream().map(this::toWard).toList();
    }

    @Transactional
    public WardResponse createWard(WardRequest request, String actor) {
        validateFloor(request.floorNumber());
        if (wards.findByCode(request.code()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Ward code already exists");
        }
        Ward ward = new Ward();
        applyWard(ward, request);
        Ward saved = wards.save(ward);
        audit.record(actor, "CREATE_WARD", "ward", String.valueOf(saved.getId()), saved.getCode());
        return toWard(saved);
    }

    @Transactional
    public WardResponse updateWard(Long id, WardRequest request, String actor) {
        validateFloor(request.floorNumber());
        Ward ward = wards.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ward not found"));
        wards.findByCode(request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Ward code already exists");
                });
        applyWard(ward, request);
        audit.record(actor, "UPDATE_WARD", "ward", String.valueOf(id), request.code());
        return toWard(ward);
    }

    @Transactional(readOnly = true)
    public List<BedResponse> listBeds() {
        return beds.findAllWithWard().stream().map(this::toBed).toList();
    }

    @Transactional
    public BedResponse createBed(BedRequest request, String actor) {
        Ward ward = wards.findById(request.wardId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ward not found"));
        Bed bed = new Bed();
        bed.setWard(ward);
        bed.setBedCode(request.bedCode().trim());
        bed.setState(BedState.AVAILABLE);
        bed.setIsolationCapable(request.isolationCapable());
        bed.setVentilator(request.ventilator());
        bed.setMonitor(request.monitor());
        bed.setOxygen(request.oxygen());
        try {
            Bed saved = beds.saveAndFlush(bed);
            audit.record(actor, "CREATE_BED", "bed", String.valueOf(saved.getId()), ward.getCode() + " " + saved.getBedCode());
            return toBed(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "Bed code already exists in this ward");
        }
    }

    @Transactional
    public BedResponse changeBedState(Long id, BedStateRequest request, String actor) {
        Bed bed = beds.findByIdForUpdate(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bed not found"));
        if (!BedStateMachine.isManualTransition(bed.getState(), request.state())) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot change bed from " + bed.getState() + " to " + request.state());
        }
        bed.setState(request.state());
        audit.record(actor, "BED_STATE", "bed", String.valueOf(id), request.state().name());
        refreshAlerts();
        return toBed(bed);
    }

    @Transactional(readOnly = true)
    public List<AdmissionResponse> listAdmissions(AdmissionStatus status) {
        List<Admission> rows = status == null
                ? admissions.findAllByOrderByAdmittedAtDesc()
                : admissions.findByStatusOrderByAdmittedAtDesc(status);
        return rows.stream().map(this::toAdmission).toList();
    }

    @Transactional(readOnly = true)
    public List<AdmissionResponse> patientAdmissions(Long patientId) {
        requirePatient(patientId);
        return admissions.findByPatientIdOrderByAdmittedAtDesc(patientId).stream().map(this::toAdmission).toList();
    }

    @Transactional(readOnly = true)
    public RecommendationResult recommend(CriteriaRequest request) {
        requirePatient(request.patientId());
        if (request.diagnosis() == null || request.diagnosis().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Diagnosis is required");
        }
        return engine.recommend(criteria(request), candidates());
    }

    @Transactional
    public AdmissionResponse admit(AllocateRequest request, String actor) {
        Patient patient = requirePatient(request.patientId());
        if (admissions.existsByActivePatientKey(patient.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Patient already has an active admission");
        }
        Bed bed = beds.findByIdForUpdate(request.bedId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bed not found"));
        RecommendationCriteria criteria = criteria(request);
        BedCandidate candidate = toCandidate(bed, wardOccupancy(bed.getWard().getId()));
        RecommendationResult check = engine.recommend(criteria, List.of(candidate));
        if (check.candidates().isEmpty()) {
            String reason = check.exclusions().isEmpty() ? "Bed is not eligible" : check.exclusions().get(0).reason();
            throw new ApiException(HttpStatus.CONFLICT, reason);
        }
        bed.setState(BedState.OCCUPIED);
        Admission admission = new Admission();
        admission.setPatient(patient);
        admission.setBed(bed);
        admission.setStatus(AdmissionStatus.ACTIVE);
        admission.setDiagnosis(request.diagnosis().trim());
        admission.setIsolationRequired(request.isolationRequired());
        admission.setRequiredWardType(request.requiredWardType());
        admission.setAdmittedAt(java.time.LocalDateTime.now());
        admission.setAdmittedBy(actor);
        admission.setActivePatientKey(patient.getId());
        try {
            Admission saved = admissions.saveAndFlush(admission);
            audit.record(actor, "ADMIT", "admission", String.valueOf(saved.getId()), patient.getMedicalRecordNumber() + " -> " + bed.getBedCode());
            refreshAlerts();
            return toAdmission(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "Patient already has an active admission");
        }
    }

    @Transactional
    public AdmissionResponse discharge(Long admissionId, DischargeRequest request, String actor) {
        Admission admission = admissions.findById(admissionId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Admission not found"));
        if (admission.getStatus() != AdmissionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Admission is not active");
        }
        Bed bed = beds.findByIdForUpdate(admission.getBed().getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bed not found"));
        if (bed.getState() != BedState.OCCUPIED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only an occupied bed can be discharged");
        }
        bed.setState(BedState.CLEANING);
        admission.setStatus(AdmissionStatus.DISCHARGED);
        admission.setDischargedAt(java.time.LocalDateTime.now());
        admission.setDischargedBy(actor);
        admission.setDischargeNotes(request.notes());
        admission.setActivePatientKey(null);
        audit.record(actor, "DISCHARGE", "admission", String.valueOf(admissionId), admission.getPatient().getMedicalRecordNumber());
        refreshAlerts();
        return toAdmission(admission);
    }

    @Transactional
    public Map<String, Object> overview() {
        List<Bed> all = beds.findAllWithWard();
        OccupancySnapshot snapshot = OccupancyCalculator.summarize(all.stream()
                .map(bed -> new BedSnapshot(bed.getWard().getCode(), bed.getWard().getName(), bed.getState()))
                .toList());
        Map<Long, List<Bed>> byWard = new LinkedHashMap<>();
        for (Bed bed : all) {
            byWard.computeIfAbsent(bed.getWard().getId(), id -> new ArrayList<>()).add(bed);
        }
        List<WardOccupancy> wardRows = new ArrayList<>();
        byWard.values().stream()
                .sorted(Comparator.comparing(group -> group.get(0).getWard().getCode()))
                .forEach(group -> {
                    OccupancySnapshot wardSnapshot = OccupancyCalculator.summarize(group.stream()
                            .map(bed -> new BedSnapshot(bed.getWard().getCode(), bed.getWard().getName(), bed.getState()))
                            .toList());
                    Ward ward = group.get(0).getWard();
                    wardRows.add(new WardOccupancy(
                            ward.getCode(),
                            ward.getName(),
                            wardSnapshot.operationalBeds(),
                            wardSnapshot.occupiedBeds(),
                            wardSnapshot.availableBeds(),
                            wardSnapshot.occupancyPercent()
                    ));
                });
        refreshAlerts(snapshot.occupancyPercent());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("operationalBeds", snapshot.operationalBeds());
        body.put("occupiedBeds", snapshot.occupiedBeds());
        body.put("availableBeds", snapshot.availableBeds());
        body.put("cleaningBeds", snapshot.cleaningBeds());
        body.put("outOfServiceBeds", snapshot.outOfServiceBeds());
        body.put("occupancyPercent", snapshot.occupancyPercent());
        body.put("wards", wardRows);
        body.put("measured", true);
        return body;
    }

    @Transactional(readOnly = true)
    public Forecast forecast() {
        List<Bed> all = beds.findAllWithWard();
        OccupancySnapshot snapshot = OccupancyCalculator.summarize(all.stream()
                .map(bed -> new BedSnapshot(bed.getWard().getCode(), bed.getWard().getName(), bed.getState()))
                .toList());
        LocalDate today = LocalDate.now();
        return OccupancyForecaster.forecast(
                snapshot.operationalBeds(),
                snapshot.occupiedBeds(),
                history(today),
                today
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listNotifications() {
        return notifications.findAllByOrderByCreatedAtDesc().stream().map(this::toNotification).toList();
    }

    @Transactional
    public NotificationResponse acknowledge(Long id) {
        AppNotification notification = notifications.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setAcknowledged(true);
        return toNotification(notification);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listStaff() {
        return users.findAllByOrderByUsernameAsc().stream().map(this::toUser).toList();
    }

    @Transactional
    public UserResponse createStaff(CreateStaffRequest request, String actor) {
        if (users.findByUsername(request.username()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }
        UserAccount account = new UserAccount();
        account.setUsername(request.username().trim());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setFullName(request.fullName().trim());
        account.setRole(request.role());
        account.setEnabled(true);
        UserAccount saved = users.save(account);
        audit.record(actor, "CREATE_STAFF", "user", String.valueOf(saved.getId()), saved.getUsername());
        return toUser(saved);
    }

    @Transactional
    public UserResponse updateStaff(Long id, UpdateStaffRequest request, String actor) {
        UserAccount account = users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff member not found"));
        if (request.enabled() != null) {
            account.setEnabled(request.enabled());
        }
        if (request.role() != null) {
            account.setRole(request.role());
        }
        audit.record(actor, "UPDATE_STAFF", "user", String.valueOf(id), account.getUsername());
        return toUser(account);
    }

    private List<DailyFlow> history(LocalDate today) {
        LocalDate start = today.minusDays(13);
        Map<LocalDate, int[]> counts = new LinkedHashMap<>();
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            counts.put(day, new int[2]);
        }
        for (Admission admission : admissions.findAll()) {
            LocalDate admitted = admission.getAdmittedAt().toLocalDate();
            if (counts.containsKey(admitted)) {
                counts.get(admitted)[0]++;
            }
            if (admission.getDischargedAt() != null && counts.containsKey(admission.getDischargedAt().toLocalDate())) {
                counts.get(admission.getDischargedAt().toLocalDate())[1]++;
            }
        }
        return counts.entrySet().stream()
                .map(entry -> new DailyFlow(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private List<BedCandidate> candidates() {
        List<Bed> all = beds.findAllWithWard();
        Map<Long, Double> occupancy = new LinkedHashMap<>();
        Map<Long, List<Bed>> grouped = new LinkedHashMap<>();
        for (Bed bed : all) {
            grouped.computeIfAbsent(bed.getWard().getId(), id -> new ArrayList<>()).add(bed);
        }
        grouped.forEach((id, group) -> occupancy.put(id, OccupancyCalculator.summarize(group.stream()
                .map(bed -> new BedSnapshot(bed.getWard().getCode(), bed.getWard().getName(), bed.getState()))
                .toList()).occupancyPercent()));
        return all.stream().map(bed -> toCandidate(bed, occupancy.getOrDefault(bed.getWard().getId(), 0.0))).toList();
    }

    private double wardOccupancy(Long wardId) {
        List<BedSnapshot> snapshots = beds.findAllWithWard().stream()
                .filter(bed -> bed.getWard().getId().equals(wardId))
                .map(bed -> new BedSnapshot(bed.getWard().getCode(), bed.getWard().getName(), bed.getState()))
                .toList();
        return OccupancyCalculator.summarize(snapshots).occupancyPercent();
    }

    private BedCandidate toCandidate(Bed bed, double occupancy) {
        return new BedCandidate(
                bed.getId(),
                bed.getBedCode(),
                bed.getWard().getCode(),
                bed.getWard().getName(),
                bed.getWard().getWardType(),
                bed.getState(),
                bed.isIsolationCapable(),
                bed.isVentilator(),
                bed.isMonitor(),
                bed.isOxygen(),
                occupancy
        );
    }

    private RecommendationCriteria criteria(CriteriaRequest request) {
        return new RecommendationCriteria(
                request.requiredWardType(),
                request.preferredWardType(),
                request.isolationRequired(),
                request.ventilatorRequired(),
                request.monitorRequired(),
                request.oxygenRequired()
        );
    }

    private RecommendationCriteria criteria(AllocateRequest request) {
        return new RecommendationCriteria(
                request.requiredWardType(),
                request.preferredWardType(),
                request.isolationRequired(),
                request.ventilatorRequired(),
                request.monitorRequired(),
                request.oxygenRequired()
        );
    }

    private void refreshAlerts() {
        List<Bed> all = beds.findAllWithWard();
        double percent = OccupancyCalculator.summarize(all.stream()
                .map(bed -> new BedSnapshot(bed.getWard().getCode(), bed.getWard().getName(), bed.getState()))
                .toList()).occupancyPercent();
        refreshAlerts(percent);
    }

    private void refreshAlerts(double percent) {
        if (percent >= 90.0) {
            ensureAlert("CRITICAL", "Critical occupancy", "Measured occupancy is " + percent + "%. Review discharges and out-of-service beds.");
        } else if (percent >= 85.0) {
            ensureAlert("WARNING", "High occupancy", "Measured occupancy is " + percent + "%. Capacity is above the warning threshold.");
        }
    }

    private void ensureAlert(String severity, String title, String message) {
        if (notifications.existsByTitleAndAcknowledgedFalse(title)) {
            return;
        }
        AppNotification notification = new AppNotification();
        notification.setSeverity(severity);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setAcknowledged(false);
        notifications.save(notification);
    }

    private Patient requirePatient(Long id) {
        return patients.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Patient not found"));
    }

    private void applyPatient(Patient patient, PatientRequest request) {
        patient.setMedicalRecordNumber(request.medicalRecordNumber().trim());
        patient.setFirstName(request.firstName().trim());
        patient.setLastName(request.lastName().trim());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setSex(request.sex());
        patient.setPhone(request.phone() == null ? null : request.phone().trim());
    }

    private void applyWard(Ward ward, WardRequest request) {
        ward.setCode(request.code().trim().toUpperCase());
        ward.setName(request.name().trim());
        ward.setWardType(request.wardType());
        ward.setFloorNumber(request.floorNumber());
    }

    private void validateFloor(Integer floor) {
        if (floor < 0 || floor > 40) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Floor must be between 0 and 40");
        }
    }

    private PatientResponse toPatient(Patient patient) {
        return new PatientResponse(patient.getId(), patient.getMedicalRecordNumber(), patient.getFirstName(), patient.getLastName(), patient.getDateOfBirth(), patient.getSex(), patient.getPhone(), patient.getCreatedAt());
    }

    private WardResponse toWard(Ward ward) {
        return new WardResponse(ward.getId(), ward.getCode(), ward.getName(), ward.getWardType(), ward.getFloorNumber());
    }

    private BedResponse toBed(Bed bed) {
        return new BedResponse(bed.getId(), bed.getWard().getId(), bed.getWard().getCode(), bed.getWard().getName(), bed.getWard().getWardType(), bed.getBedCode(), bed.getState(), bed.isIsolationCapable(), bed.isVentilator(), bed.isMonitor(), bed.isOxygen());
    }

    private AdmissionResponse toAdmission(Admission admission) {
        return new AdmissionResponse(
                admission.getId(),
                admission.getPatient().getId(),
                admission.getPatient().displayName(),
                admission.getPatient().getMedicalRecordNumber(),
                admission.getBed().getId(),
                admission.getBed().getBedCode(),
                admission.getBed().getWard().getCode(),
                admission.getBed().getWard().getName(),
                admission.getStatus(),
                admission.getDiagnosis(),
                admission.isIsolationRequired(),
                admission.getRequiredWardType(),
                admission.getAdmittedAt(),
                admission.getDischargedAt(),
                admission.getAdmittedBy(),
                admission.getDischargedBy(),
                admission.getDischargeNotes()
        );
    }

    private UserResponse toUser(UserAccount account) {
        return new UserResponse(account.getId(), account.getUsername(), account.getFullName(), account.getRole(), account.isEnabled());
    }

    private NotificationResponse toNotification(AppNotification notification) {
        return new NotificationResponse(notification.getId(), notification.getSeverity(), notification.getTitle(), notification.getMessage(), notification.isAcknowledged(), notification.getCreatedAt());
    }
}
