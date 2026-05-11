package com.bnr.portal;

import com.bnr.portal.config.PortalSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PortalSettings.class)
public class LicensingPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(LicensingPortalApplication.class, args);
    }
}
