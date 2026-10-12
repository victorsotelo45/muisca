package com.unicauca.muisca.domain.model;

import jakarta.persistence.*;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    /** VARCHAR y no el ENUM nativo de MySQL, para poder agregar estados sin migrar a mano. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    /** Etapa en curso mientras está RUNNING (p. ej. ATL o ACCELEO); null al terminar. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private ExecutionStage stage;

    @Column(name = "input_model_name", nullable = false)
    private String inputModelName;

    @Column(name = "input_model", nullable = false, length = Length.LONG32)
    private byte[] inputModel;

    @Column(name = "output_model_name")
    private String outputModelName;

    @Column(name = "output_model", length = Length.LONG32)
    private byte[] outputModel;

    @Column(name = "generated_code", length = Length.LONG32)
    private byte[] generatedCode;

    @Column(name = "generated_file_names", length = Length.LONG32)
    private String generatedFileNames;

    @Column(name = "error_message", length = Length.LONG32)
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
        this.status = ExecutionStatus.RUNNING;
        this.stage = ExecutionStage.QUEUED;
        this.createdAt = LocalDateTime.now();
    }

    public void advanceTo(ExecutionStage stage) {
        this.stage = stage;
    }

    public void succeed(String outputModelName, byte[] outputModel, List<String> generatedFileNames,
            byte[] generatedCode, long durationMs) {
        this.status = ExecutionStatus.SUCCEEDED;
        this.stage = null;
        this.outputModelName = outputModelName;
        this.outputModel = outputModel;
        this.generatedFileNames = generatedFileNames == null ? null : String.join("\n", generatedFileNames);
        this.generatedCode = generatedCode;
        this.durationMs = durationMs;
    }

    public void fail(String errorMessage, long durationMs) {
        this.status = ExecutionStatus.FAILED;
        this.stage = null;
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

    public ExecutionStage getStage() {
        return stage;
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
