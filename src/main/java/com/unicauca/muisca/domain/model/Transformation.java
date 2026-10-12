package com.unicauca.muisca.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Opción del catálogo que ve el usuario final (p. ej. "Contrato Go → Contrato Java"). Encadena
 * una transformación ATL opcional (modelo → modelo) y una plantilla Acceleo opcional
 * (modelo → código). El usuario solo sube un modelo conforme a {@link #inputMetamodel}.
 * Solo las transformaciones publicadas por un administrador son visibles para el rol USER.
 */
@Entity
@Table(name = "transformations")
public class Transformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "input_metamodel_id", nullable = false)
    private Metamodel inputMetamodel;

    @ManyToOne
    @JoinColumn(name = "atl_module_id")
    private AtlModule atlModule;

    @ManyToOne
    @JoinColumn(name = "acceleo_module_id")
    private AcceleoModule acceleoModule;

    @Column(nullable = false)
    private boolean published;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Transformation() {
    }

    public Transformation(UserEntity createdBy) {
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void define(String name, String description, Metamodel inputMetamodel, AtlModule atlModule,
            AcceleoModule acceleoModule) {
        this.name = name;
        this.description = description;
        this.inputMetamodel = inputMetamodel;
        this.atlModule = atlModule;
        this.acceleoModule = acceleoModule;
        this.updatedAt = LocalDateTime.now();
    }

    public void setPublished(boolean published) {
        this.published = published;
        this.updatedAt = LocalDateTime.now();
    }

    /** Metamodelo del modelo producido por la etapa ATL, o null si no hay etapa ATL. */
    public Metamodel getOutputMetamodel() {
        return atlModule != null ? atlModule.getTargetMetamodel() : null;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Metamodel getInputMetamodel() {
        return inputMetamodel;
    }

    public AtlModule getAtlModule() {
        return atlModule;
    }

    public AcceleoModule getAcceleoModule() {
        return acceleoModule;
    }

    public boolean isPublished() {
        return published;
    }

    public UserEntity getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
