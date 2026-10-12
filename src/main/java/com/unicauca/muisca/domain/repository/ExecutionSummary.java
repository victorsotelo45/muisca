package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.ExecutionStage;
import com.unicauca.muisca.domain.model.ExecutionStatus;

import java.time.LocalDateTime;

/** Vista de una ejecución sin sus BLOBs, para listar el historial sin cargar archivos. */
public record ExecutionSummary(
        Long id,
        Long transformationId,
        String transformationName,
        String executedBy,
        ExecutionStatus status,
        ExecutionStage stage,
        String inputModelName,
        String errorMessage,
        long durationMs,
        LocalDateTime createdAt) {
}
