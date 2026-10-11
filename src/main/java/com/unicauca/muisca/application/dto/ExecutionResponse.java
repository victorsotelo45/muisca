package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.domain.model.Execution;
import com.unicauca.muisca.domain.model.ExecutionStatus;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** @param downloads enlaces relativos para descargar cada artefacto disponible */
public record ExecutionResponse(Long id, Long transformationId, String transformationName, String executedBy,
        ExecutionStatus status, String inputModelName, String outputModelName, List<String> generatedFiles,
        String errorMessage, long durationMs, LocalDateTime createdAt, Map<String, String> downloads) {

    public static ExecutionResponse from(Execution e) {
        String base = "/api/executions/" + e.getId();
        Map<String, String> downloads = new LinkedHashMap<>();
        downloads.put("inputModel", base + "/input-model");
        if (e.getOutputModel() != null) {
            downloads.put("outputModel", base + "/output-model");
        }
        if (e.getGeneratedCode() != null) {
            downloads.put("generatedCode", base + "/generated-code");
            downloads.put("generatedFiles", base + "/generated-files");
        }
        return new ExecutionResponse(e.getId(),
                e.getTransformation() == null ? null : e.getTransformation().getId(),
                e.getTransformationName(), e.getExecutedBy().getUsername(), e.getStatus(), e.getInputModelName(),
                e.getOutputModelName(), e.getGeneratedFileNameList(), e.getErrorMessage(), e.getDurationMs(),
                e.getCreatedAt(), downloads);
    }
}
