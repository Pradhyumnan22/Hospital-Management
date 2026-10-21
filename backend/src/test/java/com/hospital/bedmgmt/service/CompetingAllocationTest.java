package com.hospital.bedmgmt.service;

import com.hospital.bedmgmt.domain.Bed;
import com.hospital.bedmgmt.domain.BedState;
import com.hospital.bedmgmt.domain.Patient;
import com.hospital.bedmgmt.domain.Sex;
import com.hospital.bedmgmt.domain.Ward;
import com.hospital.bedmgmt.domain.WardType;
import com.hospital.bedmgmt.error.ApiException;
import com.hospital.bedmgmt.repo.BedRepository;
import com.hospital.bedmgmt.repo.PatientRepository;
import com.hospital.bedmgmt.repo.WardRepository;
import com.hospital.bedmgmt.web.ApiDtos.AllocateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class CompetingAllocationTest {

    @Autowired
    private HospitalServices services;

    @Autowired
    private WardRepository wards;

    @Autowired
    private BedRepository beds;

    @Autowired
    private PatientRepository patients;

    @Test
    void onlyOneOfTwoConcurrentAdmissionsGetsTheBed() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Ward ward = new Ward();
        ward.setCode("Z" + suffix.substring(0, 4));
        ward.setName("Race ward");
        ward.setWardType(WardType.GENERAL);
        ward.setFloorNumber(1);
        Ward savedWard = wards.save(ward);

        Bed bed = new Bed();
        bed.setWard(savedWard);
        bed.setBedCode("R-1");
        bed.setState(BedState.AVAILABLE);
        bed.setOxygen(true);
        Bed savedBed = beds.save(bed);

        Patient first = patient("MRN-R1-" + suffix);
        Patient second = patient("MRN-R2-" + suffix);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        try {
            Future<?> one = pool.submit(() -> attempt(savedBed.getId(), first.getId(), ready, start, success, conflict));
            Future<?> two = pool.submit(() -> attempt(savedBed.getId(), second.getId(), ready, start, success, conflict));
            ready.await();
            start.countDown();
            one.get();
            two.get();
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, success.get());
        assertEquals(1, conflict.get());
    }

    private void attempt(Long bedId, Long patientId, CountDownLatch ready, CountDownLatch start, AtomicInteger success, AtomicInteger conflict) {
        ready.countDown();
        try {
            start.await();
            services.admit(new AllocateRequest(patientId, bedId, "Synthetic race", WardType.GENERAL, WardType.GENERAL, false, false, false, true), "race-test");
            success.incrementAndGet();
        } catch (ApiException ex) {
            if (ex.getStatus().value() == 409) {
                conflict.incrementAndGet();
            } else {
                throw ex;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private Patient patient(String mrn) {
        Patient patient = new Patient();
        patient.setMedicalRecordNumber(mrn);
        patient.setFirstName("Race");
        patient.setLastName("Patient");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setSex(Sex.UNKNOWN);
        return patients.save(patient);
    }
}
