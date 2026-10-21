package com.hospital.bedmgmt.repo;

import com.hospital.bedmgmt.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);
    List<Patient> findAllByOrderByLastNameAscFirstNameAsc();
}
