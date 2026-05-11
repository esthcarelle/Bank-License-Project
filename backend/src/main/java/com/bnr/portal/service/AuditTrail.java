package com.bnr.portal.service;

import com.bnr.portal.entity.Application;
import com.bnr.portal.entity.AuditEntry;
import com.bnr.portal.entity.User;
import com.bnr.portal.repository.AuditEntryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditTrail {

    private final AuditEntryRepository rows;
    private final ObjectMapper json;

    public AuditTrail(AuditEntryRepository rows, ObjectMapper json) {
        this.rows = rows;
        this.json = json;
    }

    /**
     * Writes one immutable line to the audit table (never updated or deleted by app code).
     */
    @Transactional
    public void record(
            User whoDidIt,
            Application aboutThisApplication,
            String whatHappened,
            String stageBefore,
            String stageAfter,
            Map<String, Object> extraDetails
    ) {
        AuditEntry line = new AuditEntry();
        line.setActor(whoDidIt);
        line.setApplication(aboutThisApplication);
        line.setAction(whatHappened);
        line.setStateBefore(stageBefore);
        line.setStateAfter(stageAfter);
        if (extraDetails != null && !extraDetails.isEmpty()) {
            try {
                line.setDetailsJson(json.writeValueAsString(extraDetails));
            } catch (JsonProcessingException e) {
                line.setDetailsJson("{\"error\":\"could_not_serialize_details\"}");
            }
        }
        rows.save(line);
    }
}
