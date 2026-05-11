package com.bnr.portal.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class UploadFolderBootstrap {

    private final PortalSettings portalSettings;

    public UploadFolderBootstrap(PortalSettings portalSettings) {
        this.portalSettings = portalSettings;
    }

    @PostConstruct
    public void makeSureUploadFolderExists() throws Exception {
        Path dir = Path.of(portalSettings.getFiles().getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
    }
}
