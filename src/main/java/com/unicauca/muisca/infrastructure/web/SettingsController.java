package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.PublicSettingsResponse;
import com.unicauca.muisca.application.dto.SettingsRequest;
import com.unicauca.muisca.application.dto.SystemSettingsResponse;
import com.unicauca.muisca.application.service.SettingsService;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.infrastructure.security.CurrentUser;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Configuración del sistema: lectura pública de lo imprescindible y edición solo para ADMIN. */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/public")
    public PublicSettingsResponse publicSettings() {
        return PublicSettingsResponse.from(settingsService.current());
    }

    @GetMapping
    public SystemSettingsResponse get() {
        return SystemSettingsResponse.from(settingsService.current());
    }

    @PutMapping
    public SystemSettingsResponse update(@Valid @RequestBody SettingsRequest request, @CurrentUser UserEntity actor) {
        return SystemSettingsResponse.from(settingsService.update(request, actor));
    }
}
