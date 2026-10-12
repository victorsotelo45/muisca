package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.dto.SettingsRequest;
import com.unicauca.muisca.domain.model.SystemSettings;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.SystemSettingsRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Configuración global del sistema; si nunca se guardó, rigen los valores por defecto. */
@Service
public class SettingsService {

    private final SystemSettingsRepository repository;

    public SettingsService(SystemSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SystemSettings current() {
        return repository.findById(SystemSettings.SINGLETON_ID).orElseGet(SystemSettings::new);
    }

    @Transactional(readOnly = true)
    public boolean isRegistrationEnabled() {
        return current().isRegistrationEnabled();
    }

    @Transactional
    public SystemSettings update(SettingsRequest request, UserEntity actor) {
        SystemSettings settings = current();
        settings.update(request.registrationEnabled(), StoredModelService.blankToNull(request.announcement()),
                actor.getUsername());
        return repository.save(settings);
    }
}
