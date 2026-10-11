package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.domain.model.SystemSettings;

/** Lo que necesita saber la pantalla de acceso antes de iniciar sesión. */
public record PublicSettingsResponse(boolean registrationEnabled, String announcement) {

    public static PublicSettingsResponse from(SystemSettings s) {
        return new PublicSettingsResponse(s.isRegistrationEnabled(), s.getAnnouncement());
    }
}
