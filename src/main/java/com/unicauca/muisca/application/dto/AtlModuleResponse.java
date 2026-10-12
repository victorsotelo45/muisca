package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.application.port.ArtifactProblem;
import com.unicauca.muisca.domain.model.AtlCompiler;
import com.unicauca.muisca.domain.model.AtlModule;

import java.time.LocalDateTime;
import java.util.List;

public record AtlModuleResponse(Long id, String name, String description, String moduleName, AtlCompiler compiler,
        Reference sourceMetamodel, String inModelName, String sourceAlias,
        Reference targetMetamodel, String outModelName, String targetAlias,
        String createdBy, LocalDateTime createdAt, LocalDateTime updatedAt, List<ArtifactProblem> warnings) {

    public static AtlModuleResponse from(AtlModule m) {
        return from(m, List.of());
    }

    public static AtlModuleResponse from(AtlModule m, List<ArtifactProblem> warnings) {
        return new AtlModuleResponse(m.getId(), m.getName(), m.getDescription(), m.getModuleName(), m.getCompiler(),
                new Reference(m.getSourceMetamodel().getId(), m.getSourceMetamodel().getName()),
                m.getInModelName(), m.getSourceAlias(),
                new Reference(m.getTargetMetamodel().getId(), m.getTargetMetamodel().getName()),
                m.getOutModelName(), m.getTargetAlias(),
                m.getCreatedBy().getUsername(), m.getCreatedAt(), m.getUpdatedAt(), warnings);
    }
}
