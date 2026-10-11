package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.application.port.ArtifactProblem;
import com.unicauca.muisca.domain.model.Metamodel;

import java.time.LocalDateTime;
import java.util.List;

public record MetamodelResponse(Long id, String name, String description, String nsUri, String ecoreFileName,
        boolean hasGenmodel, String createdBy, LocalDateTime createdAt, List<ArtifactProblem> warnings) {

    public static MetamodelResponse from(Metamodel m) {
        return from(m, List.of());
    }

    public static MetamodelResponse from(Metamodel m, List<ArtifactProblem> warnings) {
        return new MetamodelResponse(m.getId(), m.getName(), m.getDescription(), m.getNsUri(), m.getEcoreFileName(),
                m.getGenmodelData() != null, m.getCreatedBy().getUsername(), m.getCreatedAt(), warnings);
    }
}
