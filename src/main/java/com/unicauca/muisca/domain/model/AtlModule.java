package com.unicauca.muisca.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Transformación modelo-a-modelo escrita en ATL. Se guarda el código fuente y el módulo
 * compilado en el servidor; los alias y nombres de modelo se extraen de la cabecera
 * {@code create OUT : Destino from IN : Origen;}.
 */
@Entity
@Table(name = "atl_modules")
public class AtlModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AtlCompiler compiler;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_metamodel_id", nullable = false)
    private Metamodel sourceMetamodel;

    @Column(name = "in_model_name", nullable = false)
    private String inModelName;

    @Column(name = "source_alias", nullable = false)
    private String sourceAlias;

    @ManyToOne(optional = false)
    @JoinColumn(name = "target_metamodel_id", nullable = false)
    private Metamodel targetMetamodel;

    @Column(name = "out_model_name", nullable = false)
    private String outModelName;

    @Column(name = "target_alias", nullable = false)
    private String targetAlias;

    @Lob
    @Column(name = "source_code", nullable = false, columnDefinition = "LONGTEXT")
    private String sourceCode;

    @Lob
    @Column(name = "compiled_module", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] compiledModule;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AtlModule() {
    }

    public AtlModule(String name, String description, Metamodel sourceMetamodel, Metamodel targetMetamodel,
            UserEntity createdBy) {
        this.name = name;
        this.description = description;
        this.sourceMetamodel = sourceMetamodel;
        this.targetMetamodel = targetMetamodel;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void applyCompilation(String sourceCode, AtlCompiler compiler, String moduleName, String inModelName,
            String sourceAlias, String outModelName, String targetAlias, byte[] compiledModule) {
        this.sourceCode = sourceCode;
        this.compiler = compiler;
        this.moduleName = moduleName;
        this.inModelName = inModelName;
        this.sourceAlias = sourceAlias;
        this.outModelName = outModelName;
        this.targetAlias = targetAlias;
        this.compiledModule = compiledModule;
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

    public AtlCompiler getCompiler() {
        return compiler;
    }

    public Metamodel getSourceMetamodel() {
        return sourceMetamodel;
    }

    public String getInModelName() {
        return inModelName;
    }

    public String getSourceAlias() {
        return sourceAlias;
    }

    public Metamodel getTargetMetamodel() {
        return targetMetamodel;
    }

    public String getOutModelName() {
        return outModelName;
    }

    public String getTargetAlias() {
        return targetAlias;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public byte[] getCompiledModule() {
        return compiledModule;
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
