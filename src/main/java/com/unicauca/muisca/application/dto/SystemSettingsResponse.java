package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.domain.model.SystemSettings;

import java.time.LocalDateTime;

/** Configuración completa (solo ADMIN). */
public record SystemSettingsResponse(boolean registrationEnabled, String announcement, String updatedBy,
        LocalDateTime updatedAt) {

    public static SystemSettingsResponse from(SystemSettings s) {
        return new SystemSettingsResponse(s.isRegistrationEnabled(), s.getAnnouncement(), s.getUpdatedBy(),
                s.getUpdatedAt());
    }
}
