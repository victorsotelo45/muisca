package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.domain.model.Metamodel;
import com.unicauca.muisca.domain.model.Transformation;

import java.time.LocalDateTime;

/**
 * @param producesModel true si devuelve un modelo transformado (etapa ATL)
 * @param producesCode  true si devuelve código fuente generado (etapa Acceleo)
 */
public record TransformationResponse(Long id, String name, String description, Reference inputMetamodel,
        Reference outputMetamodel, Reference atlModule, Reference acceleoModule, boolean producesModel,
        boolean producesCode, boolean published, String createdBy, LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TransformationResponse from(Transformation t) {
        return new TransformationResponse(t.getId(), t.getName(), t.getDescription(),
                ref(t.getInputMetamodel()), ref(t.getOutputMetamodel()),
                t.getAtlModule() == null ? null : new Reference(t.getAtlModule().getId(), t.getAtlModule().getName()),
                t.getAcceleoModule() == null ? null
                        : new Reference(t.getAcceleoModule().getId(), t.getAcceleoModule().getName()),
                t.getAtlModule() != null, t.getAcceleoModule() != null, t.isPublished(),
                t.getCreatedBy().getUsername(), t.getCreatedAt(), t.getUpdatedAt());
    }

    private static Reference ref(Metamodel m) {
        return m == null ? null : new Reference(m.getId(), m.getName());
    }
}
