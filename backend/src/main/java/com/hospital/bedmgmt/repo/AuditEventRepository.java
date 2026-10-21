package com.hospital.bedmgmt.repo;

import com.hospital.bedmgmt.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
