package com.hospital.bedmgmt.service;

import com.hospital.bedmgmt.domain.AuditEvent;
import com.hospital.bedmgmt.repo.AuditEventRepository;
import com.hospital.bedmgmt.web.ApiDtos.AuditResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository events;

    public AuditService(AuditEventRepository events) {
        this.events = events;
    }

    public void record(String actor, String action, String entityType, String entityId, String details) {
        AuditEvent event = new AuditEvent();
        event.setActor(actor);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setDetails(details);
        events.save(event);
    }

    public List<AuditResponse> latest() {
        return events.findTop200ByOrderByCreatedAtDesc().stream()
                .map(event -> new AuditResponse(
                        event.getId(),
                        event.getActor(),
                        event.getAction(),
                        event.getEntityType(),
                        event.getEntityId(),
                        event.getDetails(),
                        event.getCreatedAt()
                ))
                .toList();
    }
}
