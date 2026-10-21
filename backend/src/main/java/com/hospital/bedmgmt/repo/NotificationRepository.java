package com.hospital.bedmgmt.repo;

import com.hospital.bedmgmt.domain.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findAllByOrderByCreatedAtDesc();
    boolean existsByTitleAndAcknowledgedFalse(String title);
}
