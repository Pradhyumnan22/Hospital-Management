package com.hospital.bedmgmt.repo;

import com.hospital.bedmgmt.domain.Ward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WardRepository extends JpaRepository<Ward, Long> {
    Optional<Ward> findByCode(String code);
    List<Ward> findAllByOrderByCodeAsc();
}
