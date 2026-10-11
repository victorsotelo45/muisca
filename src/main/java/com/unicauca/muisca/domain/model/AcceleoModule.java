package com.unicauca.muisca.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Plantilla Acceleo (modelo-a-texto) que genera el código fuente del contrato a partir de
 * un modelo conforme a {@link #metamodel}. Se ejecuta la plantilla marcada con {@code @main}.
 */
@Entity
@Table(name = "acceleo_modules")
public class AcceleoModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "main_template", nullable = false)
    private String mainTemplate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "metamodel_id", nullable = false)
    private Metamodel metamodel;

    @Lob
    @Column(name = "template_source", nullable = false, columnDefinition = "LONGTEXT")
    private String templateSource;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AcceleoModule() {
    }

    public AcceleoModule(String name, String description, Metamodel metamodel, UserEntity createdBy) {
        this.name = name;
        this.description = description;
        this.metamodel = metamodel;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void applyCompilation(String templateSource, String moduleName, String mainTemplate) {
        this.templateSource = templateSource;
        this.moduleName = moduleName;
        this.mainTemplate = mainTemplate;
        this.updatedAt = LocalDateTime.now();
    }

    public void describe(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
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

    public String getModuleName() {
        return moduleName;
    }

    public String getMainTemplate() {
        return mainTemplate;
    }

    public Metamodel getMetamodel() {
        return metamodel;
    }

    public String getTemplateSource() {
        return templateSource;
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
