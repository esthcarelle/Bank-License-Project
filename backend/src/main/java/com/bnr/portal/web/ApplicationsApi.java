package com.bnr.portal.web;

import com.bnr.portal.domain.CaseMove;
import com.bnr.portal.repository.AuditEntryRepository;
import com.bnr.portal.repository.ApplicationDocumentRepository;
import com.bnr.portal.security.SignedInUser;
import com.bnr.portal.service.ApplicationRecords;
import com.bnr.portal.service.DocumentUploads;
import com.bnr.portal.service.WorkflowRunner;
import com.bnr.portal.service.api.InvalidOperationException;
import com.bnr.portal.web.dto.ApplicationResponse;
import com.bnr.portal.web.dto.ApplicationSummaryResponse;
import com.bnr.portal.web.dto.CreateApplicationRequest;
import com.bnr.portal.web.dto.WorkflowChangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationsApi {

    private final ApplicationRecords applicationRecords;
    private final WorkflowRunner workflowRunner;
    private final DocumentUploads documentUploads;
    private final AuditEntryRepository auditLines;
    private final ApplicationDocumentRepository documentRows;
    private final ApplicationPayloads payloads;

    public ApplicationsApi(
            ApplicationRecords applicationRecords,
            WorkflowRunner workflowRunner,
            DocumentUploads documentUploads,
            AuditEntryRepository auditLines,
            ApplicationDocumentRepository documentRows,
            ApplicationPayloads payloads) {
        this.applicationRecords = applicationRecords;
        this.workflowRunner = workflowRunner;
        this.documentUploads = documentUploads;
        this.auditLines = auditLines;
        this.documentRows = documentRows;
        this.payloads = payloads;
    }

    @GetMapping
    public List<ApplicationSummaryResponse> listMineOrEverything(@AuthenticationPrincipal SignedInUser who) {
        return applicationRecords.everythingVisibleTo(who).stream()
                .map(row -> new ApplicationSummaryResponse(
                        row.getId(),
                        row.getInstitutionName(),
                        row.getState(),
                        row.getVersion(),
                        row.getApplicant().getEmail(),
                        row.getCreatedAt()
                ))
                .toList();
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> openNewCase(
            @Valid @RequestBody CreateApplicationRequest body,
            @AuthenticationPrincipal SignedInUser who
    ) {
        var saved = applicationRecords.registerNew(body.institutionName(), who);
        return ResponseEntity.ok(envelope(saved.getId(), who));
    }

    @GetMapping("/{id}")
    public ApplicationResponse openOne(@PathVariable long id, @AuthenticationPrincipal SignedInUser who) {
        return envelope(id, who);
    }

    @PostMapping("/{id}/workflow")
    public ApplicationResponse pushWorkflowForward(
            @PathVariable long id,
            @Valid @RequestBody WorkflowChangeRequest body,
            @AuthenticationPrincipal SignedInUser who
    ) {
        CaseMove move;
        try {
            move = CaseMove.valueOf(body.action().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOperationException("Unknown action: " + body.action());
        }
        workflowRunner.runTransition(
                id,
                move,
                who,
                body.expectedVersion(),
                body.rejectionReason()
        );
        return envelope(id, who);
    }

    @PostMapping("/{id}/documents")
    public ApplicationResponse attachFile(
            @PathVariable long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal SignedInUser who
    ) throws java.io.IOException {
        documentUploads.saveIncomingFile(id, file, who);
        return envelope(id, who);
    }

    private ApplicationResponse envelope(long id, SignedInUser who) {
        var row = applicationRecords.fetchOneIfAllowed(id, who);
        var audit = auditLines.findForApplicationWithActor(row);
        var files = documentRows.findByApplicationWithUploader(row);
        return payloads.packForBrowser(row, audit, files);
    }
}
