package com.bnr.portal.service;

import com.bnr.portal.config.PortalSettings;
import com.bnr.portal.domain.ApplicationStage;
import com.bnr.portal.domain.UserRole;
import com.bnr.portal.entity.Application;
import com.bnr.portal.entity.ApplicationDocument;
import com.bnr.portal.entity.User;
import com.bnr.portal.repository.ApplicationDocumentRepository;
import com.bnr.portal.repository.UserRepository;
import com.bnr.portal.security.SignedInUser;
import com.bnr.portal.service.api.InvalidOperationException;
import com.bnr.portal.service.api.PermissionDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentUploads {

    /** 5MB limit we tell applicants in the UI. */
    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private final PortalSettings portalSettings;
    private final ApplicationDocumentRepository documents;
    private final UserRepository users;
    private final AuditTrail auditTrail;
    private final ApplicationRecords applicationRecords;

    public DocumentUploads(
            PortalSettings portalSettings,
            ApplicationDocumentRepository documents,
            UserRepository users,
            AuditTrail auditTrail,
            ApplicationRecords applicationRecords) {
        this.portalSettings = portalSettings;
        this.documents = documents;
        this.users = users;
        this.auditTrail = auditTrail;
        this.applicationRecords = applicationRecords;
    }

    @Transactional
    public ApplicationDocument saveIncomingFile(
            long applicationId,
            MultipartFile incoming,
            SignedInUser caller
    ) throws IOException {
        Application row = applicationRecords.fetchOneIfAllowed(applicationId, caller);

        if (caller.getRole() != UserRole.APPLICANT
                || !row.getApplicant().getId().equals(caller.getId())) {
            throw new PermissionDeniedException("Only the applicant may upload documents for this application");
        }

        ApplicationStage stage = row.getState();
        if (stage != ApplicationStage.SUBMITTED && stage != ApplicationStage.ADDITIONAL_INFO_REQUESTED) {
            throw new PermissionDeniedException(
                    "Documents can only be uploaded while submitted or when additional information is requested");
        }

        if (incoming == null || incoming.isEmpty()) {
            throw new InvalidOperationException("File is required");
        }
        if (incoming.getSize() > MAX_FILE_BYTES) {
            throw new InvalidOperationException("Maximum file size is 5MB");
        }

        User uploader = users.findById(caller.getId()).orElseThrow();

        int nextRevision = documents.maxRevisionByApplication(row) + 1;

        String originalName = incoming.getOriginalFilename() != null ? incoming.getOriginalFilename() : "upload";
        Path root = Path.of(portalSettings.getFiles().getUploadDir()).toAbsolutePath().normalize();
        Path folder = root.resolve(String.valueOf(applicationId)).resolve(String.valueOf(nextRevision));
        Files.createDirectories(folder);
        String storedFileName = UUID.randomUUID() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path onDisk = folder.resolve(storedFileName);
        Files.copy(incoming.getInputStream(), onDisk, StandardCopyOption.REPLACE_EXISTING);

        ApplicationDocument meta = new ApplicationDocument();
        meta.setApplication(row);
        meta.setRevision(nextRevision);
        meta.setOriginalFilename(originalName);
        meta.setSizeBytes(incoming.getSize());
        meta.setContentType(incoming.getContentType() != null ? incoming.getContentType() : "application/octet-stream");
        meta.setStoragePath(onDisk.toString());
        meta.setUploadedBy(uploader);
        meta.setUploadedAt(Instant.now());
        ApplicationDocument savedMeta = documents.save(meta);

        String before = stage.name();
        auditTrail.record(uploader, row, "DOCUMENT_UPLOADED",
                before, before,
                Map.of(
                        "filename", originalName,
                        "sizeBytes", incoming.getSize(),
                        "revision", nextRevision,
                        "documentId", savedMeta.getId()
                ));

        return savedMeta;
    }

    @Transactional(readOnly = true)
    public java.util.List<ApplicationDocument> listForApplication(long applicationId, SignedInUser caller) {
        Application row = applicationRecords.fetchOneIfAllowed(applicationId, caller);
        return documents.findByApplicationWithUploader(row);
    }
}
