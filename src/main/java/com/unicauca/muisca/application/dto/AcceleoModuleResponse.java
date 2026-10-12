package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.application.port.ArtifactProblem;
import com.unicauca.muisca.domain.model.AcceleoModule;

import java.time.LocalDateTime;
import java.util.List;

public record AcceleoModuleResponse(Long id, String name, String description, String moduleName,
        String mainTemplate, Reference metamodel, String createdBy, LocalDateTime createdAt,
        LocalDateTime updatedAt, List<ArtifactProblem> warnings) {

    public static AcceleoModuleResponse from(AcceleoModule m) {
        return from(m, List.of());
    }

    public static AcceleoModuleResponse from(AcceleoModule m, List<ArtifactProblem> warnings) {
        return new AcceleoModuleResponse(m.getId(), m.getName(), m.getDescription(), m.getModuleName(),
                m.getMainTemplate(), new Reference(m.getMetamodel().getId(), m.getMetamodel().getName()),
                m.getCreatedBy().getUsername(), m.getCreatedAt(), m.getUpdatedAt(), warnings);
    }
}
