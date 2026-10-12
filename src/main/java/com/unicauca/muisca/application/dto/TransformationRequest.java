package com.unicauca.muisca.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Al menos una de las dos etapas (ATL o Acceleo) es obligatoria. */
public record TransformationRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        @NotNull Long inputMetamodelId,
        Long atlModuleId,
        Long acceleoModuleId) {
}
