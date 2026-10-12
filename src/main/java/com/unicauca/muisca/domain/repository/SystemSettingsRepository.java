package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
}
