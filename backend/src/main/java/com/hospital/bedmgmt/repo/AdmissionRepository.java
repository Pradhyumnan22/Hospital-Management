package com.hospital.bedmgmt.repo;

import com.hospital.bedmgmt.domain.Admission;
import com.hospital.bedmgmt.domain.AdmissionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {
    boolean existsByActivePatientKey(Long activePatientKey);

    @EntityGraph(attributePaths = {"patient", "bed", "bed.ward"})
    List<Admission> findByStatusOrderByAdmittedAtDesc(AdmissionStatus status);

    @EntityGraph(attributePaths = {"patient", "bed", "bed.ward"})
    List<Admission> findByPatientIdOrderByAdmittedAtDesc(Long patientId);

    @EntityGraph(attributePaths = {"patient", "bed", "bed.ward"})
    List<Admission> findAllByOrderByAdmittedAtDesc();
}
