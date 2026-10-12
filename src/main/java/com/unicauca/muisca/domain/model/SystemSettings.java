package com.unicauca.muisca.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Parámetros del sistema que administra el ADMIN. Es una única fila (id = 1). */
@Entity
@Table(name = "system_settings")
public class SystemSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    /** Si es false, ni el auto-registro ni el primer ingreso con Google crean cuentas nuevas. */
    @Column(name = "registration_enabled", nullable = false)
    private boolean registrationEnabled = true;

    /** Aviso que se muestra en el inicio de sesión y en la página de inicio. */
    @Column(length = 1000)
    private String announcement;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(boolean registrationEnabled, String announcement, String updatedBy) {
        this.registrationEnabled = registrationEnabled;
        this.announcement = announcement;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public String getAnnouncement() {
        return announcement;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
