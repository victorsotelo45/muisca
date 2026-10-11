package com.unicauca.muisca.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ejecución de una {@link Transformation} sobre el modelo que subió un usuario, con sus
 * artefactos de salida. Guarda el nombre de la transformación porque esta puede borrarse
 * después sin que el usuario pierda su historial.
 */
@Entity
@Table(name = "executions")
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "transformation_id")
    private Transformation transformation;

    @Column(name = "transformation_name", nullable = false)
    private String transformationName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "executed_by", nullable = false)
    private UserEntity executedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(name = "input_model_name", nullable = false)
    private String inputModelName;

    @Lob
    @Column(name = "input_model", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] inputModel;

    @Column(name = "output_model_name")
    private String outputModelName;

    @Lob
    @Column(name = "output_model", columnDefinition = "LONGBLOB")
    private byte[] outputModel;

    @Lob
    @Column(name = "generated_code", columnDefinition = "LONGBLOB")
    private byte[] generatedCode;

    @Column(name = "generated_file_names", columnDefinition = "TEXT")
    private String generatedFileNames;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Execution() {
    }

    public Execution(Transformation transformation, UserEntity executedBy, String inputModelName, byte[] inputModel) {
        this.transformation = transformation;
        this.transformationName = transformation.getName();
        this.executedBy = executedBy;
        this.inputModelName = inputModelName;
        this.inputModel = inputModel;
        this.createdAt = LocalDateTime.now();
    }

    public void succeed(String outputModelName, byte[] outputModel, List<String> generatedFileNames,
            byte[] generatedCode, long durationMs) {
        this.status = ExecutionStatus.SUCCEEDED;
        this.outputModelName = outputModelName;
        this.outputModel = outputModel;
        this.generatedFileNames = generatedFileNames == null ? null : String.join("\n", generatedFileNames);
        this.generatedCode = generatedCode;
        this.durationMs = durationMs;
    }

    public void fail(String errorMessage, long durationMs) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }

    public List<String> getGeneratedFileNameList() {
        return generatedFileNames == null || generatedFileNames.isEmpty() ? List.of()
                : List.of(generatedFileNames.split("\n"));
    }

    public Long getId() {
        return id;
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public String getTransformationName() {
        return transformationName;
    }

    public UserEntity getExecutedBy() {
        return executedBy;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public String getInputModelName() {
        return inputModelName;
    }

    public byte[] getInputModel() {
        return inputModel;
    }

    public String getOutputModelName() {
        return outputModelName;
    }

    public byte[] getOutputModel() {
        return outputModel;
    }

    public byte[] getGeneratedCode() {
        return generatedCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
